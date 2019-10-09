package com.highmobility.exploreautoapis

import androidx.test.espresso.IdlingResource
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.hmkit.HMKit

class WaitUntilCertDownloaded(private val serial: DeviceSerial) : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return javaClass.name
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }

    override fun isIdleNow(): Boolean {
        var matched = HMKit.getInstance().getCertificate(serial) != null

        if (matched) resourceCallback?.onTransitionToIdle()
        return matched
    }


}