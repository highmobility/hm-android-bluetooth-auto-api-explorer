package com.highmobility.exploreautoapis

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.test.espresso.IdlingResource
import org.hamcrest.Matcher

class ViewShownIdlingResource(
    private val viewMatcher: Matcher<View>,
    private val shown: Boolean,
    private val notifyWhileIdle: Boolean = false
) : IdlingResource {
    private val lastVibrateTime: Long = 0
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun isIdleNow(): Boolean {
        val view = getView(viewMatcher)
        var idle = false
        if (shown == false) {
            if (view == null || view.isShown == false) {
                idle = true
            }
        } else {
            if (view != null && view.isShown) idle = true
        }
        if (idle == true && resourceCallback != null) {
            resourceCallback!!.onTransitionToIdle()
        }
        if (idle == false) notifyIdle()
        return idle
    }

    private fun notifyIdle() {
        if (notifyWhileIdle && (lastVibrateTime == 0L || System.currentTimeMillis() - lastVibrateTime > 1000)) {
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            (getTopMostActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
                effect
            )
        }
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    override fun getName(): String {
        return this.toString() + viewMatcher.toString()
    }
}