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

package com.highmobility.exploreautoapis.storage;

import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ChargeState;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandWithProperties;
import com.highmobility.autoapi.ControlCommand;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.RooftopState;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.VehicleLocation;
import com.highmobility.autoapi.property.CapabilityProperty;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;

import java.util.ArrayList;

import static com.highmobility.exploreautoapis.VehicleActivity.TAG;

/**
 * Created by root on 30/05/2017.
 */
public class VehicleStatus {
    public float insideTemperature;
    public float batteryPercentage;

    public boolean doorsLocked;
    public TrunkLockState trunkLockState;
    public TrunkPosition trunkLockPosition;
    public boolean isWindshieldDefrostingActive;
    public float rooftopDimmingPercentage;
    public float rooftopOpenPercentage;
    public FrontExteriorLightState frontExteriorLightState;
    public boolean isRearExteriorLightActive;
    public boolean isInteriorLightActive;
    public int[] lightsAmbientColor;

    public CapabilityProperty[] exteriorCapabilities;
    private Capabilities capabilities;
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
        capabilities = null;
        frontExteriorLightState = FrontExteriorLightState.INACTIVE;
        isRearExteriorLightActive = false;
        isInteriorLightActive = false;
        lightsAmbientColor = null;
    }

    public void update(Command command, boolean usingBle) {
        if (command instanceof com.highmobility.autoapi.VehicleStatus) {
            com.highmobility.autoapi.VehicleStatus status = (com.highmobility.autoapi
                    .VehicleStatus) command;
            CommandWithProperties[] states = status.getStates();

            if (states == null) {
                Log.e(TAG, "update: null featureStates");
                return;
            }

            for (int i = 0; i < states.length; i++) {
                CommandWithProperties subCommand = states[i];
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
        } else if (command instanceof VehicleLocation) {
            // nothing
        } else if (command instanceof ClimateState) {
            ClimateState state = (ClimateState) command;
            isWindshieldDefrostingActive = state.isDefrostingActive();
            insideTemperature = state.getInsideTemperature();
        } else if (command instanceof ControlCommand) {

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

            ArrayList<CapabilityProperty> exteriorCapabilities = new ArrayList<>();
            if (capabilities.isSupported(RooftopState.TYPE)) {
                // if control is not available, then it will just fail
                exteriorCapabilities.add(capabilities.getCapability(RooftopState.TYPE));
            } else if (capabilities.isSupported(ControlCommand.TYPE) && usingBle) {
                exteriorCapabilities.add(capabilities.getCapability(ControlCommand.TYPE));
            } else if (capabilities.isSupported(ClimateState.TYPE)) {
                exteriorCapabilities.add(capabilities.getCapability(ClimateState.TYPE));
            } else if (capabilities.isSupported(LockState.TYPE)) {
                exteriorCapabilities.add(capabilities.getCapability(LockState.TYPE));
            } else if (capabilities.isSupported(TrunkState.TYPE)) {
                exteriorCapabilities.add(capabilities.getCapability(TrunkState.TYPE));
            } else if (capabilities.isSupported(LightsState.TYPE)) {
                exteriorCapabilities.add(capabilities.getCapability(LightsState.TYPE));
            }

            this.exteriorCapabilities = exteriorCapabilities.toArray(new CapabilityProperty[exteriorCapabilities.size()]);
        }
    }

    public boolean isSupported(Type type) {
        return capabilities.isSupported(type);
    }

    public CapabilityProperty[] getCapabilities() {
        return capabilities.getCapabilities();
    }
}
