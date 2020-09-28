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
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*

import com.highmobility.sandboxui.model.ExteriorListItem
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertTrue

/**
 * Preconditions:
 *  /res/values/credentials.xml should have the HMKit initialise values
 *
 * Will:
 * Start the Telematics activity and send all of the commands. Checks that expected results are
 * reflected in the views.
 */
abstract class BaseConnectedVehicle {
    // needs to be a method because there are 2 activities shown
    abstract fun getActivity(): Activity

    fun testCommands() {
        waitViewShown(withId(R.id.lock_button))

        testOverviewSwitchButton(R.id.lock_button)
        testOverviewSwitchButton(R.id.trunk_button)
        testOverviewSwitchButton(R.id.sunroof_button)
        testOverviewSwitchButton(R.id.defrost_button)

        testExteriorListItem()
    }

    // first button(inactive) should not be checked
    private fun testExteriorListItem() {
        val frontLightsTitle = getActivity().resources.getString(R.string.frontLightsTitle)

        // swipe to exterior
        //
        onView(withId(R.id.connected_vehicle_view_pager)).perform(swipeLeft())

        val lightsContainer = onData(exteriorItemWithTitle(frontLightsTitle))
        val child = lightsContainer.onChildView(withId(R.id.third_button))
        child.perform(scrollTo())

        val lightsButtons = Pair(R.id.first_button, R.id.second_button)

        // scroll to lights, click inactive button, check selected and vice versa
        //
        lightsContainer.onChildView(withId(lightsButtons.first)).perform(click())
        waitViewNotShown(withId(R.id.progress_bar_connected))

        lightsContainer.onChildView(withId(lightsButtons.first)).check(matches(isChecked()))
        lightsContainer.onChildView(withId(lightsButtons.second)).check(matches(isNotChecked()))
        lightsContainer.onChildView(withId(lightsButtons.second)).perform(click())

        // it makes 2 clicks, and progress bar is show for both, then second checked
        waitViewNotShown(withId(R.id.progress_bar_connected))

        lightsContainer.onChildView(withId(lightsButtons.second)).check(matches(isChecked()))
        lightsContainer.onChildView(withId(lightsButtons.first)).check(matches(isNotChecked()))
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

    private fun testOverviewSwitchButton(buttonId: Int) {
        // Press the switch button and check the state is changed after.
        //
        waitViewNotShown(withId(R.id.progress_bar_connected))

        val drawableBefore =
            getTopMostActivity().findViewById<ImageButton>(buttonId).drawable.constantState

        onView(withId(buttonId)).check(matches(isDisplayed()))
        onView(withId(buttonId)).perform(click())

        waitViewNotShown(withId(R.id.progress_bar_connected))

        // check that lock image changed
        val drawableAfter =
            getTopMostActivity().findViewById<ImageButton>(buttonId).drawable.constantState

        assertTrue(drawableBefore != drawableAfter)
    }
}
