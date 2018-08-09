package com.highmobility.sandboxui.controller;

import android.util.Log;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Type;
import com.highmobility.hmkit.Telematics;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.queue.CommandFailure;
import com.highmobility.queue.ICommandQueue;
import com.highmobility.queue.TelematicsCommandQueue;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleTelematicsController extends ConnectedVehicleController {
    ConnectedVehicleTelematicsController(IConnectedVehicleView view) {
        super(false, view);
        queue = new TelematicsCommandQueue(iQueue);
    }

    ICommandQueue iQueue = new ICommandQueue() {
        @Override public void onCommandReceived(Command sentCommand, Bytes command) {
            ConnectedVehicleTelematicsController.super.onCommandReceived(command);
        }

        @Override public void onCommandFailed(CommandFailure reason) {

        }

        @Override public void sendCommand(Command command) {

        }
    };

    @Override public void init() {
        super.init();
        readyToSendCommands();
    }

    @Override void queueCommand(Command command, Type response) {
        Log.d(SandboxUi.TAG, "queueCommand: " + certificate.toString());
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
