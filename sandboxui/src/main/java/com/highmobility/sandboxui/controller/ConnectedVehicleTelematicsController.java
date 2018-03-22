package com.highmobility.sandboxui.controller;

import android.util.Log;

import com.highmobility.hmkit.Error.TelematicsError;
import com.highmobility.hmkit.Telematics;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.view.IConnectedVehicleView;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleTelematicsController extends ConnectedVehicleController {
    ConnectedVehicleTelematicsController(IConnectedVehicleView view) {
        super(false, view);
    }

    @Override
    void sendCommand(byte[] command) {
        Log.d(SandboxUi.TAG, "sendCommand: " + certificate.toString());
        manager.getTelematics().sendCommand(command, certificate.getGainerSerial(), new Telematics.CommandCallback() {
            @Override
            public void onCommandResponse(byte[] bytes) {
                onCommandReceived(bytes);
            }

            @Override
            public void onCommandFailed(TelematicsError telematicsError) {
                Log.e(SandboxUi.TAG, "send telematics command error: " + telematicsError.getMessage());
                onCommandError(telematicsError.getCode(), telematicsError.getMessage());
            }
        });
    }
}
