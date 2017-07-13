package com.highmobility.exploreautoapis.storage;

import android.util.Log;

import com.highmobility.hmkit.Command.Capability.AvailableCapability;
import com.highmobility.hmkit.Command.Capability.AvailableGetStateCapability;
import com.highmobility.hmkit.Command.Capability.ClimateCapability;
import com.highmobility.hmkit.Command.Capability.FeatureCapability;
import com.highmobility.hmkit.Command.Capability.LightsCapability;
import com.highmobility.hmkit.Command.Capability.RooftopCapability;
import com.highmobility.hmkit.Command.Capability.TrunkAccessCapability;
import com.highmobility.hmkit.Command.Command;

import com.highmobility.hmkit.Command.Incoming.ChargeState;
import com.highmobility.hmkit.Command.Incoming.ClimateState;
import com.highmobility.hmkit.Command.Incoming.IncomingCommand;
import com.highmobility.hmkit.Command.Incoming.LightsState;
import com.highmobility.hmkit.Command.Incoming.LockState;
import com.highmobility.hmkit.Command.Incoming.TrunkState;
import com.highmobility.hmkit.Command.VehicleStatus.Charging;
import com.highmobility.hmkit.Command.VehicleStatus.Climate;
import com.highmobility.hmkit.Command.VehicleStatus.DoorLocks;
import com.highmobility.hmkit.Command.VehicleStatus.FeatureState;
import com.highmobility.hmkit.Command.VehicleStatus.Lights;
import com.highmobility.hmkit.Command.VehicleStatus.RooftopState;
import com.highmobility.hmkit.Command.VehicleStatus.TrunkAccess;

import java.util.ArrayList;

import static com.highmobility.hmkit.Command.Command.Identifier.CHARGING;
import static com.highmobility.hmkit.Command.Command.Identifier.CLIMATE;
import static com.highmobility.hmkit.Command.Command.Identifier.DOOR_LOCKS;
import static com.highmobility.hmkit.Command.Command.Identifier.LIGHTS;
import static com.highmobility.hmkit.Command.Command.Identifier.REMOTE_CONTROL;
import static com.highmobility.hmkit.Command.Command.Identifier.ROOFTOP;
import static com.highmobility.hmkit.Command.Command.Identifier.TRUNK_ACCESS;
import static com.highmobility.hmkit.Command.Command.Identifier.VEHICLE_LOCATION;
import static com.highmobility.exploreautoapis.VehicleActivity.TAG;

/**
 * Created by root on 30/05/2017.
 */

public class VehicleStatus {
    public float insideTemperature;
    public float batteryPercentage;

    public boolean doorsLocked;
    public TrunkState.LockState trunkLockState;
    public TrunkState.Position trunkLockPosition;
    public boolean isWindshieldDefrostingActive;
    public float rooftopDimmingPercentage;
    public float rooftopOpenPercentage;
    public LightsState.FrontExteriorLightState frontExteriorLightState;
    public boolean isRearExteriorLightActive;
    public boolean isInteriorLightActive;
    public int lightsAmbientColor;

    public FeatureCapability[] exteriorCapabilities;
    public FeatureCapability[] overviewCapabilities;

    static VehicleStatus instance;
    public static VehicleStatus getInstance() {
        if (instance == null) {
            instance = new VehicleStatus();
        }

        return instance;
    }

    public void reset() {
        insideTemperature = 0f;
        batteryPercentage = 0f;
        doorsLocked = false;
        trunkLockState = null;
        trunkLockPosition = null;
        isWindshieldDefrostingActive = false;
        rooftopDimmingPercentage = 0f;
        rooftopOpenPercentage = 0f;
        exteriorCapabilities = null;
        overviewCapabilities = null;
        frontExteriorLightState = LightsState.FrontExteriorLightState.INACTIVE;
        isRearExteriorLightActive = false;
        isInteriorLightActive = false;
        lightsAmbientColor = 0;
    }

