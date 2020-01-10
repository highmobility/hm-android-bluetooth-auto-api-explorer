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
package com.highmobility.sandboxui.view;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.controller.RemoteControlController;

public class RemoteControlActivity extends Activity implements IRemoteControlView {
    RelativeLayout vehicleView;
    RelativeLayout vehicleLoadingView;
    Button stopButton;

    RemoteControlController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);

        vehicleView = findViewById(R.id.vehicle_view);
        vehicleLoadingView = findViewById(R.id.loading_view);
        stopButton = findViewById(R.id.stop_button);

        controller = new RemoteControlController(this);
    }

    @Override
    public void showLoadingView(boolean show) {
        if (show) {
            vehicleLoadingView.setVisibility(View.VISIBLE);
            vehicleView.setVisibility(View.GONE);
        }
        else {
            vehicleLoadingView.setVisibility(View.GONE);
            vehicleView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void showStopButton(boolean show) {
        stopButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("deprecation") // we need vibrate on < 26 versions as well
    public void onMoveButtonClicked(View view) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, 1));
        }
        else {
            v.vibrate(50);
        }

        int tag = Integer.valueOf((String) view.getTag());
        controller.onMoveButtonClicked(tag);
    }

    public void onStopClicked(View view) {
        controller.onStopButtonClicked();
    }
}
