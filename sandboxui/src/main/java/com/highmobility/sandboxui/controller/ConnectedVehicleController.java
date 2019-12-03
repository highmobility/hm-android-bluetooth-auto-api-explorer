package com.highmobility.sandboxui.controller;

import android.content.Intent;

import com.highmobility.autoapi.CapabilitiesState;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.ControlTrunk;
import com.highmobility.autoapi.DoorsState;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.RooftopControlState;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.VehicleStatusState;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.ActiveState;
import com.highmobility.autoapi.value.Light;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.autoapi.value.Position;
import com.highmobility.autoapi.value.ReadingLamp;
import com.highmobility.crypto.AccessCertificate;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.HMKit;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;
import com.highmobility.queue.CommandFailure;
import com.highmobility.sandboxui.model.VehicleStatus;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;

import static com.highmobility.autoapi.value.LockState.LOCKED;
import static com.highmobility.autoapi.value.LockState.UNLOCKED;
import static timber.log.Timber.d;
import static timber.log.Timber.e;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleController {
    public static final String EXTRA_SERIAL = "EXTRA_SERIAL";
    public static final String EXTRA_USE_BLE = "EXTRA_USE_BLE";
    public static final String EXTRA_SERVICE_NAME = "EXTRA_SERVICE_NAME";
    public static final String EXTRA_ALIVE_PING_AMOUNT_NAME = "EXTRA_ALIVE";
    // expects hmkit init values + accessToken separated by : (4 values)
    public static final String EXTRA_INIT_INFO = "EXTRA_INIT_INFO";

    public boolean useBle;
    private String serviceName;
    private String[] initInfo;

    public static VehicleStatus vehicle;
    public AccessCertificate certificate;
    public DeviceSerial vehicleSerial;

    HMKit hmKit;
    IConnectedVehicleView view;

    boolean initialising;

    public String getVehicleName() {
        return vehicle.name;
    }

    public void onLockDoorsClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        LockState lockState = vehicle.doorsLocked == true ? UNLOCKED : LOCKED;
        queueCommand(new LockUnlockDoors(lockState), DoorsState.class);
    }

    public void onLockTrunkClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        LockState newLockState;
        Position newPosition;

        if (vehicle.trunkLockState == LOCKED) {
            newLockState = UNLOCKED;
            newPosition = Position.OPEN;
        } else {
            newLockState = LOCKED;
            newPosition = Position.CLOSED;
        }

        Command command = new ControlTrunk(newLockState, newPosition);
        queueCommand(command, TrunkState.class);
    }

    public void onWindshieldDefrostingClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        ActiveState startDefrosting = vehicle.isWindshieldDefrostingActive ?
                ActiveState.INACTIVE : ActiveState.ACTIVE;
        Command command = new StartStopDefrosting(startDefrosting);
        queueCommand(command, ClimateState.class);
    }

    public void onSunroofVisibilityClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        double dimPercentage = vehicle.rooftopDimmingPercentage == 1d ? 0d : 1d;

        Command command = new ControlRooftop(dimPercentage, vehicle.rooftopOpenPercentage, null,
                null, null);
        queueCommand(command, RooftopControlState.class);
    }

    public void onSunroofOpenClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        double openPercentage = vehicle.rooftopOpenPercentage == 0d ? 1d : 0d;
        Command command = new ControlRooftop(vehicle.rooftopDimmingPercentage, openPercentage,
                null, null, null);
        queueCommand(command, RooftopControlState.class);
    }

    public void onFrontExteriorLightClicked(int segment) {
        LightsState.FrontExteriorLight state;
        if (segment == 0) state = LightsState.FrontExteriorLight.INACTIVE;
        else if (segment == 1) state = LightsState.FrontExteriorLight.ACTIVE;
        else state = LightsState.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM;

        if (state == vehicle.lightsState.getFrontExteriorLight().getValue()) return;

        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        Command command = new ControlLights(state,
                vehicle.lightsState.getRearExteriorLight().getValue(),
                vehicle.lightsState.getAmbientLightColour().getValue(),
                Property.propertiesToValues(vehicle.lightsState.getFogLights(), Light.class),
                Property.propertiesToValues(vehicle.lightsState.getReadingLamps(),
                        ReadingLamp.class),
                Property.propertiesToValues(vehicle.lightsState.getInteriorLights(), Light.class));

        queueCommand(command, LightsState.class);
    }

    public void onRevokeClicked() {
        // ble method
    }

    public void onRefreshClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        queueCommand(new GetVehicleStatus(), VehicleStatusState.class);
    }

    public static ConnectedVehicleController create(IConnectedVehicleView view,
                                                    IConnectedVehicleBleView bleView,
                                                    Intent intent) {
        String vehicleSerialBytes = intent.getStringExtra(EXTRA_SERIAL);
        boolean useBle = intent.getBooleanExtra(EXTRA_USE_BLE, true);
        String serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
        String initInfo = intent.getStringExtra(EXTRA_INIT_INFO);

        ConnectedVehicleController controller;

        DeviceSerial vehicleSerial = null;

        if (useBle) {
            int alivePingInterval = intent.getIntExtra(EXTRA_ALIVE_PING_AMOUNT_NAME, -1);
            controller = new ConnectedVehicleBleController(view, bleView, alivePingInterval);
        } else {
            controller = new ConnectedVehicleTelematicsController(view);
        }

        if (vehicleSerialBytes != null) vehicleSerial = new DeviceSerial(vehicleSerialBytes);

        if (initInfo != null) {
            // we are expected to be initialised in this class
            controller.initInfo = initInfo.split(":");
        }

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

    public void onViewInitialised() {
        downloadCertOrUseFromStorage();
    }

    protected void onCertificateDownloaded() {

    }

    protected void downloadCertOrUseFromStorage() {
        // initInfo is used in instrumented tests
        if (initInfo != null) {
            view.setViewState(IConnectedVehicleView.ViewState.DOWNLOADING_CERT);

            if (initInfo.length != 4) throw new IllegalArgumentException("invalid init info");
            try {
                HMKit.getInstance().initialise(view.getActivity());
            } catch (Exception e) {
            }

            hmKit.setDeviceCertificate(initInfo[0], initInfo[1], initInfo[2]);

            if (vehicleSerial != null) certificate = hmKit.getCertificate(vehicleSerial);
            if (certificate == null) {
                hmKit.downloadAccessCertificate(initInfo[3], new HMKit.DownloadCallback() {
                    @Override public void onDownloaded(DeviceSerial serial) {
                        vehicleSerial = serial;
                        certificate = hmKit.getCertificate(serial);
                        onCertificateDownloaded();
                    }

                    @Override
                    public void onDownloadFailed(DownloadAccessCertificateError error) {
                        view.onError(true, "failed to download cert");
                        view.setViewState(IConnectedVehicleView.ViewState.FAILED_TO_DOWNLOAD_CERT);
                    }
                });
            } else {
                onCertificateDownloaded();
            }
        } else {
            // we are expected to be initialised before
            if (vehicleSerial != null) {
                certificate = HMKit.getInstance().getCertificate(vehicleSerial);
            } else {
                AccessCertificate[] certificates = HMKit.getInstance().getCertificates();
                if (certificates == null || certificates.length < 1)
                    view.onError(true, "No certificates in HMKit");
                certificate = HMKit.getInstance().getCertificates()[0];
                vehicleSerial = certificate.getGainerSerial();
            }

            if (certificate != null) onCertificateDownloaded();
            else view.onError(true, "No certificate to send commands");
        }
    }

    protected void readyToSendCommands() {
        initialising = true;
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        sendInitCommands();
    }

    private void sendInitCommands() {
        // capabilities are required to know if action commands are available.
        queueCommand(new GetCapabilities(), CapabilitiesState.class);
        queueCommand(new GetVehicleStatus(), VehicleStatusState.class);
    }

    public Intent willDestroy() {
        return new Intent().putExtra(ConnectedVehicleActivity.EXTRA_VEHICLE_SERIAL,
                vehicleSerial.getByteArray());
    }

    public void onDestroy() {

    }

    <T extends Command> void queueCommand(Command command, Class<T> response) {
        // telematics and ble have different queues and handle that in their classes
    }

    void onCommandReceived(Command command, Command sentCommand) {
        vehicle.update(command);

        if (command instanceof CapabilitiesState) {
            view.onCapabilitiesUpdate(vehicle);
        } else {
            if (initialising && command instanceof VehicleStatusState) {
                initialising = false; // got vs, initialize is finished
            }

            view.onVehicleStatusUpdate(vehicle);
            view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED);
        }
    }

    // timeout or other reason
    void onCommandFailed(Command sentCommand, CommandFailure failure) {
        String reason = String.format("Command failed: %s", failure.getErrorMessage());
        e("onCommandFailed: %s", reason);

        if (initialising) {
            // initialization failed
            initialising = false;
            view.onError(true, reason);
            view.getActivity().finish();
        } else {
            view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED);
            view.onError(false, reason);
        }
    }
}
