package com.highmobility.exploreautoapis;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.highmobility.hmkit.Command.Capability.AvailableCapability;
import com.highmobility.hmkit.Command.Capability.AvailableGetStateCapability;
import com.highmobility.hmkit.Command.Capability.ClimateCapability;
import com.highmobility.hmkit.Command.Capability.FeatureCapability;
import com.highmobility.hmkit.Command.Capability.RooftopCapability;
import com.highmobility.hmkit.Command.Capability.TrunkAccessCapability;
import com.highmobility.hmkit.Command.Constants;
import com.highmobility.exploreautoapis.storage.VehicleStatus;
import com.highmobility.exploreautoapis.view.CircleButton;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.highmobility.hmkit.Command.Command.Identifier.CHARGING;
import static com.highmobility.hmkit.Command.Command.Identifier.CLIMATE;
import static com.highmobility.hmkit.Command.Command.Identifier.DOOR_LOCKS;
import static com.highmobility.hmkit.Command.Command.Identifier.REMOTE_CONTROL;
import static com.highmobility.hmkit.Command.Command.Identifier.ROOFTOP;
import static com.highmobility.hmkit.Command.Command.Identifier.TRUNK_ACCESS;
import static com.highmobility.hmkit.Command.Command.Identifier.VEHICLE_LOCATION;

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
                Constants.TrunkLockState state = vehicle.trunkLockState;

                if (trunkAccessCapability.getLockCapability() != TrunkAccessCapability.LockCapability.UNAVAILABLE
                    || state == Constants.TrunkLockState.UNSUPPORTED) {
                    trunkButton.setVisibility(View.VISIBLE);

                    if (state == Constants.TrunkLockState.LOCKED) {
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
