package com.highmobility.sandboxui.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
