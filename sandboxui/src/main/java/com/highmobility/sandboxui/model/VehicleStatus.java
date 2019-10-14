package com.highmobility.sandboxui.model;

import com.highmobility.autoapi.CapabilitiesState;
import com.highmobility.autoapi.ChargingState;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.ControlCommand;
import com.highmobility.autoapi.DoorsState;
import com.highmobility.autoapi.Identifier;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.RooftopControlState;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.ActiveState;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.autoapi.value.Position;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Link;

import static timber.log.Timber.e;

/**
 * This class will keep the state of the vehicle according to commands received.
 */
public class VehicleStatus {
    public static final String TAG = "VehicleStatusState";
    // means SDK cannot be terminated
    public static DeviceSerial vehicleConnectedWithBle;

    public static boolean isVehicleConnected(DeviceSerial serial) {
        return vehicleConnectedWithBle != null && serial.equals(vehicleConnectedWithBle) == true;
    }

    public static boolean isVehicleConnectedButNotTo(DeviceSerial serial) {
        return vehicleConnectedWithBle != null && serial.equals(vehicleConnectedWithBle) == false;
    }

    public String name;

    public Float insideTemperature;
    public Double batteryPercentage;

    public Boolean doorsLocked;
    public LockState trunkLockState;
    public Position trunkLockPosition;
    public Boolean isWindshieldDefrostingActive;
    public Double rooftopDimmingPercentage;
    public Double rooftopOpenPercentage;

    // retained for set commands
    public LightsState lightsState;

    public int[] lightsAmbientColor;

    private CapabilitiesState capabilities;

    public CapabilitiesState getCapabilitiesState() {
        return capabilities;
    }

    public void update(Command command) {
        if (command instanceof com.highmobility.autoapi.VehicleStatusState) {
            com.highmobility.autoapi.VehicleStatusState status = (com.highmobility.autoapi
                    .VehicleStatusState) command;

            name = status.getName().getValue();
            Property<Command>[] states = status.getStates();
            if (states == null) {
                e("update: null featureStates");
                return;
            }

            for (int i = 0; i < states.length; i++) {
                if (states[i].getValue() != null) update(states[i].getValue());
            }
        } else if (command instanceof ClimateState) {
            ClimateState state = (ClimateState) command;
            insideTemperature = state.getInsideTemperature().getValue();
            isWindshieldDefrostingActive = state.getDefrostingState().getValue() == ActiveState.ACTIVE;
        } else if (command instanceof DoorsState) {
            DoorsState state = (DoorsState) command;
            doorsLocked = state.getLocksState().getValue() == LockState.LOCKED;
        } else if (command instanceof TrunkState) {
            TrunkState state = (TrunkState) command;
            trunkLockState = state.getLock().getValue();
            trunkLockPosition = state.getPosition().getValue();
        } else if (command instanceof RooftopControlState) {
            RooftopControlState state = (RooftopControlState) command;
            rooftopDimmingPercentage = state.getDimming().getValue();
            rooftopOpenPercentage = state.getPosition().getValue();
        } else if (command instanceof ChargingState) {
            ChargingState state = (ChargingState) command;
            batteryPercentage = state.getBatteryLevel().getValue();
        } else if (command instanceof LightsState) {
            lightsState = (LightsState) command;
        } else if (command instanceof CapabilitiesState) {
            capabilities = (CapabilitiesState) command;
        }
    }

    public void onLinkAuthenticated(Link link) {
        // when link first connects, it has no serial(auth gives serial). We use the serial from
        // backend.
        vehicleConnectedWithBle = link.getSerial();
    }

    public void onLinkConnected(Link link) {
        // we do not have a serial yet.

        // Could be that previously link connected but did not authenticate and its serial
        // is null(vehicleConnectedWithBle not set), this means we will start broadcasting
        // when a link is connected. Currently the SDK does not have a fix to it.

        vehicleConnectedWithBle = new DeviceSerial("000000000000000000");
    }

    public boolean isSupported(Identifier identifier, byte property) {
        return capabilities == null ? false : capabilities.getSupported(identifier, property);
    }

    public boolean isRemoteControlSupported() {
        return isSupported(ControlCommand.IDENTIFIER, ControlCommand.IDENTIFIER_SPEED) &&
                isSupported(ControlCommand.IDENTIFIER, ControlCommand.IDENTIFIER_ANGLE);
    }
}
