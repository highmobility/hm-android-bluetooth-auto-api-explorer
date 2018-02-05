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

package com.highmobility.exploreautoapis;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.highmobility.autoapi.capability.AvailableCapability;
import com.highmobility.autoapi.capability.AvailableGetStateCapability;
import com.highmobility.autoapi.capability.ClimateCapability;
import com.highmobility.autoapi.capability.FeatureCapability;
import com.highmobility.autoapi.capability.RooftopCapability;
import com.highmobility.autoapi.capability.TrunkAccessCapability;

import com.highmobility.exploreautoapis.storage.VehicleStatus;
import com.highmobility.exploreautoapis.view.CircleButton;
import com.highmobility.autoapi.incoming.TrunkState;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.highmobility.autoapi.Command.Identifier.CHARGING;
import static com.highmobility.autoapi.Command.Identifier.CLIMATE;
import static com.highmobility.autoapi.Command.Identifier.DOOR_LOCKS;
import static com.highmobility.autoapi.Command.Identifier.REMOTE_CONTROL;
import static com.highmobility.autoapi.Command.Identifier.ROOFTOP;
import static com.highmobility.autoapi.Command.Identifier.TRUNK_ACCESS;
import static com.highmobility.autoapi.Command.Identifier.VEHICLE_LOCATION;

public class VehicleOverviewFragment extends Fragment {
    @BindView(R.id.defrost_button) ImageButton defrostButton;
    @BindView(R.id.sunroof_button) ImageButton sunroofButton;
    @BindView(R.id.trunk_button) ImageButton trunkButton;

    @BindView(R.id.gps_indicator) LinearLayout gpsIndicatorContainer;
    @BindView(R.id.temperature_indicator) LinearLayout temperatureIndicatorContainer;
    @BindView(R.id.battery_indicator) LinearLayout batteryIndicatorContainer;

    @BindView(R.id.temperature_indicator_value) TextView temperatureIndicatorTextView;
    @BindView(R.id.battery_indicator_value) TextView batteryIndicatorTextView;

    @BindView(R.id.remote_control_button) CircleButton remoteControlButton;
    @BindView(R.id.lock_button) CircleButton lockButton;

    VehicleStatus vehicle;
    VehicleActivity parent;

    public VehicleOverviewFragment() {
        // Required empty public constructor
    }

    public static VehicleOverviewFragment newInstance(VehicleStatus vehicle, VehicleActivity vehicleActivity) {
        VehicleOverviewFragment fragment = new VehicleOverviewFragment();
        fragment.vehicle = vehicle;
        fragment.parent = vehicleActivity;
        return fragment;
    }

    public void setVehicle(VehicleStatus vehicle) {
        this.vehicle = vehicle;
    }

    public void onVehicleStatusUpdate() {
        updateViews();
    }

