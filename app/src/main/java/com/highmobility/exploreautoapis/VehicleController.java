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

import android.content.Intent;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandParseException;

import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetLightsState;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.OpenCloseTrunk;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.DoorLockProperty;
import com.highmobility.autoapi.property.FailureReason;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;
import com.highmobility.exploreautoapis.remotecontrol.RemoteControlActivity;
import com.highmobility.exploreautoapis.storage.VehicleStatus;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Constants;
import com.highmobility.hmkit.Error.BroadcastError;
import com.highmobility.hmkit.Error.LinkError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;

import java.util.Timer;
import java.util.TimerTask;

import static com.highmobility.exploreautoapis.VehicleActivity.REQUEST_CODE_REMOTE_CONTROL;
import static com.highmobility.exploreautoapis.VehicleActivity.TAG;
import static com.highmobility.exploreautoapis.remotecontrol.RemoteControlController
        .LINK_IDENTIFIER_MESSAGE;


/**
 * Created by root on 24/05/2017.
 */

public class VehicleController implements BroadcasterListener, ConnectedLinkListener {
    private final IVehicleView view;
    Timer initTimer;
    int retryCount;
    VehicleStatus vehicle;

    Manager manager;
    Type sentCommand;

    TimerTask repeatTask;
    boolean initializing;

    Broadcaster broadcaster;
    ConnectedLink link;

    public VehicleController(IVehicleView view) {
        manager = Manager.getInstance();

        /*
         * Before using HMKit, you'll have to initialise the Manager singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   Manager.getInstance().initialize(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         */

        // PASTE THE SNIPPET HERE

        this.view = view;
        vehicle = VehicleStatus.getInstance();

        broadcaster = Manager.getInstance().getBroadcaster();
        broadcaster.setListener(this);

        startBroadcasting();
    }

    public void startRemoteControl() {
        Intent i = new Intent(view.getActivity(), RemoteControlActivity.class);
        i.putExtra(LINK_IDENTIFIER_MESSAGE, link.getSerial());
        view.getActivity().startActivityForResult(i, REQUEST_CODE_REMOTE_CONTROL);
    }

    public void onReturnFromRemoteControl() {
        if (link != null) {
            // take over the link listener, update the views.
            link.setListener(this);
            view.onVehicleStatusUpdate(vehicle);
        }
        // else link disconnected
    }

    public void onLockDoorsClicked() {
        view.showLoadingView(true);
        sentCommand = LockUnlockDoors.TYPE;
        DoorLockProperty.LockState lockState = vehicle.doorsLocked == true ? DoorLockProperty
                .LockState.UNLOCKED : DoorLockProperty.LockState.LOCKED;
        sendCommand(new LockUnlockDoors(lockState).getBytes());
    }

    public void onLockTrunkClicked() {
        view.showLoadingView(true);
        sentCommand = OpenCloseTrunk.TYPE;
        TrunkLockState newLockState;
        TrunkPosition newPosition;

        if (vehicle.trunkLockState == TrunkLockState.LOCKED) {
            newLockState = TrunkLockState.UNLOCKED;
            newPosition = TrunkPosition.OPEN;
        } else {
            newLockState = TrunkLockState.LOCKED;
            newPosition = TrunkPosition.CLOSED;
        }

        byte[] command = new OpenCloseTrunk(newLockState, newPosition).getBytes();
        sendCommand(command);
    }

    public void onWindshieldDefrostingClicked() {
        view.showLoadingView(true);
        sentCommand = StartStopDefrosting.TYPE;

        byte[] command = new StartStopDefrosting(vehicle.isWindshieldDefrostingActive ?
                false : true).getBytes();
        sendCommand(command);
    }

    public void onSunroofVisibilityClicked() {
        view.showLoadingView(true);
        sentCommand = ControlRooftop.TYPE;

        float dimPercentage = vehicle.rooftopDimmingPercentage == 1f ? 0f : 1f;
        byte[] command = new ControlRooftop(dimPercentage, vehicle
                .rooftopOpenPercentage).getBytes();
        sendCommand(command);
    }

