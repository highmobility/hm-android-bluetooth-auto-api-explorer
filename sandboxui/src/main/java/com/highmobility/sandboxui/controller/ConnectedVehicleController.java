package com.highmobility.sandboxui.controller;

import android.content.Intent;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.OpenCloseTrunk;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;
import com.highmobility.autoapi.property.doors.DoorLock;
import com.highmobility.crypto.AccessCertificate;
import com.highmobility.hmkit.Manager;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.model.VehicleStatus;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;
import com.highmobility.crypto.value.DeviceSerial;

import java.util.Timer;
import java.util.TimerTask;

import static com.highmobility.autoapi.property.doors.DoorLock.LOCKED;
import static com.highmobility.autoapi.property.doors.DoorLock.UNLOCKED;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleController {
    public static final String EXTRA_SERIAL = "EXTRA_SERIAL";
    public static final String EXTRA_USE_BLE = "EXTRA_USE_BLE";
    public static final String EXTRA_SERVICE_NAME = "EXTRA_SERVICE_NAME";
    public static final String EXTRA_ALIVE_PING_AMOUNT_NAME = "EXTRA_ALIVE";

    public boolean useBle;
    public String serviceName;

    public static VehicleStatus vehicle;
    public AccessCertificate certificate;

    Timer initTimer;
    int retryCount;
    DeviceSerial vehicleSerial;

    Manager manager;
    IConnectedVehicleView view;
    Type sentCommand;

    TimerTask repeatTask;
    boolean initializing;

    public static ConnectedVehicleController create(IConnectedVehicleView view,
                                                    IConnectedVehicleBleView bleView, Intent
                                                            intent) {
        byte[] vehicleSerialBytes = intent.getByteArrayExtra(EXTRA_SERIAL);
        DeviceSerial vehicleSerial;
        boolean useBle = intent.getBooleanExtra(EXTRA_USE_BLE, true);

        String serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
        if (serviceName == null) serviceName = "High-Mobility";

        AccessCertificate cert;
        if (vehicleSerialBytes != null) {
            vehicleSerial = new DeviceSerial(vehicleSerialBytes);
            cert = Manager.getInstance().getCertificate(new DeviceSerial(vehicleSerialBytes));
        } else {
            AccessCertificate[] certificates = Manager.getInstance().getCertificates();
            if (certificates == null || certificates.length < 1)
                throw new IllegalStateException("Manager not initialized");
            cert = Manager.getInstance().getCertificates()[0];
            vehicleSerial = cert.getGainerSerial();
        }

        ConnectedVehicleController controller;

        if (useBle) {
            int alivePingInterval = intent.getIntExtra(EXTRA_ALIVE_PING_AMOUNT_NAME, -1);
            controller = new ConnectedVehicleBleController(view, bleView, alivePingInterval);
        } else {
            controller = new ConnectedVehicleTelematicsController(view);
        }

        controller.certificate = cert;
        controller.useBle = useBle;
        controller.vehicleSerial = vehicleSerial;
        controller.serviceName = serviceName;

        return controller;
    }

    ConnectedVehicleController(boolean useBle, IConnectedVehicleView view) {
        manager = Manager.getInstance();
        this.view = view;
        this.useBle = useBle;
        vehicle = new VehicleStatus();
    }

    public void init() {
    }

    public void onLockDoorsClicked() {
        view.showLoadingView(true);
        sentCommand = LockUnlockDoors.TYPE;
        DoorLock lockState = vehicle.doorsLocked == true ? UNLOCKED : LOCKED;
        sendCommand(new LockUnlockDoors(lockState));
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

        Command command = new OpenCloseTrunk(newLockState, newPosition);
        sendCommand(command);
    }

    public void onWindshieldDefrostingClicked() {
        view.showLoadingView(true);
        sentCommand = StartStopDefrosting.TYPE;
        Command command = new StartStopDefrosting(vehicle.isWindshieldDefrostingActive ?
                false : true);
        sendCommand(command);
    }

    public void onSunroofVisibilityClicked() {
        view.showLoadingView(true);
        sentCommand = ControlRooftop.TYPE;

        float dimPercentage = vehicle.rooftopDimmingPercentage == 1f ? 0f : 1f;

        Command command = new ControlRooftop(dimPercentage, vehicle
                .rooftopOpenPercentage);
        sendCommand(command);
    }

    public void onSunroofOpenClicked() {
        view.showLoadingView(true);
        sentCommand = ControlRooftop.TYPE;
        float openPercentage = vehicle.rooftopOpenPercentage == 0f ? 1f : 0f;
        Command command = new ControlRooftop(vehicle
                .rooftopDimmingPercentage, openPercentage);
        sendCommand(command);
    }

    public void onFrontExteriorLightClicked(int segment) {
        FrontExteriorLightState state;
        if (segment == 0) state = FrontExteriorLightState.INACTIVE;
        else if (segment == 1) state = FrontExteriorLightState.ACTIVE;
        else state = FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM;

        if (state == vehicle.frontExteriorLightState) return;

        view.showLoadingView(true);
        sentCommand = ControlLights.TYPE;

        Command command = new ControlLights(state,
                vehicle.isRearExteriorLightActive,
                vehicle.isInteriorLightActive,
                vehicle.lightsAmbientColor);

        sendCommand(command);
    }

    public void onRevokeClicked() {

    }

    public void onRefreshClicked() {
        view.showLoadingView(true);
        sentCommand = com.highmobility.autoapi.VehicleStatus.TYPE;
        sendCommand(new GetVehicleStatus());
    }

    public void readyToSendCommands() {
        initializing = true;
        view.showLoadingView(true);
        // capabilities are required to know if action commands are available.
        sentCommand = GetCapabilities.TYPE;
        sendCommand(new GetCapabilities());
    }

    public void willDestroy() {

    }

    public void onDestroy() {
        cancelInitTimer();
    }

    void sendCommand(Bytes command) {

    }

    void onBleAckReceived() {
        if (initializing == true) {
            rescheduleInitTimer();
        }
    }

    void onCommandReceived(Bytes bytes) {
        Command command = CommandResolver.resolve(bytes);
        vehicle.update(command);

        if (command instanceof Capabilities) {
            rescheduleInitTimer();
            view.onCapabilitiesUpdate(vehicle);
            continueInitAfterGetCapabilities();
        } else if (command instanceof Failure) {
            Failure failure = (Failure) command;
            Log.d(SandboxUi.TAG, "failure " + failure.getFailureReason());

            if (sentCommand != null) {
                if (initializing) {
                    // initialization failed
                    initializing = false;
                    cancelInitTimer();
                    view.onError(true, "Cannot get vehicle data: " + failure
                            .getFailureReason());
                } else {
                    view.showLoadingView(false);
                    view.onError(false, failure.getFailedType() + " failed: "
                            + failure.getFailureReason());
                    sentCommand = null;
                }
            }
        } else {
            if (initializing && command instanceof com.highmobility.autoapi.VehicleStatus) {
                cancelInitTimer();
                initializing = false;
            }

            sentCommand = null;
            view.onVehicleStatusUpdate(vehicle);
            view.showLoadingView(false);
        }
    }

    void continueInitAfterGetCapabilities() {
        rescheduleInitTimer();
        sentCommand = GetVehicleStatus.TYPE;
        sendCommand(new GetVehicleStatus());
    }

    void cancelInitTimer() {
        if (initTimer != null) {
            Log.d(SandboxUi.TAG, "cancelInitTimer:");
            repeatTask.cancel();
            initTimer.cancel();
            initTimer = null;
        }
    }

    TimerTask repeatTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Log.d(SandboxUi.TAG, "run: app layer command wait timeout");
                // TODO: if a command is still waiting for a response, app will go to invalid state
                failedToSendInitCommand("command timed out.");
            }
        };
    }

    void failedToSendInitCommand(final String message) {
        view.getActivity().runOnUiThread(() -> {
            retryCount++;
            if (retryCount == 3) {
                initializing = false;
                view.onError(true, message);
                retryCount = 0;
                return;
            }

            Log.d(SandboxUi.TAG, "init: try to send the command again " + (sentCommand !=
                    null ?
                    sentCommand : "null command"));
            if (sentCommand != null) {
                // try to send command again
                if (sentCommand == GetVehicleStatus.TYPE) {
                    Log.d(SandboxUi.TAG, "send vs");
                    sendCommand(new GetVehicleStatus());
                } else if (sentCommand == GetCapabilities.TYPE) {
                    Log.d(SandboxUi.TAG, "send capa");
                    sendCommand(new GetCapabilities());
                }

                if (initTimer != null) rescheduleInitTimer(); // no timer for telematics
            }
        });
    }

    void rescheduleInitTimer() {
        cancelInitTimer();
        Log.d(SandboxUi.TAG, "rescheduleInitTimer: ");
        initTimer = new Timer();
        if (repeatTask != null) repeatTask.cancel();
        repeatTask = repeatTask();
        initTimer.schedule(repeatTask, (long) 120 * 1000);
    }

    void onCommandError(int errorCode, String message) {
        if (initializing == true &&
                (sentCommand == GetVehicleStatus.TYPE
                        || sentCommand == GetCapabilities.TYPE)) {
            Log.d(SandboxUi.TAG, "initialize, onCommandError: " + errorCode + " " +
                    message);
            failedToSendInitCommand(message);
        } else if (sentCommand != null) {
            view.showLoadingView(false);
            view.onError(false, message);
            sentCommand = null;
        }
    }
}
