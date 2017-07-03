package com.highmobility.exploreautoapis.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by root on 08/06/2017.
 */

public class CircleButton extends ImageButton {
    Paint paint;

    public CircleButton(Context context) {
        super(context);
    }

    public CircleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (paint == null) {
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setShader(new RadialGradient(getWidth() / 2f, getHeight() / 2f,
                    getHeight() / 2.7f + 1, Color.argb(20, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.REPEAT));
            // 2.5f and 1.2f are connected. gradient is repeating from inside the circle
        }

        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 1.35f, paint);
        super.onDraw(canvas);
    }
}
