package com.highmobility.exploreautoapis;

import android.content.Intent;
import android.util.Log;

import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.Command.Command;
import com.highmobility.hmkit.Command.CommandParseException;
import com.highmobility.hmkit.Command.Incoming.Capabilities;
import com.highmobility.hmkit.Command.Incoming.Failure;
import com.highmobility.hmkit.Command.Incoming.IncomingCommand;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Constants;
import com.highmobility.hmkit.Error.BroadcastError;
import com.highmobility.hmkit.Error.LinkError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;
import com.highmobility.exploreautoapis.remotecontrol.RemoteControlActivity;
import com.highmobility.exploreautoapis.storage.VehicleStatus;

import java.util.Timer;
import java.util.TimerTask;

import static com.highmobility.hmkit.Command.Constants.TrunkLockState;
import static com.highmobility.exploreautoapis.VehicleActivity.REQUEST_CODE_REMOTE_CONTROL;
import static com.highmobility.exploreautoapis.VehicleActivity.TAG;
import static com.highmobility.exploreautoapis.remotecontrol.RemoteControlController.LINK_IDENTIFIER_MESSAGE;


/**
 * Created by root on 24/05/2017.
 */

public class VehicleController implements BroadcasterListener, ConnectedLinkListener {
    Timer initTimer;
    int retryCount;
    VehicleStatus vehicle;

    Manager manager;
    IVehicleView view;
    Command.Type sentCommand;

    TimerTask repeatTask;
    boolean initializing;

    Broadcaster broadcaster;
    ConnectedLink link;

