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
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.test.espresso.IdlingResource

class WaitUntilViewVisible(private val activity: Activity, private val id: Int, private val visible: Boolean = true) : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return javaClass.name
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }

    override fun isIdleNow(): Boolean {
        val topView = activity.findViewById<ViewGroup>(android.R.id.content)
        val viewToCheck = findOnScreenView(topView, id, activity, visible)

        var matched = false

        if (this.visible == false) {
            if (viewToCheck == null || (viewToCheck.visibility == INVISIBLE || viewToCheck.visibility == GONE)) {
                matched = true
            }
        }
        else {
            if (viewToCheck != null && viewToCheck.visibility == VISIBLE) matched = true
        }

        if (matched) resourceCallback?.onTransitionToIdle()
        return matched
    }

    fun findOnScreenView(parent: ViewGroup, id: Int, activity: Activity, visible: Boolean): View? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                if (child.id == id && isOnScreen(child, activity)) {
                    return child
                }

                val childReturn = findOnScreenView(child, id, activity, visible)
                if (childReturn != null) return childReturn
            }
            else {
                if (child != null && child.id == id && isOnScreen(child, activity)) {
                    return child
                }
            }
        }

        return null
    }
}