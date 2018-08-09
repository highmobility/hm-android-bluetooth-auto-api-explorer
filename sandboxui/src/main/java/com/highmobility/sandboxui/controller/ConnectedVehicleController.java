package com.highmobility.sandboxui.controller;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.OpenCloseTrunk;
import com.highmobility.autoapi.RooftopState;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;
import com.highmobility.autoapi.property.doors.DoorLock;
import com.highmobility.crypto.AccessCertificate;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Manager;
import com.highmobility.queue.CommandQueue;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.model.VehicleStatus;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;

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
    DeviceSerial vehicleSerial;

    CommandQueue queue;

    Manager manager;
    IConnectedVehicleView view;

    boolean initialising;

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
        DoorLock lockState = vehicle.doorsLocked == true ? UNLOCKED : LOCKED;
        queueCommand(new LockUnlockDoors(lockState), LockState.TYPE);
    }

    public void onLockTrunkClicked() {
        view.showLoadingView(true);
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
        queueCommand(command, TrunkState.TYPE);
    }

    public void onWindshieldDefrostingClicked() {
        view.showLoadingView(true);
        boolean startDefrosting = vehicle.isWindshieldDefrostingActive ? false : true;
        Command command = new StartStopDefrosting(startDefrosting);
        queueCommand(command, ClimateState.TYPE);
    }

    public void onSunroofVisibilityClicked() {
        view.showLoadingView(true);

        float dimPercentage = vehicle.rooftopDimmingPercentage == 1f ? 0f : 1f;

        Command command = new ControlRooftop(dimPercentage, vehicle
                .rooftopOpenPercentage);
        queueCommand(command, RooftopState.TYPE);
    }

    public void onSunroofOpenClicked() {
        view.showLoadingView(true);
        float openPercentage = vehicle.rooftopOpenPercentage == 0f ? 1f : 0f;
        Command command = new ControlRooftop(vehicle
                .rooftopDimmingPercentage, openPercentage);
        queueCommand(command, RooftopState.TYPE); // TODO: 07/08/2018 test that rooftopstate comes
        // back
    }

    public void onFrontExteriorLightClicked(int segment) {
        FrontExteriorLightState state;
        if (segment == 0) state = FrontExteriorLightState.INACTIVE;
        else if (segment == 1) state = FrontExteriorLightState.ACTIVE;
        else state = FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM;

        if (state == vehicle.frontExteriorLightState) return;

        view.showLoadingView(true);

        Command command = new ControlLights(state,
                vehicle.isRearExteriorLightActive,
                vehicle.isInteriorLightActive,
                vehicle.lightsAmbientColor);

        queueCommand(command, LightsState.TYPE);
    }

    public void onRevokeClicked() {
        // ble method
    }

    public void onRefreshClicked() {
        view.showLoadingView(true);
        queueCommand(new GetVehicleStatus(), com.highmobility.autoapi.VehicleStatus.TYPE);
    }

    public void readyToSendCommands() {
        initialising = true;
        view.showLoadingView(true);
        // capabilities are required to know if action commands are available.
        queueCommand(new GetCapabilities(), Capabilities.TYPE);
        queueCommand(new GetVehicleStatus(), com.highmobility.autoapi.VehicleStatus.TYPE);
    }

    public void willDestroy() {

    }

    public void onDestroy() {
        queue.purge();
    }

    void queueCommand(Command command, Type response) {

    }

    void onCommandReceived(Bytes bytes, Command sentCommand) {
        Command command = CommandResolver.resolve(bytes);
        vehicle.update(command);

        if (command instanceof Capabilities) {
            view.onCapabilitiesUpdate(vehicle);
        } else if (command instanceof Failure) {

            if (sentCommand != null) {
                Failure failure = (Failure) command;
                onCommandFailed(sentCommand, failure);
            }
        } else {
            if (initialising && command instanceof com.highmobility.autoapi.VehicleStatus) {
                initialising = false; // got vs, initialize is finished
            }

            view.onVehicleStatusUpdate(vehicle);
            view.showLoadingView(false);
        }
    }

    // timeout or other reason
    void onCommandFailed(Command sentCommand, @Nullable Failure failure) {
        String reason;
        if (failure != null) {
            reason = "Cannot get vehicle data for " + failure.getFailedType() + ": " + failure
                    .getFailureReason();
        } else {
            reason = "Failed to send " + sentCommand.getType();
        }

        Log.e(SandboxUi.TAG, "onCommandFailed: " + reason);

        if (initialising) {
            // initialization failed
            initialising = false;
            view.onError(true, reason);
        } else {
            view.showLoadingView(false);
            view.onError(false, reason);
        }
    }
}
