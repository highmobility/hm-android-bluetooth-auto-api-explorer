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

import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.highmobility.sandboxui.controller.ConnectedVehicleController.*
import com.highmobility.sandboxui.view.ConnectedVehicleActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preconditions:
 *  /res/values/credentials.xml should have the HMKit initialise values
 *
 * Will:
 * Start the Telematics activity and send all of the commands. Checks that expected results are
 * reflected in the views.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InstrTelematics : BaseConnectedVehicle() {

    companion object;

    @Before
    fun setup() {

    }

    @Rule
    @JvmField
    var activityRule = object : IntentsTestRule<ConnectedVehicleActivity>(ConnectedVehicleActivity::class.java) {
        override fun getActivityIntent(): Intent {
            val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

            val intent = Intent()
            intent.putExtra(EXTRA_USE_BLE, false)
            intent.putExtra(EXTRA_SERVICE_NAME, "TEST")
            intent.putExtra(EXTRA_ALIVE_PING_AMOUNT_NAME, -1)
            intent.putExtra(EXTRA_INIT_INFO, String.format("%s:%s:%s:%s",
                    resources.getString(R.string.prodDeviceCert),
                    resources.getString(R.string.prodPrivateKey),
                    resources.getString(R.string.prodIssuerPublicKey),
                    resources.getString(R.string.prodAccessToken)))
            return intent
        }
    }

    override fun getActivity() = activityRule.activity!!

    @Test
    fun test() {
        testCommands()
    }
}
