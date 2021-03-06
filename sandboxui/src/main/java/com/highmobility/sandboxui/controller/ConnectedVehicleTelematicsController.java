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

import com.highmobility.autoapi.Command;
import com.highmobility.commandqueue.QueueItemFailure;
import com.highmobility.commandqueue.ICommandQueue;
import com.highmobility.commandqueue.QueueItem;
import com.highmobility.commandqueue.TelematicsCommandQueue;
import com.highmobility.commandqueue.TelematicsQueueConfiguration;
import com.highmobility.hmkit.Telematics;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.value.Bytes;

import javax.annotation.Nullable;

public class ConnectedVehicleTelematicsController extends ConnectedVehicleController {
    TelematicsCommandQueue queue;

    ConnectedVehicleTelematicsController(IConnectedVehicleView view) {
        super(false, view);
        queue = new TelematicsCommandQueue(iQueue, new TelematicsQueueConfiguration(1, 800));
    }

    ICommandQueue iQueue = new ICommandQueue() {
        @Override public void onCommandReceived(Command command, @Nullable QueueItem queueItem) {
            ConnectedVehicleTelematicsController.this.onCommandReceived(command,
                    queueItem.getCommandSent());
        }

        @Override public void onCommandFailed(QueueItemFailure queueItemFailure) {
            ConnectedVehicleTelematicsController.this.onCommandFailed(queueItemFailure);
        }

        @Override public void sendCommand(Command command) {
            hmKit.getTelematics().sendCommand(command, certificate.getGainerSerial(), new
                    Telematics.CommandCallback() {
                        @Override
                        public void onCommandResponse(Bytes bytes) {
                            queue.onCommandReceived(bytes);
                        }

                        @Override public void onCommandFailed(TelematicsError telematicsError) {
                            queue.onCommandFailedToSend(command, telematicsError);
                        }
                    });
        }
    };

    @Override protected void onCertificateDownloaded() {
        super.onCertificateDownloaded();
        readyToSendCommands();
    }

    @Override public void onDestroy() {
        queue.purge();
    }

    @Override <T extends Command> void queueCommand(Command command, Class<T> response) {
        // telematics queue does not need response type
        queue.queue(command);
    }
}
