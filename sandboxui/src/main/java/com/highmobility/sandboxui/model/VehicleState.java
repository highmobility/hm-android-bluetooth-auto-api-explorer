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
package com.highmobility.sandboxui.model;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Charging;
import com.highmobility.autoapi.Climate;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Doors;
import com.highmobility.autoapi.Lights;
import com.highmobility.autoapi.RemoteControl;
import com.highmobility.autoapi.RooftopControl;
import com.highmobility.autoapi.Trunk;
import com.highmobility.autoapi.VehicleStatus;
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
public class VehicleState {
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
    public Lights.State lightsState;

    public int[] lightsAmbientColor;

    private Capabilities.State capabilities;

    public Capabilities.State getCapabilitiesState() {
        return capabilities;
    }

    public void update(Command command) {
        if (command instanceof VehicleStatus.State) {
            VehicleStatus.State status = (com.highmobility.autoapi
                    .VehicleStatus.State) command;

            name = status.getName().getValue();
            Property<Command>[] states = status.getStates();
            if (states == null) {
                e("update: null featureStates");
                return;
            }

            for (int i = 0; i < states.length; i++) {
                if (states[i].getValue() != null) update(states[i].getValue());
            }
        } else if (command instanceof Climate.State) {
            Climate.State state = (Climate.State) command;
            insideTemperature = state.getInsideTemperature().getValue();
            isWindshieldDefrostingActive =
                    state.getDefrostingState().getValue() == ActiveState.ACTIVE;
        } else if (command instanceof Doors.State) {
            Doors.State state = (Doors.State) command;
            doorsLocked = state.getLocksState().getValue() == LockState.LOCKED;
        } else if (command instanceof Trunk.State) {
            Trunk.State state = (Trunk.State) command;
            trunkLockState = state.getLock().getValue();
            trunkLockPosition = state.getPosition().getValue();
        } else if (command instanceof RooftopControl.State) {
            RooftopControl.State state = (RooftopControl.State) command;
            rooftopDimmingPercentage = state.getDimming().getValue();
            rooftopOpenPercentage = state.getPosition().getValue();
        } else if (command instanceof Charging.State) {
            Charging.State state = (Charging.State) command;
            batteryPercentage = state.getBatteryLevel().getValue();
        } else if (command instanceof Lights.State) {
            lightsState = (Lights.State) command;
        } else if (command instanceof Capabilities.State) {
            capabilities = (Capabilities.State) command;
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

    public boolean isSupported(Integer identifier, byte property) {
        return capabilities == null ? false : capabilities.getSupported(identifier, property);
    }

    public boolean isRemoteControlSupported() {
        return isSupported(RemoteControl.IDENTIFIER, RemoteControl.PROPERTY_SPEED) &&
                isSupported(RemoteControl.IDENTIFIER, RemoteControl.PROPERTY_ANGLE);
    }
}
