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

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.highmobility.autoapi.Climate;
import com.highmobility.autoapi.Doors;
import com.highmobility.autoapi.RooftopControl;
import com.highmobility.autoapi.Trunk;
import com.highmobility.autoapi.VehicleLocation;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.model.VehicleState;

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

    VehicleState vehicle;
    ConnectedVehicleActivity parent;
    private OnFragmentInteractionListener mListener;

    public VehicleOverviewFragment() {
        // Required empty public constructor
    }

    public static VehicleOverviewFragment newInstance(VehicleState vehicle,
                                                      ConnectedVehicleActivity connectedVehicleActivity) {
        VehicleOverviewFragment fragment = new VehicleOverviewFragment();
        fragment.vehicle = vehicle;
        fragment.parent = connectedVehicleActivity;
        return fragment;
    }

    public void setVehicle(VehicleState vehicle) {
        this.vehicle = vehicle;
    }

    public void onVehicleStatusUpdate() {
        updateViews();
    }

    void updateViews() {
        if (vehicle.isRemoteControlSupported()) {
            remoteControlButton.setVisibility(View.VISIBLE);
        } else {
            remoteControlButton.setVisibility(View.GONE);
        }

        if (vehicle.getRooftopDimmingPercentage() != null) {
            sunroofButton.setVisibility(View.VISIBLE);
            if (vehicle.getRooftopDimmingPercentage() == 1f) {
                sunroofButton.setImageResource(R.drawable.ovr_sunroofopaquehdpi);
            } else {
                sunroofButton.setImageResource(R.drawable.ovr_sunrooftransparenthdpi);
            }

            if (vehicle.isSupported(RooftopControl.IDENTIFIER, RooftopControl.PROPERTY_SUNROOF_STATE)) {
                // disable button
                sunroofButton.setEnabled(true);

            } else {
                sunroofButton.setEnabled(false);
            }
        } else {
            sunroofButton.setVisibility(View.GONE);
        }

        Boolean defrostingActive = vehicle.isWindshieldDefrostingActive();

        if (defrostingActive != null && vehicle.getInsideTemperature() != null) {
            defrostButton.setVisibility(View.VISIBLE);
            temperatureIndicatorContainer.setVisibility(View.VISIBLE);
            temperatureIndicatorTextView.setText(String.format("%.2f", vehicle
                    .getInsideTemperature()));

            if (defrostingActive) {
                defrostButton.setImageResource(R.drawable.ovr_defrostactivehdpi);
            } else {
                defrostButton.setImageResource(R.drawable.ovr_defrostinactivehdpi);
            }

            if (vehicle.isSupported(Climate.IDENTIFIER, Climate.PROPERTY_DEFROSTING_STATE)) {
                defrostButton.setEnabled(true);
            } else {
                // get state only available
                defrostButton.setEnabled(false);
            }
        } else {
            defrostButton.setVisibility(View.GONE);
            temperatureIndicatorContainer.setVisibility(View.GONE);
        }

        if (vehicle.getDoorsLocked() != null) {
            lockButton.setVisibility(View.VISIBLE);
            if (vehicle.getDoorsLocked() == true) {
                lockButton.setImageResource(R.drawable.ovr_doorslockedhdpi);
            } else {
                lockButton.setImageResource(R.drawable.ovr_doorsunlockedhdpi);
            }

            if (vehicle.isSupported(Doors.IDENTIFIER, Doors.PROPERTY_LOCKS_STATE)) {
                lockButton.setEnabled(true);
            } else {
                // get state only available
                lockButton.setEnabled(false);
            }
        } else {
            lockButton.setVisibility(View.GONE);
        }

        LockState state = vehicle.getTrunkLockState();
        if (state != null) {
            trunkButton.setVisibility(View.VISIBLE);

            if (state == LockState.LOCKED) {
                trunkButton.setImageResource(R.drawable.ovr_trunklockedhdpi);
            } else {
                trunkButton.setImageResource(R.drawable.ovr_trunkunlockedhdpi);
            }

            if (vehicle.isSupported(Trunk.IDENTIFIER, Trunk.PROPERTY_LOCK)) {
                trunkButton.setEnabled(true);
            } else {
                trunkButton.setEnabled(false);
            }
        } else {
            trunkButton.setVisibility(View.GONE);
        }

        if (vehicle.isSupported(VehicleLocation.IDENTIFIER, VehicleLocation.PROPERTY_COORDINATES)) {
            gpsIndicatorContainer.setVisibility(View.VISIBLE);
        } else {
            gpsIndicatorContainer.setVisibility(View.GONE);
        }

        Double batteryPercentage = vehicle.getBatteryPercentage();

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
        trunkButton.setOnClickListener(v -> parent.controller.onLockTrunkClicked());
        lockButton.setOnClickListener(v -> parent.controller.onLockDoorsClicked());
        sunroofButton.setOnClickListener(v -> parent.controller.onSunroofVisibilityClicked());
        defrostButton.setOnClickListener(v -> parent.controller.onWindshieldDefrostingClicked());

        return view;
    }
}