    public void onSunroofOpenClicked() {
        view.showLoadingView(true);
        sentCommand = ControlRooftop.TYPE;

        float openPercentage = vehicle.rooftopOpenPercentage == 0f ? 1f : 0f;
        byte[] command = new ControlRooftop(vehicle
                .rooftopDimmingPercentage, openPercentage).getBytes();

        sendCommand(command);
    }

    public void onFrontExteriorLightClicked(FrontExteriorLightState state) {
        view.showLoadingView(true);
        sentCommand = ControlLights.TYPE;

        byte[] command = new ControlLights(state,
                vehicle.isRearExteriorLightActive,
                vehicle.isInteriorLightActive,
                vehicle.lightsAmbientColor).getBytes();

        sendCommand(command);
    }

    void sendCommand(byte[] command) {
        link.sendCommand(command, new Link.CommandCallback() {
            @Override
            public void onCommandSent() {
                onBleAckReceived();
            }

            @Override
            public void onCommandFailed(LinkError linkError) {
                onCommandError(1, linkError.getType() + " " + linkError.getMessage());
            }
        });
    }

    // Broadcaster listener

    @Override
    public void onStateChanged(Broadcaster.State state) {
        Log.d(TAG, "onStateChanged: set state");
        switch (broadcaster.getState()) {
            case IDLE:
                view.showBleInfoView(true, "Idle");

                if (state == Broadcaster.State.BLUETOOTH_UNAVAILABLE) {
                    startBroadcasting();
                }
                break;
            case BLUETOOTH_UNAVAILABLE:
                view.showBleInfoView(true, "Bluetooth N/A");
                break;
            case BROADCASTING:
                if (link == null) {
                    view.showBleInfoView(true, "Looking for links... " + manager.getBroadcaster()
                            .getName());
                }
                break;
        }
    }

    @Override
    public void onLinkReceived(ConnectedLink connectedLink) {
        if (link != null) {
            Log.d(TAG, "received new link, ignore");
            return;
        }

        link = connectedLink;
        link.setListener(this);
        view.showBleInfoView(true, "link: " + connectedLink.getState());
        Log.d(TAG, "onLinkReceived: ");
    }

    // Link listener

    @Override
    public void onLinkLost(ConnectedLink connectedLink) {
        if (connectedLink == link) {
            link.setListener(null);
            link = null;
            onStateChanged(broadcaster.getState());
        } else {
            Log.d(TAG, "unknown link lost");
        }
    }

    @Override
    public void onAuthorizationRequested(ConnectedLink connectedLink, ConnectedLinkListener
            .AuthorizationCallback callback) {
        callback.approve();
    }

    @Override
    public void onAuthorizationTimeout(ConnectedLink connectedLink) {
        view.onError(true, "authorization request timed out");
    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        Log.d(TAG, "link state changed " + link.getState());
        if (link == this.link) {
            if (link.getState() == Link.State.AUTHENTICATED) {
                vehicle.reset();
                view.showBleInfoView(false, "link: " + "authenticated");
                view.showLoadingView(true);
                initializing = true;
                sentCommand = GetCapabilities.TYPE;
                sendCommand(new GetCapabilities().getBytes());
            } else if (link.getState() == Link.State.CONNECTED) {
                view.showBleInfoView(true, "link: " + "connected");
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, byte[] bytes) {
        onCommandReceived(bytes);
    }

    void startBroadcasting() {
        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {

            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                Log.e(TAG, "cant start broadcasting " + broadcastError.getType());
            }
        });
    }

    void onBleAckReceived() {
        if (initializing == true) {
            rescheduleInitTimer();
        }
    }

