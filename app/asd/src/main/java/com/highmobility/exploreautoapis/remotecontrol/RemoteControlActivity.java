/*
 * HMKit Auto API - Auto API Parser for Java
 * Copyright (C) 2018 High-Mobility <licensing@high-mobility.com>
 *
 * This file is part of HMKit Auto API.
 *
 * HMKit Auto API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HMKit Auto API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HMKit Auto API.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.highmobility.exploreautoapis.remotecontrol;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.highmobility.exploreautoapis.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RemoteControlActivity extends Activity implements IRemoteControlView {
    @BindView(R.id.vehicle_view) RelativeLayout vehicleView;
    @BindView(R.id.loading_view) RelativeLayout vehicleLoadingView;
    @BindView(R.id.stop_button) Button stopButton;

    RemoteControlController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
        ButterKnife.bind(this);
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

    public void onMoveButtonClicked(View view) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);

        int tag = Integer.valueOf((String) view.getTag());
        controller.onMoveButtonClicked(tag);
    }

    public void onStopClicked(View view) {
        controller.onStopButtonClicked();
    }
}
