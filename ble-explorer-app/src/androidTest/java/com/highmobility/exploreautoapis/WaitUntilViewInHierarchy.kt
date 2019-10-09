package com.highmobility.exploreautoapis

import android.app.Activity
import android.view.View
import androidx.test.espresso.IdlingResource

class WaitUntilViewInHierarchy(private val activity: Activity, private val id: Int) : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return javaClass.name
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }

    override fun isIdleNow(): Boolean {
        val rootView = activity.findViewById<View>(id)
        val idle = rootView != null
        if (idle) resourceCallback?.onTransitionToIdle()
        return idle
    }
}