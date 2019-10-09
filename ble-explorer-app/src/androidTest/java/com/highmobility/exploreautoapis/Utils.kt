package com.highmobility.exploreautoapis

import android.app.Activity
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import timber.log.Timber.d

fun printViewHierarchy(v: Activity) {
    d(getViewHierarchy(v.findViewById(android.R.id.content)))
}

fun getViewHierarchy(v: View): String {
    val desc = StringBuilder()
    getViewHierarchy(v, desc, 0)
    return desc.toString()
}

private fun getViewHierarchy(v: View, desc: StringBuilder, margin: Int) {
    var margin = margin
    desc.append(getViewMessage(v, margin))
    if (v is ViewGroup) {
        margin++
        val vg = v as ViewGroup
        for (i in 0 until vg.childCount) {
            getViewHierarchy(vg.getChildAt(i), desc, margin)
        }
    }
}

private fun getViewMessage(v: View, marginOffset: Int): String {
    val repeated = String(CharArray(marginOffset)).replace("\u0000", "  ")
    try {
        val resourceId = if (v.resources != null) if (v.id > 0) v.resources.getResourceName(v.id) else "no_id" else "no_resources"

        val visibility = v.visibility
        return repeated + "[" + v.javaClass.simpleName + "] " + resourceId + " " + visibility + "\n"
    } catch (e: Resources.NotFoundException) {
        return repeated + "[" + v.javaClass.simpleName + "] name_not_found\n"
    }
}

fun getTopMostActivity(): Activity {
    val currentActivity = arrayOf<Activity?>(null)

    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        val it = resumedActivity.iterator()
        currentActivity[0] = it.next()
    }

    return currentActivity[0]!!
}

fun isVisible(view: View?, activity: Activity): Boolean {
    if (view == null) return false
    if (!view.isShown) return false
    return true
}

fun isOnScreen(view: View?, activity: Activity): Boolean {
    val location = IntArray(2)
    view?.getLocationOnScreen(location)
    val screen = Rect(0, 0, activity.resources.displayMetrics.widthPixels, activity.resources.displayMetrics.heightPixels)
    return screen.contains(location[0], location[1])
}

fun waitUntilProgressBarNotVisible(activity:Activity, id:Int, completion:()->Unit) {
    val waitUntilNoProgressBar4 = WaitUntilViewVisible(activity, id, false)
    IdlingRegistry.getInstance().register(waitUntilNoProgressBar4)
    completion()
    IdlingRegistry.getInstance().unregister(waitUntilNoProgressBar4)
}