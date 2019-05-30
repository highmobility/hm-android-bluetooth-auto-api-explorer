package com.highmobility.sandboxui.model;



import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ChargeState;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.RooftopState;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.Lock;
import com.highmobility.autoapi.value.Position;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Link;

import static timber.log.Timber.e;

/**
 * This class will keep the state of the vehicle according to commands received.
 */
public class VehicleStatus {
    public static final String TAG = "VehicleStatus";
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
    public Lock trunkLockState;
    public Position trunkLockPosition;
    public Boolean isWindshieldDefrostingActive;
    public Double rooftopDimmingPercentage;
    public Double rooftopOpenPercentage;

    // retained for set commands
    public LightsState lightsState;

    public int[] lightsAmbientColor;

    private Capabilities capabilities;

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void update(Command command) {
        if (command instanceof com.highmobility.autoapi.VehicleStatus) {
            com.highmobility.autoapi.VehicleStatus status = (com.highmobility.autoapi
                    .VehicleStatus) command;

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
            isWindshieldDefrostingActive = state.isDefrostingActive().getValue();
        } else if (command instanceof LockState) {
            LockState state = (LockState) command;
            doorsLocked = state.isLocked();
        } else if (command instanceof TrunkState) {
            TrunkState state = (TrunkState) command;
            trunkLockState = state.getLockState().getValue();
            trunkLockPosition = state.getPosition().getValue();
        } else if (command instanceof RooftopState) {
            RooftopState state = (RooftopState) command;
            rooftopDimmingPercentage = state.getDimmingPercentage().getValue();
            rooftopOpenPercentage = state.getOpenPercentage().getValue();
        } else if (command instanceof ChargeState) {
            ChargeState state = (ChargeState) command;
            batteryPercentage = state.getBatteryLevel().getValue();
        } else if (command instanceof LightsState) {
            lightsState = (LightsState) command;
        } else if (command instanceof Capabilities) {
            capabilities = (Capabilities) command;
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

    public boolean isSupported(Type type) {
        return capabilities == null ? false : capabilities.isSupported(type);
    }
}
