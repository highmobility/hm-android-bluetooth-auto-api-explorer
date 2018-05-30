package com.highmobility.sandboxui.controller;

import android.util.Log;
import com.highmobility.hmkit.Error.TelematicsError;
import com.highmobility.hmkit.Telematics;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleTelematicsController extends ConnectedVehicleController {
    ConnectedVehicleTelematicsController(IConnectedVehicleView view) {
        super(false, view);
    }

    @Override public void init() {
        super.init();
        readyToSendCommands();
    }

    @Override
    void sendCommand(Bytes command) {
        Log.d(SandboxUi.TAG, "sendCommand: " + certificate.toString());
        manager.getTelematics().sendCommand(command, certificate.getGainerSerial(), new
                Telematics.CommandCallback() {
                    @Override
                    public void onCommandResponse(Bytes bytes) {
                        onCommandReceived(bytes);
                    }

                    @Override
                    public void onCommandFailed(TelematicsError telematicsError) {
                        Log.e(SandboxUi.TAG, "send telematics command error: " + telematicsError
                                .getMessage());
                        onCommandError(telematicsError.getCode(), telematicsError.getMessage());
                    }
                });
    }
}
