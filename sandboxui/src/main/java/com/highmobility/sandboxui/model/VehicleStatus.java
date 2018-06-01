package com.highmobility.sandboxui.model;

import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ChargeState;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.RooftopState;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.CapabilityProperty;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;
import com.highmobility.hmkit.Link;
import com.highmobility.value.DeviceSerial;

/**
 * This class will keep the state of the vehicle according to commands received.
 */
public class VehicleStatus {
    public static final String TAG = "VehicleStatus";
    // means SDK cannot be terminated
    public static byte[] vehicleConnectedWithBle;

    public static boolean isVehicleConnectedWithBle(DeviceSerial serial) {
        return vehicleConnectedWithBle != null && serial.equals(vehicleConnectedWithBle) == true;
    }

    public Float insideTemperature;
    public Float batteryPercentage;

    public Boolean doorsLocked;
    public TrunkLockState trunkLockState;
    public TrunkPosition trunkLockPosition;
    public Boolean isWindshieldDefrostingActive;
    public Float rooftopDimmingPercentage;
    public Float rooftopOpenPercentage;
    public FrontExteriorLightState frontExteriorLightState;
    // unused
    public Boolean isRearExteriorLightActive;
    public Boolean isInteriorLightActive;
    public int[] lightsAmbientColor;

    private Capabilities capabilities;

    public CapabilityProperty[] getCapabilities() {
        return capabilities.getCapabilities();
    }

    public void update(Command command) {
        if (command instanceof com.highmobility.autoapi.VehicleStatus) {
            com.highmobility.autoapi.VehicleStatus status = (com.highmobility.autoapi
                    .VehicleStatus) command;

            Command[] states = status.getStates();

            if (states == null) {
                Log.e(TAG, "update: null featureStates");
                return;
            }

            for (int i = 0; i < states.length; i++) {
                Command subCommand = states[i];
                if (subCommand instanceof ClimateState) {
                    ClimateState state = (ClimateState) subCommand;
                    insideTemperature = state.getInsideTemperature();
                    isWindshieldDefrostingActive = state.isDefrostingActive();
                } else if (subCommand instanceof ChargeState) {
                    ChargeState state = (ChargeState) subCommand;
                    batteryPercentage = state.getBatteryLevel();
                } else if (subCommand instanceof LockState) {
                    LockState state = (LockState) subCommand;
                    doorsLocked = state.isLocked();
                } else if (subCommand instanceof TrunkState) {
                    TrunkState state = (TrunkState) subCommand;
                    trunkLockState = state.getLockState();
                    trunkLockPosition = state.getPosition();
                } else if (subCommand instanceof RooftopState) {
                    RooftopState state = (RooftopState) subCommand;
                    rooftopDimmingPercentage = state.getDimmingPercentage();
                    rooftopOpenPercentage = state.getOpenPercentage();
                } else if (subCommand instanceof LightsState) {
                    LightsState lights = (LightsState) subCommand;
                    frontExteriorLightState = lights.getFrontExteriorLightState();
                    isRearExteriorLightActive = lights.isRearExteriorLightActive();
                    isInteriorLightActive = lights.isInteriorLightActive();
                }
            }
        } else if (command instanceof ClimateState) {
            ClimateState state = (ClimateState) command;
            insideTemperature = state.getInsideTemperature();
            isWindshieldDefrostingActive = state.isDefrostingActive();
        } else if (command instanceof LockState) {
            LockState state = (LockState) command;
            doorsLocked = state.isLocked();
        } else if (command instanceof TrunkState) {
            TrunkState state = (TrunkState) command;
            trunkLockState = state.getLockState();
            trunkLockPosition = state.getPosition();
        } else if (command instanceof RooftopState) {
            RooftopState state = (RooftopState) command;
            rooftopDimmingPercentage = state.getDimmingPercentage();
            rooftopOpenPercentage = state.getOpenPercentage();
        } else if (command instanceof ChargeState) {
            ChargeState state = (ChargeState) command;
            batteryPercentage = state.getBatteryLevel();
        } else if (command instanceof LightsState) {
            LightsState state = (LightsState) command;
            frontExteriorLightState = state.getFrontExteriorLightState();
            isRearExteriorLightActive = state.isRearExteriorLightActive();
            isInteriorLightActive = state.isInteriorLightActive();
            lightsAmbientColor = state.getAmbientColor();
        } else if (command instanceof Capabilities) {
            capabilities = (Capabilities) command;
            if (isSupported(LockUnlockDoors.TYPE)) {
                doorsLocked = false;
            }
        }
    }

    public void onLinkAuthenticated(Link link) {
        vehicleConnectedWithBle = link.getSerial().getByteArray();
    }

    public void onLinkReceived() {
        vehicleConnectedWithBle = new byte[]{0x00};
    }

    public boolean isSupported(Type type) {
        return capabilities == null ? false : capabilities.isSupported(type);
    }
}
