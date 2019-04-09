package com.highmobility.sandboxui.controller;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Type;
import com.highmobility.hmkit.Telematics;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.queue.CommandFailure;
import com.highmobility.queue.ICommandQueue;
import com.highmobility.queue.TelematicsCommandQueue;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;

import androidx.annotation.Nullable;

import static timber.log.Timber.e;

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
            ConnectedVehicleTelematicsController.this.onCommandReceived(command, sentCommand);
        }

        @Override public void onCommandFailed(CommandFailure reason, Command sentCommand) {
            ConnectedVehicleTelematicsController.this.onCommandFailed(sentCommand, reason);
        }

        @Override public void sendCommand(Command command) {
            hmKit.getTelematics().sendCommand(command, certificate.getGainerSerial(), new
                    Telematics.CommandCallback() {
                        @Override
                        public void onCommandResponse(Bytes bytes) {
                            queue.onCommandReceived(bytes);
                        }

                        @Override public void onCommandFailed(TelematicsError telematicsError) {
                            e("send telematics command error: %s", telematicsError.getMessage());
                            queue.onCommandFailedToSend(command, telematicsError);
                        }
                    });
        }
    };

    @Override public void init() {
        super.init();
        readyToSendCommands();
    }

    @Override public void onDestroy() {
        queue.purge();
    }

    @Override void queueCommand(Command command, Type response) {
        // telematics queue does not need response type
        queue.queue(command);
    }
}
