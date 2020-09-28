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
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.res.Resources
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.highmobility.hmkit.HMKit
import com.highmobility.sandboxui.view.ConnectedVehicleActivity
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeUnit

/**
 * Preconditions:
 *  /res/values/credentials.xml should have the HMKit initialise values
 *
 *  !!!! Bluetooth has to be manually connected in the emulator. After vibrate effect
 *
 * Will:
 *
 * Start the Bluetooth activity
 * * tests turning ble on/off
 * * sends all of the commands. Checks that expected results are reflected in the views.
 *
 * This is the first test for the sake of connecting BLE asap so rest of the tests are automated.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstrABluetooth : BaseConnectedVehicle() {
    private lateinit var scenario: ActivityScenario<BleExplorerActivity>
    override fun getActivity() = getTopMostActivity()

    private val res: Resources by lazy { getActivity().resources }
    private val waitBleInactive = WaitUntilBleActive(false)
    private val waitBleActive = WaitUntilBleActive(true)

    @Before
    fun setup() {
        scenario = launchActivity()

        IdlingPolicies.setMasterPolicyTimeout(1, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(1, TimeUnit.MINUTES)

        waitUntilActivityVisible<ConnectedVehicleActivity>()
        waitViewNotShown(withId(R.id.progress_bar_connected))

        turnBleOn(true)

        onView(withId(R.id.looking_for_links)).check(
            matches(withSubstring(res.getString(R.string.looking_for_links).split(" ")[0]))
        )
    }

    @After
    fun after() {
        scenario.close()
    }

    // first test turning ble on/off and that it works after doing it.
    @Test
    fun b_bleState() {
        // If ble on/off tests fail can increase the thread.sleep in this idling resource

        // start broadcast > verify broadcasting > ble off > verify shows ble unavailable > turn on again
        //
        waitViewNotShown(withId(R.id.progress_bar_connected))
        onView(withId(R.id.looking_for_links)).check(
            matches(
                withSubstring(
                    res.getString(R.string.looking_for_links).split(" ")[0]
                )
            )
        )

        turnBleOn(false)

        IdlingRegistry.getInstance().register(waitBleInactive)
        onView(withId(R.id.looking_for_links)).check(matches(withText(res.getString(R.string.ble_na))))
        IdlingRegistry.getInstance().unregister(waitBleInactive)

        turnBleOn(true)

        IdlingRegistry.getInstance().register(waitBleActive)
        onView(withId(R.id.looking_for_links)).check(
            matches(
                withSubstring(
                    res.getString(R.string.looking_for_links).split(" ")[0]
                )
            )
        )
        IdlingRegistry.getInstance().unregister(waitBleActive)
    }

    @Test
    fun x_testCommands() {
        // give some time to connect in emulator + the commands
        IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.MINUTES)

        // notify that its time to connect in emulator
        val effect = VibrationEffect.createOneShot(
            500, VibrationEffect.DEFAULT_AMPLITUDE
        )

        (getActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(effect)

        super.testCommands()

        IdlingPolicies.setMasterPolicyTimeout(1, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(1, TimeUnit.MINUTES)

        // force disconnect from the link by restarting ble
        // otherwise on next test sdk init will say: cannot set cert, disconnect from all of the links
        restartBle()
        waitViewShown(withId(R.id.broadcast_fragment))
        onView(withId(R.id.looking_for_links)).check(matches(isDisplayed()))
        HMKit.getInstance().terminate()
    }

    private fun restartBle() {
        BluetoothAdapter.getDefaultAdapter().disable()
        Thread.sleep(1000)
        BluetoothAdapter.getDefaultAdapter().enable()
    }

    private fun turnBleOn(on: Boolean) {
        if (on) BluetoothAdapter.getDefaultAdapter().enable()
        else BluetoothAdapter.getDefaultAdapter().disable()
    }

    class WaitUntilBleActive(private val active: Boolean = true) : IdlingResource {
        private var resourceCallback: IdlingResource.ResourceCallback? = null

        override fun getName(): String {
            return javaClass.name
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.resourceCallback = callback
        }

        override fun isIdleNow(): Boolean {
            var matched = false

            if (BluetoothAdapter.getDefaultAdapter().isEnabled == active) {
                Thread.sleep(1000) // wait for the system callback to happen so SDK state is reflected
                matched = true
            }

            if (matched) resourceCallback?.onTransitionToIdle()
            return matched
        }
    }
}
