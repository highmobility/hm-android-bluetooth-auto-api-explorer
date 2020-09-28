package com.highmobility.exploreautoapis;

import android.view.View;

import androidx.test.espresso.IdlingResource;

import org.hamcrest.Matcher;

import static com.highmobility.exploreautoapis.UtilsKt.getView;

public class ViewShownIdlingResource implements IdlingResource {
    private final Matcher<View> viewMatcher;
    private boolean shown;
    private ResourceCallback resourceCallback;

    public ViewShownIdlingResource(final Matcher<View> viewMatcher, boolean shown) {
        this.viewMatcher = viewMatcher;
        this.shown = shown;
    }

    @Override
    public boolean isIdleNow() {
        View view = getView(viewMatcher);
        boolean idle = false;

        if (this.shown == false) {
            if (view == null || view.isShown() == false) {
                idle = true;
            }
        } else {
            if (view != null && view.isShown()) idle = true;
        }

        if (idle == true && resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }

        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    @Override
    public String getName() {
        return this + viewMatcher.toString();
    }
}
