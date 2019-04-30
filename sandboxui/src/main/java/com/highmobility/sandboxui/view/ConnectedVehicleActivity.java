package com.highmobility.sandboxui.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ConnectedVehicleActivity extends FragmentActivity implements IConnectedVehicleBleView,
        IConnectedVehicleView, BroadcastFragment.OnFragmentInteractionListener {
    public static final int REQUEST_CODE_REMOTE_CONTROL = 12;
    public static final String EXTRA_FINISH_ON_BACK_PRESS = "EXTRA_FINISH_ON_BACK_PRESS";
    public static final String EXTRA_VEHICLE_SERIAL = "EXTRA_REVOKED_SERIAL";

    ViewPager viewPager;
    TextView title;
    ProgressBar progressBar;
    ImageButton refreshButton;
    ImageButton revokeButton;

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
        viewPager = findViewById(R.id.connected_vehicle_view_pager);
        title = findViewById(R.id.title);
        progressBar = findViewById(R.id.progress_bar_connected);
        refreshButton = findViewById(R.id.refresh_button);
        revokeButton = findViewById(R.id.revoke_button);

        broadcastFragment = (BroadcastFragment) getSupportFragmentManager().findFragmentById(R.id
                .broadcast_fragment);
        controller = ConnectedVehicleController.create(this, this, getIntent());

        title.setText(controller.getVehicleName());

        if (controller.useBle) {
            revokeButton.setOnClickListener(v -> controller.onRevokeClicked());
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
            Intent intent = controller.willDestroy();
            setResult(Activity.RESULT_FIRST_USER, intent);
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

    // MARK: IConnectedVehicleBleView

    @Override
    public void setBleInfo(String status) {
        if (broadcastFragment == null) return;
        broadcastFragment.setStatusText(status);
    }

    void showVehicleInfoView(boolean show) {
        if (show) {
            viewPager.animate().alpha(1f).setDuration(200).setListener(null);
        } else {
            viewPager.animate().alpha(0f).setDuration(200).setListener(null);
        }

        if (controller.useBle) revokeButton.setVisibility(show ? VISIBLE : GONE);
        else refreshButton.setEnabled(show);
    }

    @Override
    public void setViewState(ViewState viewState) {
        switch (viewState) {
            case BROADCASTING:
                broadcastFragment.getView().setVisibility(VISIBLE);
                broadcastFragment.onLinkReceived(false);
                showVehicleInfoView(false);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            case CONNECTED:
                if (broadcastFragment != null) {
                    broadcastFragment.getView().setVisibility(VISIBLE);
                    broadcastFragment.onLinkReceived(true);
                }
                showVehicleInfoView(false);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            case AUTHENTICATING:
                broadcastFragment.getView().setVisibility(VISIBLE);
                showVehicleInfoView(false);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            case AUTHENTICATED:
                if (broadcastFragment != null)
                    broadcastFragment.getView().setVisibility(View.INVISIBLE);
                showVehicleInfoView(true);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            case AUTHENTICATED_LOADING:
                if (broadcastFragment != null)
                    broadcastFragment.getView().setVisibility(View.INVISIBLE);
                progressBar.setVisibility(VISIBLE);
                showVehicleInfoView(false);
                break;
        }

    }

    @Override
    public void onCapabilitiesUpdate(VehicleStatus vehicle) {

    }

    @Override
    public void onVehicleStatusUpdate(VehicleStatus vehicle) {
        overviewFragment.onVehicleStatusUpdate();
        exteriorFragment.onVehicleStatusUpdate(ExteriorListItem.createExteriorListItems(
                getApplicationContext().getResources(),
                vehicle));
        title.setText(controller.getVehicleName());
    }

    @Override
    public void onError(boolean fatal, String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        if (fatal) {
            setBleInfo(message);
        } else {
            overviewFragment.onVehicleStatusUpdate();
            exteriorFragment.onVehicleStatusUpdate(ExteriorListItem.createExteriorListItems
                    (getApplicationContext().getResources(), controller.vehicle));
        }
    }

    @Override
    public void showAlert(String title, String message, String confirmTitle, String declineTitle,
                          DialogInterface.OnClickListener confirm, DialogInterface
                                  .OnClickListener decline) {
        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(confirmTitle, confirm)
                .setNegativeButton(declineTitle, decline)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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