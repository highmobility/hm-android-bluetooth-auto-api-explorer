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
package com.highmobility.sandboxui.controller;

import android.content.Intent;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Climate;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Doors;
import com.highmobility.autoapi.Lights;
import com.highmobility.autoapi.RooftopControl;
import com.highmobility.autoapi.Trunk;
import com.highmobility.autoapi.VehicleInformation;
import com.highmobility.autoapi.VehicleStatus;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.ActiveState;
import com.highmobility.autoapi.value.Light;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.autoapi.value.Position;
import com.highmobility.autoapi.value.ReadingLamp;
import com.highmobility.commandqueue.QueueItemFailure;
import com.highmobility.crypto.AccessCertificate;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.HMKit;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;

import com.highmobility.sandboxui.model.VehicleState;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;

import static com.highmobility.autoapi.value.LockState.LOCKED;
import static com.highmobility.autoapi.value.LockState.UNLOCKED;
import static com.highmobility.sandboxui.model.VehicleState.propertiesToValues;
import static timber.log.Timber.d;
import static timber.log.Timber.e;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleController {
    public static final String EXTRA_SERIAL = "EXTRA_SERIAL";
    public static final String EXTRA_USE_BLE = "EXTRA_USE_BLE";
    public static final String EXTRA_ALIVE_PING_AMOUNT_NAME = "EXTRA_ALIVE";
    // expects hmkit init values + accessToken separated by : (4 values)
    public static final String EXTRA_INIT_INFO = "EXTRA_INIT_INFO";

    public boolean useBle;
    private String[] initInfo;

    public static VehicleState vehicle;
    public AccessCertificate certificate;
    public DeviceSerial vehicleSerial;

    HMKit hmKit;
    IConnectedVehicleView view;

    boolean initialising;

    public String getVehicleName() {
        return vehicle.getName();
    }

    public void onLockDoorsClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        LockState lockState = vehicle.getDoorsLocked() == true ? UNLOCKED : LOCKED;
        queueCommand(new Doors.LockUnlockDoors(lockState), Doors.State.class);
    }

    public void onLockTrunkClicked() {
        d("controller trunk click(): thread:%d object:%s", Thread.currentThread().getId(), this);
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        LockState newLockState;
        Position newPosition;

        if (vehicle.getTrunkLockState() == LOCKED) {
            newLockState = UNLOCKED;
            newPosition = Position.OPEN;
        } else {
            newLockState = LOCKED;
            newPosition = Position.CLOSED;
        }

        Command command = new Trunk.ControlTrunk(newLockState, newPosition);
        queueCommand(command, Trunk.State.class);
    }

    public void onWindshieldDefrostingClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        ActiveState startDefrosting = vehicle.isWindshieldDefrostingActive() ?
                ActiveState.INACTIVE : ActiveState.ACTIVE;
        Command command = new Climate.StartStopDefrosting(startDefrosting);
        queueCommand(command, Climate.State.class);
    }

    public void onSunroofVisibilityClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        double dimPercentage = vehicle.getRooftopDimmingPercentage() == 1d ? 0d : 1d;

        Command command = new RooftopControl.ControlRooftop(dimPercentage, vehicle.getRooftopOpenPercentage(), null,
                null, null);
        queueCommand(command, RooftopControl.State.class);
    }

    public void onSunroofOpenClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        double openPercentage = vehicle.getRooftopOpenPercentage() == 0d ? 1d : 0d;
        Command command = new RooftopControl.ControlRooftop(vehicle.getRooftopDimmingPercentage(), openPercentage,
                null, null, null);
        queueCommand(command, RooftopControl.State.class);
    }

    public void onFrontExteriorLightClicked(int segment) {
        Lights.FrontExteriorLight state;
        if (segment == 0) state = Lights.FrontExteriorLight.INACTIVE;
        else if (segment == 1) state = Lights.FrontExteriorLight.ACTIVE;
        else state = Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM;

        if (state == vehicle.getLightsState().getFrontExteriorLight().getValue()) return;

        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);

        Command command = new Lights.ControlLights(state,
                vehicle.getLightsState().getRearExteriorLight().getValue(),
                vehicle.getLightsState().getAmbientLightColour().getValue(),
                propertiesToValues(vehicle.getLightsState().getFogLights()),
                propertiesToValues(vehicle.getLightsState().getReadingLamps()),
                propertiesToValues(vehicle.getLightsState().getInteriorLights()));

        queueCommand(command, Lights.State.class);
    }

    public void onRevokeClicked() {
        // ble method
    }

    public void onRefreshClicked() {
        view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
        queueCommand(new VehicleStatus.GetVehicleStatus(), VehicleStatus.State.class);
    }

    public static ConnectedVehicleController create(IConnectedVehicleView view,
                                                    IConnectedVehicleBleView bleView,
                                                    Intent intent) {
        String vehicleSerialBytes = intent.getStringExtra(EXTRA_SERIAL);
        boolean useBle = intent.getBooleanExtra(EXTRA_USE_BLE, true);

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

        return controller;
    }

    ConnectedVehicleController(boolean useBle, IConnectedVehicleView view) {
        hmKit = hmKit.getInstance();
        this.view = view;
        this.useBle = useBle;
        vehicle = new VehicleState();
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
        queueCommand(new Capabilities.GetCapabilities(), Capabilities.State.class);
        queueCommand(new VehicleStatus.GetVehicleStatus(), VehicleStatus.State.class);
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

        if (command instanceof Capabilities.State) {
            view.onCapabilitiesUpdate(vehicle);
        } else {
            if (initialising && command instanceof VehicleStatus.State) {
                initialising = false; // got vs, initialize is finished
            }

            view.onVehicleStatusUpdate(vehicle);
            view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED);
        }
    }

    // timeout or other reason
    void onCommandFailed(QueueItemFailure failure) {
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