    public VehicleController(IVehicleView view) {
        manager = Manager.getInstance();
        Manager.getInstance().initialize(
                "dGVzdIvKUwG9f5n6Ecvql8AAaI0jemi8XwhG5Acf6zcmVKOaMjUkRijxaovhsHtEHsPVzLZWGXYGfnnnhS0qemu7pxqU5/qDGy/dpeZ6zloiN/4OnC234azF5rEakNWtQ4f5vDV2ji1JzCWYa1/+g8CJOLuqa+qBIrP8CzqklTCER3wE2YsMzH4u6zqJq5kFyEv5vP3OY6u6",
                "P2hvDNWo1/TatePGjKYkVvOY38zr86VQ8Qfe6pJOwH8=",
                "qQMAui2c/PJX898ZVJNdCfJXh8hs41yOT0URcThBEXsfBkvrKV8072XluC1YaE25arNKXv8Ig5+XyDTbCfVGOg==",
                view.getActivity()
        );

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
            view.onVehicleStatusUpdate(vehicle);
        }
        // else link disconnected
    }

    public void onLockDoorsClicked() {
        view.showLoadingView(true);
        sentCommand = Command.DoorLocks.LOCK_UNLOCK;
        sendCommand(Command.DoorLocks.lockDoors(vehicle.doorsLocked == true ? false : true));
    }

    public void onLockTrunkClicked() {
        view.showLoadingView(true);
        sentCommand = Command.TrunkAccess.OPEN_CLOSE;
        TrunkLockState newLockState;
        if (vehicle.trunkLockState == TrunkLockState.LOCKED) {
            newLockState = TrunkLockState.UNLOCKED;
        }
        else {
            newLockState = TrunkLockState.LOCKED;
        }

        byte[] command = Command.TrunkAccess.setTrunkState(newLockState, vehicle.trunkLockPosition);
        sendCommand(command);
    }

    public void onWindshieldDefrostingClicked() {
        view.showLoadingView(true);
        sentCommand = Command.Climate.START_STOP_DEFROSTING;
        byte[] command = Command.Climate.startDefrost(vehicle.isWindshieldDefrostingActive ? false : true);
        sendCommand(command);
    }

    public void onSunroofVisibilityClicked() {
        view.showLoadingView(true);
        sentCommand = Command.RooftopControl.CONTROL_ROOFTOP;

        float dimPercentage = vehicle.rooftopDimmingPercentage == 1f ? 0f : 1f;
        byte[] command = Command.RooftopControl.controlRooftop(dimPercentage, vehicle.rooftopOpenPercentage);
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
                onCommandError(linkError.getCode(), linkError.getMessage()); // TODO: use type
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
                    view.showBleInfoView(true, "Looking for links... " + manager.getBroadcaster().getName());
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
        }
        else {
            Log.d(TAG, "unknown link lost");
        }
    }

    @Override
    public void onAuthorizationRequested(ConnectedLink connectedLink, ConnectedLink.AuthorizationCallback callback) {
        callback.approve();
    }

    @Override
    public void onAuthorizationTimeout(ConnectedLink connectedLink) {
        view.onError(true, "authorization request timed out");
    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        Log.d(TAG, "link state changed " + link.getState());
        if (link == this.link ) {
            if (link.getState() == Link.State.AUTHENTICATED) {
                vehicle.reset();
                view.showBleInfoView(false, "link: " + "authenticated");
                view.showLoadingView(true);
                initializing = true;
                sentCommand = Command.Capabilities.GET_CAPABILITIES;
                sendCommand(Command.Capabilities.getCapabilities());
            }
            else if (link.getState() == Link.State.CONNECTED) {
                view.showBleInfoView(true, "link: " + "connected");
            }
            else {
                this.link = null;
                onStateChanged(broadcaster.getState());
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
            IncomingCommand command = IncomingCommand.create(bytes);

            if (command.is(Command.Capabilities.CAPABILITIES)) {
                vehicle.onCapabilitiesReceived(((Capabilities) command).getCapabilites(), true);
                view.onCapabilitiesUpdate(vehicle);
                // capabilities are asked only on initialization, follow it by get VS

                sentCommand = Command.VehicleStatus.GET_VEHICLE_STATUS;
                sendCommand(Command.VehicleStatus.getVehicleStatus());
            }
            else if (command.is(Command.FailureMessage.FAILURE_MESSAGE)) {
                Failure failure = (Failure)command;
                Log.d(TAG, "failure " + failure.getFailureReason().toString());
                if (sentCommand != null) {
                    view.showLoadingView(false);

                    if (initializing) {
                        // initialization failed
                        initializationFailed("Cannot get vehicle data: " + failure.getFailureReason());
                    }
                    else {
                        view.onError(false, failure.getFailedType().getIdentifier() + " failed: " + failure.getFailureReason());
                        sentCommand = null;
                    }
                }
            }
            else {
                if (command.is(Command.VehicleStatus.VEHICLE_STATUS)) {
                    cancelInitTimer();
                    initializing = false;
                }

                sentCommand = null;
                vehicle.update(command);
                view.onVehicleStatusUpdate(vehicle);
                view.showLoadingView(false);
            }
        }
        catch (CommandParseException e) {
            Log.d(TAG, "IncomingCommand parse exception ", e);
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

    void failedToSendInitCommand(String message) {
        retryCount++;
        if (retryCount == 3) {
            initializationFailed(message);
            return;
        }

        Log.d(TAG, "init: try to send the command again " + (sentCommand != null ? sentCommand.getIdentifier() : "null command"));
        if (sentCommand != null) {
            // try to send command again
            if (sentCommand == Command.VehicleStatus.GET_VEHICLE_STATUS) {
                view.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "send vs");
                        sendCommand(Command.VehicleStatus.getVehicleStatus());
                        if (initTimer != null) rescheduleInitTimer(); // no timer for telematics
                    }
                });
            } else if (sentCommand == Command.Capabilities.GET_CAPABILITIES) {
                view.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "send capa");
                        sendCommand(Command.Capabilities.getCapabilities());
                        if (initTimer != null) rescheduleInitTimer();
                    }
                });
            }
        }
    }

    void initializationFailed(String message) {
        initializing = false;
        retryCount = 0;
        cancelInitTimer();
        view.onError(true, message);
        broadcaster.stopBroadcasting();
        startBroadcasting();
    }

    void rescheduleInitTimer() {
        cancelInitTimer();
        Log.d(TAG, "rescheduleInitTimer: ");
        initTimer = new Timer();
        if (repeatTask != null) repeatTask.cancel();
        repeatTask = repeatTask();
        initTimer.schedule(repeatTask, (long) ((Constants.commandTimeout + 5) * 1000));
    }

    void onCommandError(int errorCode, String message) {
        if (initializing == true &&
                (sentCommand == Command.VehicleStatus.GET_VEHICLE_STATUS
                || sentCommand == Command.Capabilities.GET_CAPABILITIES )) {
            Log.d(TAG, "initialize, onCommandError: " + errorCode + " " + message);
            failedToSendInitCommand(message);
            view.onError(true, "Cannot get data from vehicle: " + errorCode);
        }
        else if (sentCommand != null) {
            Log.d(TAG, "onCommandError: " + initializing);
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
