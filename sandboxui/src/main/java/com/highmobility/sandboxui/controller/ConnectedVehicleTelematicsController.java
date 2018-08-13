package com.highmobility.sandboxui.controller;

import android.support.annotation.Nullable;
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
    TelematicsCommandQueue queue;

    ConnectedVehicleTelematicsController(IConnectedVehicleView view) {
        super(false, view);
        queue = new TelematicsCommandQueue(iQueue);
    }

    ICommandQueue iQueue = new ICommandQueue() {
        @Override public void onCommandReceived(Bytes command, @Nullable Command sentCommand) {

        }

        @Override public void onCommandFailed(CommandFailure reason, Command sentCommand) {

        }

        @Override public void sendCommand(Command command) {
            Log.d(SandboxUi.TAG, "queueCommand: " + certificate.toString());
            manager.getTelematics().sendCommand(command, certificate.getGainerSerial(), new
                    Telematics.CommandCallback() {
                        @Override
                        public void onCommandResponse(Bytes bytes) {
                            ConnectedVehicleTelematicsController.this.onCommandReceived(bytes,
                                    command);
                        }

                        @Override public void onCommandFailed(TelematicsError telematicsError) {
                            Log.e(SandboxUi.TAG, "send telematics command error: " + telematicsError
                                    .getMessage());
                            queue.onCommandFailedToSend(command, telematicsError);
                        }
                    });
        }
    };

    @Override public void init() {
        super.init();
        readyToSendCommands();
    }

    @Override void queueCommand(Command command, Type response) {
        // telematics queue does not need response type
        queue.queue(command);
    }
}