    void updateViews() {
        FeatureCapability[] capabilities = vehicle.overviewCapabilities;
        
        for (int i = 0; i < capabilities.length; i++) {
            FeatureCapability capability = capabilities[i];

            if (capability.getIdentifier() == REMOTE_CONTROL) {
                AvailableCapability remoteControlCapability = (AvailableCapability) capability;
                if (remoteControlCapability.getCapability() == AvailableCapability.Capability.AVAILABLE) {
                    remoteControlButton.setVisibility(View.VISIBLE);
                }
                else {
                    remoteControlButton.setVisibility(View.GONE);
                }
            }
            else if (capability.getIdentifier() == ROOFTOP) {
                RooftopCapability rooftopCapability = (RooftopCapability)capability;

                if (rooftopCapability.getDimmingCapability() != RooftopCapability.DimmingCapability.UNAVAILABLE) {
                    sunroofButton.setVisibility(View.VISIBLE);
                    if (vehicle.rooftopDimmingPercentage == 1f) {
                        sunroofButton.setImageResource(R.drawable.ovr_sunroofopaquehdpi);
                    }
                    else {
                        sunroofButton.setImageResource(R.drawable.ovr_sunrooftransparenthdpi);
                    }

                    if (rooftopCapability.getDimmingCapability() == RooftopCapability.DimmingCapability.GET_STATE_AVAILABLE) {
                        // disable button
                        sunroofButton.setEnabled(false);
                    }
                    else {
                        sunroofButton.setEnabled(true);
                        sunroofButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                parent.controller.onSunroofVisibilityClicked();
                            }
                        });
                    }
                }
                else {
                    sunroofButton.setVisibility(View.GONE);
                }
            }
            else if (capability.getIdentifier() == CLIMATE) {
                ClimateCapability climateCapability = (ClimateCapability)capability;
                boolean defrostingActive = vehicle.isWindshieldDefrostingActive;

                if (climateCapability.getClimateCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    defrostButton.setVisibility(View.VISIBLE);
                    temperatureIndicatorContainer.setVisibility(View.VISIBLE);
                    temperatureIndicatorTextView.setText(String.format("%.2f", vehicle.insideTemperature));

                    if (defrostingActive) {
                        defrostButton.setImageResource(R.drawable.ovr_defrostactivehdpi);
                    }
                    else {
                        defrostButton.setImageResource(R.drawable.ovr_defrostinactivehdpi);
                    }


                    if (climateCapability.getClimateCapability() == AvailableGetStateCapability.Capability.AVAILABLE) {
                        defrostButton.setEnabled(true);
                        defrostButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                parent.controller.onWindshieldDefrostingClicked();
                            }
                        });
                    }
                    else {
                        // get state only available
                        defrostButton.setEnabled(false);
                    }
                }
                else {
                    defrostButton.setVisibility(View.GONE);
                    temperatureIndicatorContainer.setVisibility(View.GONE);
                }
            }
            else if (capability.getIdentifier() == DOOR_LOCKS) {
                AvailableGetStateCapability doorLocksCapability = (AvailableGetStateCapability)capability;


                if (doorLocksCapability.getCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    lockButton.setVisibility(View.VISIBLE);
                    if (vehicle.doorsLocked == true) {
                        lockButton.setImageResource(R.drawable.ovr_doorslockedhdpi);
                    }
                    else {
                        lockButton.setImageResource(R.drawable.ovr_doorsunlockedhdpi);
                    }

                    if (doorLocksCapability.getCapability() == AvailableGetStateCapability.Capability.AVAILABLE) {
                        lockButton.setEnabled(true);
                        lockButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                parent.controller.onLockDoorsClicked();
                            }
                        });
                    }
                    else {
                        // get state only available
                        lockButton.setEnabled(false);
                    }
                }
                else {
                    lockButton.setVisibility(View.GONE);
                }
            }
            else if (capability.getIdentifier() == TRUNK_ACCESS) {
                TrunkAccessCapability trunkAccessCapability = (TrunkAccessCapability)capability;
                TrunkState.LockState state = vehicle.trunkLockState;

                if (trunkAccessCapability.getLockCapability() != TrunkAccessCapability.LockCapability.UNAVAILABLE
                    || state == TrunkState.LockState.UNSUPPORTED) {
                    trunkButton.setVisibility(View.VISIBLE);

                    if (state == TrunkState.LockState.LOCKED) {
                        trunkButton.setImageResource(R.drawable.ovr_trunklockedhdpi);
                    }
                    else {
                        trunkButton.setImageResource(R.drawable.ovr_trunkunlockedhdpi);

                        if (trunkAccessCapability.getLockCapability() == TrunkAccessCapability.LockCapability.GET_STATE_UNLOCK_AVAILABLE) {
                            trunkButton.setEnabled(true);

                            trunkButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    parent.controller.onLockTrunkClicked();
                                }
                            });
                        }
                    }

                    if (trunkAccessCapability.getLockCapability() == TrunkAccessCapability.LockCapability.AVAILABLE) {
                        trunkButton.setEnabled(true);
                        trunkButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                parent.controller.onLockTrunkClicked();
                            }
                        });
                    }
                    else if (trunkAccessCapability.getLockCapability() == TrunkAccessCapability.LockCapability.GET_STATE_AVAILABLE) {
                        trunkButton.setEnabled(false);
                    }
                }
                else {
                    trunkButton.setVisibility(View.GONE);
                }
            }
            else if (capability.getIdentifier() == VEHICLE_LOCATION) {
                AvailableCapability locationCapability = (AvailableCapability)capability;

                if (locationCapability.getCapability() != AvailableCapability.Capability.UNAVAILABLE) {
                    gpsIndicatorContainer.setVisibility(View.VISIBLE);
                }
                else {
                    gpsIndicatorContainer.setVisibility(View.GONE);
                }
            }
            else if (capability.getIdentifier() == CHARGING) {
                AvailableGetStateCapability chargingCapability = (AvailableGetStateCapability)capability;

                float batteryPercentage = vehicle.batteryPercentage;

                if (chargingCapability.getCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    batteryIndicatorContainer.setVisibility(View.VISIBLE);
                    batteryIndicatorTextView.setText((int)(batteryPercentage * 100f) + "%");
                }
                else {
                    batteryIndicatorContainer.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_vehicle_overview, container, false);
        ButterKnife.bind(this, v);

        remoteControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.onRemoteControlClicked();
            }
        });
        return v;
    }
}