    public void update(IncomingCommand command) {
        if (command.is(Command.VehicleStatus.VEHICLE_STATUS)) {
            com.highmobility.hmkit.Command.Incoming.VehicleStatus status = (com.highmobility.hmkit.Command.Incoming.VehicleStatus)command;
            FeatureState[] featureStates = status.getFeatureStates();
            if (featureStates == null) {
                Log.e(TAG, "update: null featureStates");
                return;
            }
            for (int i = 0; i < featureStates.length; i++) {
                FeatureState featureState = featureStates[i];
                if (featureState.getFeature() == CLIMATE) {
                    Climate state = (Climate) featureState;
                    insideTemperature = state.getInsideTemperature();
                }
                else if (featureState.getFeature() == CHARGING) {
                    Charging state = (Charging) featureState;
                    batteryPercentage = state.getBatteryLevel();
                }
                else if (featureState.getFeature() == DOOR_LOCKS) {
                    DoorLocks state = (DoorLocks) featureState;
                    doorsLocked = state.isLocked();
                }
                else if (featureState.getFeature() == TRUNK_ACCESS) {
                    TrunkAccess state = (TrunkAccess) featureState;
                    trunkLockState = state.getLockState();
                    trunkLockPosition = state.getPosition();
                }
                else if (featureState.getFeature() == ROOFTOP) {
                    RooftopState state = (RooftopState) featureState;
                    rooftopDimmingPercentage = state.getDimmingPercentage();
                    rooftopOpenPercentage = state.getOpenPercentage();
                }
                else if (featureState.getFeature() == LIGHTS) {
                    Lights lights = (Lights) featureState;
                    frontExteriorLightState = lights.getFrontExteriorLightState();
                    isRearExteriorLightActive = lights.isRearExteriorLightActive();
                    isInteriorLightActive = lights.isInteriorLightActive();
                }
            }
        }
        else if (command.is(Command.VehicleLocation.VEHICLE_LOCATION)) {
            // nothing
        }
        else if (command.is(Command.Climate.CLIMATE_STATE)) {
            ClimateState state = (ClimateState)command;
            isWindshieldDefrostingActive = state.isDefrostingActive();
            insideTemperature = state.getInsideTemperature();
        }
        else if (command.is(Command.RemoteControl.CONTROL_COMMAND)) {

        }
        else if (command.is(Command.DoorLocks.LOCK_STATE)) {
            LockState state = (LockState) command;
            doorsLocked = state.isLocked();
        }
        else if (command.is(Command.TrunkAccess.TRUNK_STATE)) {
            TrunkState state = (TrunkState)command;

            trunkLockState = state.getLockState();
            trunkLockPosition = state.getPosition();
        }
        else if (command.is(Command.RooftopControl.ROOFTOP_STATE)) {
            com.highmobility.hmkit.Command.Incoming.RooftopState state = (com.highmobility.hmkit.Command.Incoming.RooftopState)command;
            rooftopDimmingPercentage = state.getDimmingPercentage();
            rooftopOpenPercentage = state.getOpenPercentage();
        }
        else if (command.is(Command.Charging.CHARGE_STATE)) {
            ChargeState state = (ChargeState)command;
            batteryPercentage = state.getBatteryLevel();
        }
        else if (command.is(Command.Lights.LIGHTS_STATE)) {
            LightsState state = (LightsState)command;
            frontExteriorLightState = state.getFrontExteriorLightState();
            isRearExteriorLightActive = state.isRearExteriorLightActive();
            isInteriorLightActive = state.isInteriorLightActive();
            lightsAmbientColor = state.getAmbientColor();
        }
    }

    // if usingBle is false, remote control capability is not added.
    public void onCapabilitiesReceived(FeatureCapability[] capabilities, boolean usingBle) {
        ArrayList<FeatureCapability> exteriorCapabilities = new ArrayList<>();
        ArrayList<FeatureCapability> overviewCapabilities = new ArrayList<>();

        for (int i = 0; i < capabilities.length; i++) {
            FeatureCapability capability = capabilities[i];
            Command.Identifier feature = capability.getIdentifier();

            if (feature == ROOFTOP) {
                RooftopCapability rooftopCapability = (RooftopCapability)capability;
                if (rooftopCapability.getDimmingCapability() != RooftopCapability.DimmingCapability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == REMOTE_CONTROL && usingBle) {
                AvailableCapability remoteControlCapability = (AvailableCapability)capability;
                if (remoteControlCapability.getCapability() == AvailableCapability.Capability.AVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == CLIMATE) {
                ClimateCapability climateCapability = (ClimateCapability) capability;
                if (climateCapability.getClimateCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == DOOR_LOCKS) {
                AvailableGetStateCapability doorLocksCapability = (AvailableGetStateCapability)capability;
                if (doorLocksCapability.getCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == TRUNK_ACCESS) {
                TrunkAccessCapability trunkAccessCapability = (TrunkAccessCapability)capability;
                if (trunkAccessCapability.getLockCapability() != TrunkAccessCapability.LockCapability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == VEHICLE_LOCATION) {
                AvailableCapability locationCapability = (AvailableCapability)capability;
                if (locationCapability.getCapability() == AvailableCapability.Capability.AVAILABLE) {
                    overviewCapabilities.add(capability);
                }
            }
            else if (feature == CHARGING) {
                AvailableGetStateCapability chargingCapability = (AvailableGetStateCapability)capability;
                if (chargingCapability.getCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                }
            }
            else if (feature == LIGHTS) {
                LightsCapability lightsCapability = (LightsCapability)capability;
                if (lightsCapability.getExteriorLightsCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    exteriorCapabilities.add(lightsCapability);
                }
            }
        }

        this.exteriorCapabilities = exteriorCapabilities.toArray(new FeatureCapability[exteriorCapabilities.size()]);
        this.overviewCapabilities = overviewCapabilities.toArray(new FeatureCapability[overviewCapabilities.size()]);
    }
}
