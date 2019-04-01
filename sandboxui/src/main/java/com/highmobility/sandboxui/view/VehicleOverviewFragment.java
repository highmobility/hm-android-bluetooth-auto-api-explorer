package com.highmobility.sandboxui.view;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.highmobility.autoapi.ControlCommand;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.ControlTrunk;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.VehicleLocation;
import com.highmobility.autoapi.value.Lock;
import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.model.VehicleStatus;

import androidx.fragment.app.Fragment;

public class VehicleOverviewFragment extends Fragment {
    ImageButton defrostButton;
    ImageButton sunroofButton;
    ImageButton trunkButton;

    LinearLayout gpsIndicatorContainer;
    LinearLayout temperatureIndicatorContainer;
    LinearLayout batteryIndicatorContainer;

    TextView temperatureIndicatorTextView;
    TextView batteryIndicatorTextView;

    CircleButton remoteControlButton;
    CircleButton lockButton;

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    VehicleStatus vehicle;
    ConnectedVehicleActivity parent;
    private OnFragmentInteractionListener mListener;

    public VehicleOverviewFragment() {
        // Required empty public constructor
    }

    public static VehicleOverviewFragment newInstance(VehicleStatus vehicle,
                                                      ConnectedVehicleActivity connectedVehicleActivity) {
        VehicleOverviewFragment fragment = new VehicleOverviewFragment();
        fragment.vehicle = vehicle;
        fragment.parent = connectedVehicleActivity;
        return fragment;
    }

    public void setVehicle(VehicleStatus vehicle) {
        this.vehicle = vehicle;
    }

    public void onVehicleStatusUpdate() {
        updateViews();
    }

    void updateViews() {
        if (vehicle.isSupported(ControlCommand.TYPE)) {
            remoteControlButton.setVisibility(View.VISIBLE);
        } else {
            remoteControlButton.setVisibility(View.GONE);
        }

        if (vehicle.rooftopDimmingPercentage != null) {
            sunroofButton.setVisibility(View.VISIBLE);
            if (vehicle.rooftopDimmingPercentage == 1f) {
                sunroofButton.setImageResource(R.drawable.ovr_sunroofopaquehdpi);
            } else {
                sunroofButton.setImageResource(R.drawable.ovr_sunrooftransparenthdpi);
            }

            if (vehicle.isSupported(ControlRooftop.TYPE)) {
                // disable button
                sunroofButton.setEnabled(true);
                sunroofButton.setOnClickListener(v -> parent.controller.onSunroofVisibilityClicked());
            } else {
                sunroofButton.setEnabled(false);
            }
        } else {
            sunroofButton.setVisibility(View.GONE);
        }

        Boolean defrostingActive = vehicle.isWindshieldDefrostingActive;

        if (defrostingActive != null && vehicle.insideTemperature != null) {
            defrostButton.setVisibility(View.VISIBLE);
            temperatureIndicatorContainer.setVisibility(View.VISIBLE);
            temperatureIndicatorTextView.setText(String.format("%.2f", vehicle
                    .insideTemperature));

            if (defrostingActive) {
                defrostButton.setImageResource(R.drawable.ovr_defrostactivehdpi);
            } else {
                defrostButton.setImageResource(R.drawable.ovr_defrostinactivehdpi);
            }

            if (vehicle.isSupported(StartStopDefrosting.TYPE)) {
                defrostButton.setEnabled(true);
                defrostButton.setOnClickListener(v -> parent.controller.onWindshieldDefrostingClicked());
            } else {
                // get state only available
                defrostButton.setEnabled(false);
            }
        } else {
            defrostButton.setVisibility(View.GONE);
            temperatureIndicatorContainer.setVisibility(View.GONE);
        }

        if (vehicle.doorsLocked != null) {
            lockButton.setVisibility(View.VISIBLE);
            if (vehicle.doorsLocked == true) {
                lockButton.setImageResource(R.drawable.ovr_doorslockedhdpi);
            } else {
                lockButton.setImageResource(R.drawable.ovr_doorsunlockedhdpi);
            }

            if (vehicle.isSupported(LockUnlockDoors.TYPE)) {
                lockButton.setEnabled(true);
                lockButton.setOnClickListener(v -> parent.controller.onLockDoorsClicked());
            } else {
                // get state only available
                lockButton.setEnabled(false);
            }
        } else {
            lockButton.setVisibility(View.GONE);
        }

        Lock state = vehicle.trunkLockState;
        if (state != null) {
            trunkButton.setVisibility(View.VISIBLE);

            if (state == Lock.LOCKED) {
                trunkButton.setImageResource(R.drawable.ovr_trunklockedhdpi);
            } else {
                trunkButton.setImageResource(R.drawable.ovr_trunkunlockedhdpi);
            }

            if (vehicle.isSupported(ControlTrunk.TYPE)) {
                trunkButton.setEnabled(true);

                trunkButton.setOnClickListener(v -> parent.controller.onLockTrunkClicked());
            } else {
                trunkButton.setEnabled(false);
            }
        } else {
            trunkButton.setVisibility(View.GONE);
        }

        if (vehicle.isSupported(VehicleLocation.TYPE)) {
            gpsIndicatorContainer.setVisibility(View.VISIBLE);
        } else {
            gpsIndicatorContainer.setVisibility(View.GONE);
        }

        Double batteryPercentage = vehicle.batteryPercentage;

        if (batteryPercentage != null) {
            batteryIndicatorContainer.setVisibility(View.VISIBLE);
            batteryIndicatorTextView.setText((int) (batteryPercentage * 100f) + "%");
        } else {
            batteryIndicatorContainer.setVisibility(View.GONE);
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vehicle_overview, container, false);

        defrostButton = view.findViewById(R.id.defrost_button);
        sunroofButton = view.findViewById(R.id.sunroof_button);
        trunkButton = view.findViewById(R.id.trunk_button);
        gpsIndicatorContainer = view.findViewById(R.id.gps_indicator);
        temperatureIndicatorContainer = view.findViewById(R.id.temperature_indicator);
        batteryIndicatorContainer = view.findViewById(R.id.battery_indicator);
        temperatureIndicatorTextView = view.findViewById(R.id.temperature_indicator_value);
        batteryIndicatorTextView = view.findViewById(R.id.battery_indicator_value);
        remoteControlButton = view.findViewById(R.id.remote_control_button);
        lockButton = view.findViewById(R.id.lock_button);

        remoteControlButton.setOnClickListener(v -> parent.onRemoteControlClicked());
        return view;
    }
}
