package com.highmobility.sandboxui.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.highmobility.sandboxui.R;

/**
 * Created by root on 26/05/2017.
 */

public class TapToMoveButton extends ImageButton {
    public TapToMoveButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundColor(Color.parseColor("#00FFFFFF"));
        setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.tap_circle_button, null));
    }
}
