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