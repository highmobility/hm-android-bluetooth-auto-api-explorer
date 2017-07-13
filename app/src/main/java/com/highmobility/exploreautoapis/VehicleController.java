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
import com.highmobility.hmkit.Command.Incoming.LightsState;
import com.highmobility.hmkit.Command.Incoming.TrunkState;
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
                "dGVzdPvHnDYCtq0VRgiu99awaqMQAJ5635fC0SYDLBtSyF6T61m7JLYVtbuSw7TtmGE/9y8Om5FxTTJnrjaF0ZPXD8nuW8Umg97SE9uYl/IGfa/hECaxeCuW53HokBxF62l7eE9OYzCiZXri0idXV2iVRr0vZvrMbbm6HQ0x/owW7kLCgO/BROOCriY3i8+Pu2VwXXEel0yg",
                "zhYxSv7TNUipyINravpzmpotN9L7ePGrYw0KhQuBU9I=",
                "HJS8Wh+Gjh2JRB8pMOmQdTMfVR7JoPLVF1U85xjSg7puYoTwLf+DO9Zs67jw+6pXmtkYxynMQm0rfcBU0XFF5A==",
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
            link.setListener(this);
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
        TrunkState.LockState newLockState;
        TrunkState.Position newPosition;

        if (vehicle.trunkLockState == TrunkState.LockState.LOCKED) {
            newLockState = TrunkState.LockState.UNLOCKED;
            newPosition = TrunkState.Position.OPEN;
        }
        else {
            newLockState = TrunkState.LockState.LOCKED;
            newPosition = TrunkState.Position.CLOSED;
        }

        byte[] command = Command.TrunkAccess.setTrunkState(newLockState, newPosition);
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

    public void onSunroofOpenClicked() {
        view.showLoadingView(true);
        sentCommand = Command.RooftopControl.CONTROL_ROOFTOP;

        float openPercentage = vehicle.rooftopOpenPercentage == 0f ? 1f : 0f;
        byte[] command = Command.RooftopControl.controlRooftop(vehicle.rooftopDimmingPercentage, openPercentage);
        sendCommand(command);
    }

    public void onFrontExteriorLightClicked(LightsState.FrontExteriorLightState state) {
        view.showLoadingView(true);
        sentCommand = Command.Lights.CONTROL_LIGHTS;

        byte[] command = Command.Lights.controlLights(state,
                vehicle.isRearExteriorLightActive,
                vehicle.isInteriorLightActive,
                vehicle.lightsAmbientColor);

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
    public void onAuthorizationRequested(ConnectedLink connectedLink, ConnectedLinkListener.AuthorizationCallback callback) {
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

                sentCommand = Command.Lights.GET_LIGHTS_STATE;
                sendCommand(Command.Lights.getLightsState());
            }
            else if (command.is(Command.FailureMessage.FAILURE_MESSAGE)) {
                Failure failure = (Failure)command;
                Log.d(TAG, "failure " + failure.getFailureReason().toString());
                if (sentCommand != null) {
                    if (initializing) {
                        if (sentCommand == Command.Lights.GET_LIGHTS_STATE && failure.getFailureReason() == Failure.Reason.UNSUPPORTED_CAPABILITY) {
                            // never mind that there is no lights capa, continue with init
                            continueInitAfterGetLightsState(null);
                        }
                        else {
                            // initialization failed
                            initializing = false;
                            cancelInitTimer();
                            view.onError(true, "Cannot get vehicle data");
                        }
                    }
                    else {
                        view.showLoadingView(false);
                        view.onError(false, failure.getFailedType().getIdentifier() + " failed: " + failure.getFailureReason());
                        sentCommand = null;
                    }
                }
            }
            else {
                if (initializing) {
                    if (command.is(Command.Lights.LIGHTS_STATE)) {
                        continueInitAfterGetLightsState((LightsState) command);
                        return;
                    }
                    else if (command.is(Command.VehicleStatus.VEHICLE_STATUS)) {
                        cancelInitTimer();
                        initializing = false;
                    }
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

    void continueInitAfterGetLightsState(LightsState state) {
        rescheduleInitTimer();
        sentCommand = Command.VehicleStatus.GET_VEHICLE_STATUS;
        sendCommand(Command.VehicleStatus.getVehicleStatus());
        if (state != null) {
            vehicle.update(state);
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
                        sendCommand(Command.VehicleStatus.getVehicleStatus());
                        if (initTimer != null) rescheduleInitTimer(); // no timer for telematics
                    }
                });
            } else if (sentCommand == Command.Capabilities.GET_CAPABILITIES) {
                view.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
