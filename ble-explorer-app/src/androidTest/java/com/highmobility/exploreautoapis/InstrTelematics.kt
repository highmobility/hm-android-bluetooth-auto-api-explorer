package com.highmobility.exploreautoapis

import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.highmobility.hmkit.HMKit
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
            HMKit.getInstance().deleteCertificates()

            val res = InstrumentationRegistry.getInstrumentation().context.resources
            val intent = Intent()
            intent.putExtra(EXTRA_USE_BLE, false)
            intent.putExtra(EXTRA_SERVICE_NAME, "TEST")
            intent.putExtra(EXTRA_ALIVE_PING_AMOUNT_NAME, -1)
            intent.putExtra(EXTRA_INIT_INFO, String.format("%s:%s:%s:%s",
                    res.getString(com.highmobility.exploreautoapis.test.R.string.deviceCert),
                    res.getString(com.highmobility.exploreautoapis.test.R.string.privateKey),
                    res.getString(com.highmobility.exploreautoapis.test.R.string.issuerPublicKey),
                    res.getString(com.highmobility.exploreautoapis.test.R.string.accessToken)))
            return intent
        }
    }

    override fun getActivity() = activityRule.activity!!

    @Test
    fun test() {
        testCommands()
    }
}
