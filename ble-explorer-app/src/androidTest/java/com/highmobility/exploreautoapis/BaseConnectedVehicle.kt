/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.exploreautoapis

import android.app.Activity
import android.widget.ImageButton
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.highmobility.sandboxui.model.ExteriorListItem
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert

/**
 * Preconditions:
 *  /res/values/credentials.xml should have the HMKit initialise values
 *
 * Will:
 * Start the Telematics activity and send all of the commands. Checks that expected results are
 * reflected in the views.
 */
abstract class BaseConnectedVehicle {
    val waitUntilNoProgressBar by lazy {
        WaitUntilViewVisible(getActivity(), R.id.progress_bar_connected, false)
    }

    // override
    abstract fun getActivity(): Activity

    fun testCommands() {
        waitInitCommandsFinished()

//        testOverviewSwitchButton(R.id.lock_button) // currently lock behaviour is ambiguous
        testOverviewSwitchButton(R.id.trunk_button)
        testOverviewSwitchButton(R.id.sunroof_button)
        testOverviewSwitchButton(R.id.defrost_button)

        waitInitCommandsFinished()
        val frontLightsTitle = getActivity().resources.getString(R.string.frontLightsTitle)

        // swipe to exterior
        //
        Espresso.onView(withId(R.id.connected_vehicle_view_pager)).perform(swipeLeft())

        // scroll to lights, click on 3rd option, check third selected, click on first, check first
        // selected
        //
        onData(exteriorItemWithTitle(frontLightsTitle)).onChildView(withId(R.id.third_button)).perform(click())

        IdlingRegistry.getInstance().register(waitUntilNoProgressBar)
        onData(exteriorItemWithTitle(frontLightsTitle)).onChildView(withId(R.id.third_button)).check(matches(isChecked()))
        onData(exteriorItemWithTitle(frontLightsTitle)).onChildView(withId(R.id.first_button)).check(matches(isNotChecked()))
        IdlingRegistry.getInstance().unregister(waitUntilNoProgressBar)

        onData(exteriorItemWithTitle(frontLightsTitle)).onChildView(withId(R.id.first_button)).perform(click())

        IdlingRegistry.getInstance().register(waitUntilNoProgressBar)
        onData(exteriorItemWithTitle(frontLightsTitle)).onChildView(withId(R.id.first_button)).check(matches(isChecked()))
        onData(exteriorItemWithTitle(frontLightsTitle)).onChildView(withId(R.id.third_button)).check(matches(isNotChecked()))
        IdlingRegistry.getInstance().unregister(waitUntilNoProgressBar)
    }

    private fun exteriorItemWithTitle(title: String): Matcher<ExteriorListItem> {
        return object : TypeSafeMatcher<ExteriorListItem>() {
            override fun describeTo(description: Description?) {
                description?.appendText("Cell title should be $title")
            }

            override fun matchesSafely(item: ExteriorListItem?): Boolean {
                return item?.title == title
            }
        }
    }

    private fun waitInitCommandsFinished() {
        // wait until lock button visible (telematics connected or ble authorised)
        //
        val waitUntilNoProgressBar3 = WaitUntilViewVisible(getTopMostActivity(), R.id.lock_button, true)
        IdlingRegistry.getInstance().register(waitUntilNoProgressBar3)
        Espresso.onView(withId(R.id.lock_button)).check(matches(isDisplayed()))
        IdlingRegistry.getInstance().unregister(waitUntilNoProgressBar3)
    }

    private fun testOverviewSwitchButton(buttonId: Int) {
        // Press the switch button and check the state is changed after.
        //
        val drawableBefore = getTopMostActivity().findViewById<ImageButton>(buttonId).drawable.constantState
        Espresso.onView(withId(buttonId)).perform(click())

        val waitUntilNoProgressBar4 = WaitUntilViewVisible(getTopMostActivity(), R.id.progress_bar_connected, false)
        IdlingRegistry.getInstance().register(waitUntilNoProgressBar4)

        Espresso.onView(withId(buttonId)).check(matches(isDisplayed()))
        // check that lock image changed
        val drawableAfter = getTopMostActivity().findViewById<ImageButton>(buttonId).drawable.constantState
        Assert.assertTrue(drawableBefore != drawableAfter)
        IdlingRegistry.getInstance().unregister(waitUntilNoProgressBar4)
    }
}