    void onCommandReceived(byte[] bytes) {
        try {
            Command command = CommandResolver.resolve(bytes);
            vehicle.update(command, true);

            if (command instanceof Capabilities) {
                rescheduleInitTimer();

                view.onCapabilitiesUpdate(vehicle);

                // capabilities are asked only on initialization, follow it by get Lights
                if (vehicle.isSupported(LightsState.TYPE)) {
                    sentCommand = GetLightsState.TYPE;
                    sendCommand(new GetLightsState().getBytes());
                } else {
                    continueInitAfterGetLightsState(null);
                }
            } else if (command instanceof Failure) {
                Failure failure = (Failure) command;
                Log.d(TAG, "failure " + failure.getFailureReason());

                if (sentCommand != null) {
                    if (initializing) {
                        if (sentCommand == GetLightsState.TYPE && failure
                                .getFailureReason() == FailureReason.UNSUPPORTED_CAPABILITY) {
                            // never mind that there is no lights capa, continue with init
                            continueInitAfterGetLightsState(null);
                        } else {
                            // initialization failed
                            initializing = false;
                            cancelInitTimer();
                            view.onError(true, "Cannot get vehicle data: " + failure
                                    .getFailureReason());
                        }
                    } else {
                        view.showLoadingView(false);
                        view.onError(false, failure.getFailedType() + " failed: "
                                + failure.getFailureReason());
                        sentCommand = null;
                    }
                }
            } else {
                if (initializing) {
                    if (command instanceof LightsState) {
                        continueInitAfterGetLightsState((LightsState) command);
                        return;
                    } else if (command instanceof com.highmobility.autoapi.VehicleStatus) {
                        cancelInitTimer();
                        initializing = false;
                    }
                }

                sentCommand = null;
                view.onVehicleStatusUpdate(vehicle);
                view.showLoadingView(false);
            }

        } catch (CommandParseException e) {
            Log.d(TAG, "IncomingCommand parse exception ", e);
        }
    }

    void continueInitAfterGetLightsState(LightsState state) {
        rescheduleInitTimer();
        sentCommand = GetVehicleStatus.TYPE;
        sendCommand(new GetVehicleStatus().getBytes());
        if (state != null) {
            vehicle.update(state, true);
            view.onVehicleStatusUpdate(vehicle);
        }
    }

    void cancelInitTimer() {
        if (initTimer != null) {
            Log.d(TAG, "cancelInitTimer:");
            repeatTask.cancel();
            initTimer.cancel();
            initTimer = null;
        }
    }

    TimerTask repeatTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "run: app layer command wait timeout");
                failedToSendInitCommand("command timed out.");
            }
        };
    }

    void failedToSendInitCommand(final String message) {
        view.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                retryCount++;
                if (retryCount == 3) {
                    initializing = false;
                    view.onError(true, message);
                    retryCount = 0;
                    return;
                }

                Log.d(TAG, "init: try to send the command again " + (sentCommand != null ?
                        sentCommand : "null command"));
                if (sentCommand != null) {
                    // try to send command again
                    if (sentCommand == GetVehicleStatus.TYPE) {
                        Log.d(TAG, "send vs");
                        sendCommand(new GetVehicleStatus().getBytes());
                    } else if (sentCommand == GetCapabilities.TYPE) {
                        Log.d(TAG, "send capa");
                        sendCommand(new GetCapabilities().getBytes());
                    } else if (sentCommand == GetLightsState.TYPE) {
                        Log.d(TAG, "send lights");
                        sendCommand(new GetLightsState().getBytes());
                    }

                    if (initTimer != null) rescheduleInitTimer(); // no timer for telematics
                }
            }
        });
    }

    void rescheduleInitTimer() {
        cancelInitTimer();
        Log.d(TAG, "rescheduleInitTimer: ");
        initTimer = new Timer();
        if (repeatTask != null) repeatTask.cancel();
        repeatTask = repeatTask();
        initTimer.schedule(repeatTask, (long) (120 * 1000));
    }

    void onCommandError(int errorCode, String message) {
        if (initializing == true &&
                (sentCommand == GetVehicleStatus.TYPE
                        || sentCommand == GetCapabilities.TYPE
                        || sentCommand == GetLightsState.TYPE)) {
            Log.d(TAG, "initialize, onCommandError: " + errorCode + " " + message);
            failedToSendInitCommand(message);
        } else if (sentCommand != null) {
            view.showLoadingView(false);
            view.onError(false, message);
            sentCommand = null;
        }
    }

    void onDestroy() {
        cancelInitTimer();
        manager.terminate();
        Log.d(TAG, "onDestroy: ");
        broadcaster = null;
    }
}
