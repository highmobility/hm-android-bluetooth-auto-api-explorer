package com.highmobility.sandboxui.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.controller.BroadcastFragment;
import com.highmobility.sandboxui.controller.ConnectedVehicleBleController;
import com.highmobility.sandboxui.controller.ConnectedVehicleController;
import com.highmobility.sandboxui.model.ExteriorListItem;
import com.highmobility.sandboxui.model.VehicleStatus;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ConnectedVehicleActivity extends FragmentActivity implements
        IConnectedVehicleBleView, IConnectedVehicleView, BroadcastFragment
        .OnFragmentInteractionListener {
    public static final int REQUEST_CODE_REMOTE_CONTROL = 12;
    public static final String EXTRA_FINISH_ON_BACK_PRESS = "EXTRA_FINISH_ON_BACK_PRESS";

    ViewPager viewPager;
    TextView title;
    ProgressBar progressBar;
    ImageButton refreshButton;

    VehicleOverviewFragment overviewFragment;

    BroadcastFragment broadcastFragment;

    VehicleExteriorFragment exteriorFragment;
    ConnectedVehicleController controller;

    boolean finishOnBackPress;

    public void onRemoteControlClicked() {
        ((ConnectedVehicleBleController) controller).startRemoteControl();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_vehicle);

        finishOnBackPress = getIntent().getBooleanExtra(EXTRA_FINISH_ON_BACK_PRESS, true);
        viewPager = findViewById(R.id.view_pager);
        title = findViewById(R.id.title);
        progressBar = findViewById(R.id.progress_bar);
        refreshButton = findViewById(R.id.refresh_button);

        broadcastFragment = (BroadcastFragment) getSupportFragmentManager().findFragmentById(R.id
                .broadcast_fragment);
        controller = ConnectedVehicleController.create(this, this, getIntent());

        title.setText(controller.serviceName);

        if (controller.useBle) {
            refreshButton.setVisibility(GONE);
        } else {
            ((ViewGroup) broadcastFragment.getView().getParent()).removeView(broadcastFragment
                    .getView());
            getSupportFragmentManager().beginTransaction().remove(broadcastFragment).commit();
            broadcastFragment = null;
            refreshButton.setOnClickListener(v -> controller.onRefreshClicked());
        }

        viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager(), controller.vehicle));
        controller.init();
        if (controller.useBle) {
            // this has to happen after init
            broadcastFragment.onBroadcastingSerial(((ConnectedVehicleBleController) controller)
                    .isBroadcastingSerial());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_REMOTE_CONTROL) {
            ((ConnectedVehicleBleController) controller).onReturnFromRemoteControl();
        }
    }

    @Override public void onBackPressed() {
        if (finishOnBackPress) {
            controller.willDestroy();
            finish();
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onDestroy() {
        controller.onDestroy();
        super.onDestroy();
    }

    @Override public void onBroadcastSerialSwitchChanged(boolean on) {
        ((ConnectedVehicleBleController) controller).onBroadcastSerialSwitchChanged(on);
    }

    @Override
    public void showBleInfoView(boolean show, String status) {
        showNormalView(!show);
        broadcastFragment.getView().setVisibility(show ? VISIBLE : GONE);
        broadcastFragment.setStatusText(status);
    }

    @Override public void onLinkReceived(boolean received) {
        broadcastFragment.onLinkReceived(received);
        if (received == false) {
            showLoadingView(false);
            showNormalView(false);
        }
    }

    void showNormalView(boolean show) {
        if (show) {
            viewPager.animate().alpha(1f).setDuration(200).setListener(null);
        } else {
            viewPager.animate().alpha(0f).setDuration(200).setListener(null);
        }
    }

    @Override
    public void showLoadingView(boolean loading) {
        showNormalView(!loading);
        progressBar.setVisibility(loading ? VISIBLE : GONE);
        if (controller.useBle == false) {
            refreshButton.setEnabled(!loading);
        }
    }

    @Override
    public void onCapabilitiesUpdate(VehicleStatus vehicle) {

    }

    @Override
    public void onVehicleStatusUpdate(VehicleStatus vehicle) {
        overviewFragment.onVehicleStatusUpdate();
        exteriorFragment.onVehicleStatusUpdate(ExteriorListItem.createExteriorListItems(vehicle));
    }

    @Override
    public void onError(boolean fatal, String message) {
        Log.e("", "onError: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        if (fatal) {
            finish();
        } else {
            overviewFragment.onVehicleStatusUpdate();
            exteriorFragment.onVehicleStatusUpdate(ExteriorListItem.createExteriorListItems
                    (controller.vehicle));
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        VehicleStatus vehicle;

        public PagerAdapter(FragmentManager fragmentManager, VehicleStatus vehicle) {
            super(fragmentManager);
            this.vehicle = vehicle;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    overviewFragment = VehicleOverviewFragment.newInstance(vehicle,
                            ConnectedVehicleActivity.this);
                    return overviewFragment;
                case 1:
                    exteriorFragment = VehicleExteriorFragment.newInstance
                            (ConnectedVehicleActivity.this);
                    return exteriorFragment;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "OVERVIEW";
                case 1:
                    return "EXTERIOR";
                case 2:
                    return "INTERIOR";
                default:
                    return null;
            }
        }
    }
}