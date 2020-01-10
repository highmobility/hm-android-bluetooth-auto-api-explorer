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

import android.view.View
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.core.internal.deps.guava.base.Predicate
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun withViewCount(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        var actualCount = -1

        override fun describeTo(description: Description) {
            if (actualCount >= 0) {
                description.appendText("With expected number of items: $expectedCount")
                description.appendText("\n With matcher: ")
                viewMatcher.describeTo(description)
                description.appendText("\n But got: $actualCount")
            }
        }

        override fun matchesSafely(root: View): Boolean {
            actualCount = 0
            val iterable = TreeIterables.breadthFirstViewTraversal(root)
            actualCount = Iterables.filter(iterable, withMatcherPredicate(viewMatcher)).count()
            return actualCount == expectedCount
        }
    }
}

fun isNotDisplayed(): ViewAssertion {
    return ViewAssertion { view, noView ->
        if (view != null && isDisplayed().matches(view)) {
            throw AssertionError("View is present in the hierarchy and Displayed: " + HumanReadables.describe(view))
        }
    }
}

private fun withMatcherPredicate(matcher: Matcher<View>): Predicate<View> {
    return Predicate { view -> matcher.matches(view) }
}

fun <T> first(matcher: Matcher<T>): Matcher<T> {
    return object : BaseMatcher<T>() {
        var isFirst = true

        override fun matches(item: Any): Boolean {
            if (isFirst && matcher.matches(item)) {
                isFirst = false
                return true
            }

            return false
        }

        override fun describeTo(description: Description) {
            description.appendText("should return first matching item")
        }
    }
}