package com.highmobility.sandboxui.controller;

import android.content.Intent;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.ControlTrunk;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.RooftopState;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.Lock;
import com.highmobility.autoapi.value.Position;
import com.highmobility.autoapi.value.lights.FogLight;
import com.highmobility.autoapi.value.lights.FrontExteriorLightState;
import com.highmobility.autoapi.value.lights.InteriorLamp;
import com.highmobility.autoapi.value.lights.ReadingLamp;
import com.highmobility.crypto.AccessCertificate;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.HMKit;
import com.highmobility.queue.CommandFailure;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.model.VehicleStatus;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;

import static com.highmobility.autoapi.value.Lock.LOCKED;
import static com.highmobility.autoapi.value.Lock.UNLOCKED;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleController {
    public static final String EXTRA_SERIAL = "EXTRA_SERIAL";
    public static final String EXTRA_USE_BLE = "EXTRA_USE_BLE";
    public static final String EXTRA_SERVICE_NAME = "EXTRA_SERVICE_NAME";
    public static final String EXTRA_ALIVE_PING_AMOUNT_NAME = "EXTRA_ALIVE";

    public boolean useBle;
    private String serviceName;

    public static VehicleStatus vehicle;
    public AccessCertificate certificate;
    public DeviceSerial vehicleSerial;

    HMKit hmKit;
    IConnectedVehicleView view;

    boolean initialising;

    public String getVehicleName() {
        return vehicle.name;
    }

    public static ConnectedVehicleController create(IConnectedVehicleView view,
                                                    IConnectedVehicleBleView bleView,
                                                    Intent intent) {
        byte[] vehicleSerialBytes = intent.getByteArrayExtra(EXTRA_SERIAL);
        DeviceSerial vehicleSerial;
        boolean useBle = intent.getBooleanExtra(EXTRA_USE_BLE, true);
        String serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);

        AccessCertificate cert;
        if (vehicleSerialBytes != null) {
            vehicleSerial = new DeviceSerial(vehicleSerialBytes);
            cert = HMKit.getInstance().getCertificate(vehicleSerial);
        } else {
            AccessCertificate[] certificates = HMKit.getInstance().getCertificates();
            if (certificates == null || certificates.length < 1)
                throw new IllegalStateException("HMKit not initialised");
            cert = HMKit.getInstance().getCertificates()[0];
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
        hmKit = hmKit.getInstance();
        this.view = view;
        this.useBle = useBle;
        vehicle = new VehicleStatus();
    }

    public void init() {
    }

    public void onLockDoorsClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        Lock lockState = vehicle.doorsLocked == true ? UNLOCKED : LOCKED;
        queueCommand(new LockUnlockDoors(lockState), LockState.TYPE);
    }

    public void onLockTrunkClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        Lock newLockState;
        Position newPosition;

        if (vehicle.trunkLockState == LOCKED) {
            newLockState = UNLOCKED;
            newPosition = Position.OPEN;
        } else {
            newLockState = LOCKED;
            newPosition = Position.CLOSED;
        }

        Command command = new ControlTrunk(newLockState, newPosition);
        queueCommand(command, TrunkState.TYPE);
    }

    public void onWindshieldDefrostingClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        boolean startDefrosting = vehicle.isWindshieldDefrostingActive ? false : true;
        Command command = new StartStopDefrosting(startDefrosting);
        queueCommand(command, ClimateState.TYPE);
    }

    public void onSunroofVisibilityClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        double dimPercentage = vehicle.rooftopDimmingPercentage == 1d ? 0d : 1d;

        Command command = new ControlRooftop(dimPercentage, vehicle.rooftopOpenPercentage, null,
                null, null);
        queueCommand(command, RooftopState.TYPE);
    }

    public void onSunroofOpenClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        double openPercentage = vehicle.rooftopOpenPercentage == 0d ? 1d : 0d;
        Command command = new ControlRooftop(vehicle.rooftopDimmingPercentage, openPercentage,
                null, null, null);
        queueCommand(command, RooftopState.TYPE);
    }

    public void onFrontExteriorLightClicked(int segment) {
        FrontExteriorLightState state;
        if (segment == 0) state = FrontExteriorLightState.INACTIVE;
        else if (segment == 1) state = FrontExteriorLightState.ACTIVE;
        else state = FrontExteriorLightState.ACTIVE_FULL_BEAM;

        if (state == vehicle.lightsState.getFrontExteriorLightState().getValue()) return;

        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        Command command = new ControlLights(state,
                vehicle.lightsState.isRearExteriorLightActive().getValue(),
                vehicle.lightsState.getAmbientColor().getValue(),
                Property.propertiesToValues(vehicle.lightsState.getFogLights(), FogLight.class),
                Property.propertiesToValues(vehicle.lightsState.getReadingLamps(),
                        ReadingLamp.class),
                Property.propertiesToValues(vehicle.lightsState.getInteriorLamps(),
                        InteriorLamp.class));

        queueCommand(command, LightsState.TYPE);
    }

    public void onRevokeClicked() {
        // ble method
    }

    public void onRefreshClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        queueCommand(new GetVehicleStatus(), com.highmobility.autoapi.VehicleStatus.TYPE);
    }

    public void readyToSendCommands() {
        initialising = true;
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        // capabilities are required to know if action commands are available.
        queueCommand(new GetCapabilities(), Capabilities.TYPE);
        queueCommand(new GetVehicleStatus(), com.highmobility.autoapi.VehicleStatus.TYPE);
    }

    public Intent willDestroy() {
        return new Intent().putExtra(ConnectedVehicleActivity.EXTRA_VEHICLE_SERIAL, vehicleSerial
                .getByteArray());
    }

    public void onDestroy() {

    }

    void queueCommand(Command command, Type response) {
        // telematics and ble have different queues and handle that in their classes
    }

    void onCommandReceived(Bytes bytes, Command sentCommand) {
        Command command = CommandResolver.resolve(bytes);
        vehicle.update(command);

        if (command instanceof Capabilities) {
            view.onCapabilitiesUpdate(vehicle);
        } else {
            if (initialising && command instanceof com.highmobility.autoapi.VehicleStatus) {
                initialising = false; // got vs, initialize is finished
            }

            view.onVehicleStatusUpdate(vehicle);
            view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED);
        }
    }

    // timeout or other reason
    void onCommandFailed(Command sentCommand, CommandFailure failure) {
        String reason = String.format("Command failed: %s", failure.getErrorMessage());

        Log.e(SandboxUi.TAG, "onCommandFailed: " + reason);

        if (initialising) {
            // initialization failed
            initialising = false;
            view.onError(true, reason);
        } else {
            view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED);
            view.onError(false, reason);
        }
    }
}
