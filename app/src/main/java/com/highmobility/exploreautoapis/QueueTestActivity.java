package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.SetNaviDestination;
import com.highmobility.autoapi.VehicleStatus;
import com.highmobility.autoapi.property.CoordinatesProperty;
import com.highmobility.autoapi.property.doors.DoorLock;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;
import com.highmobility.hmkit.Telematics;
import com.highmobility.hmkit.error.BroadcastError;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.queue.BleCommandQueue;
import com.highmobility.queue.CommandFailure;
import com.highmobility.queue.CommandQueue;
import com.highmobility.queue.IBleCommandQueue;
import com.highmobility.queue.ICommandQueue;
import com.highmobility.value.Bytes;

// TODO: 13/08/2018 delete class
public class QueueTestActivity extends Activity {
    private static final String TAG = "Scaffold";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Manager.getInstance().initialize(
                "dGVzdOlUDp/iZdmzY3AthPYPev5SFk72I" +
                        "+/pKDMlvcYS9ksR6nS8xf3WoGmARSzAZsGsIkdvs56zzvGbwsmg" +
                        "+IV6Qkgh6U2WGXe4mGpoib8WcW/de2lPZ94EMazB0wppKQCNu7Q1yHPuTlPx6EwaT6ntlCz2oPmtspy9mO+U6hzg3eSzjptslG+MzfMTUcbImFsokoZpl39s",
                "HRgynolx5zIfK+r9Xd/Js6DNdnvHtE/kc6j6T9V+zbQ=",
                "HJS8Wh+Gjh2JRB8pMOmQdTMfVR7JoPLVF1U85xjSg7puYoTwLf+DO9Zs67jw" +
                        "+6pXmtkYxynMQm0rfcBU0XFF5A==",
                getApplicationContext()
        );

        String accessToken = "O6kzNEYre8PNzjY8WaPRN9dTVXpOQr5Wbzy09UuimtS1-lEKpSKY5jSMkoPnZ" +
                "-gauSNZp2IJp8mlL_5wLLnP-mvQ2wzR74Z-9rxQ_W7J6mCs_TCXmsbXN6NaSM_z4FiZfw";

        Manager.getInstance().downloadCertificate(accessToken, new Manager.DownloadCallback() {
            @Override
            public void onDownloaded(DeviceSerial serial) {
                Log.d(TAG, "Certificate downloaded for vehicle: " + serial);
                // Send command to the car through Telematics, make sure that the emulator is
                // opened for this to work, otherwise "Vehicle asleep" will be returned
                workWithTelematics(serial);

                // Also make the device visible through Bluetooth to the car
//                workWithBluetooth();
            }

            @Override
            public void onDownloadFailed(DownloadAccessCertificateError error) {
                Log.e(TAG, "Could not download a certificate with token: " + error
                        .getMessage());
            }
        });
    }

    private void workWithTelematics(DeviceSerial serial) {
        Telematics telematics = Manager.getInstance().getTelematics();

        Command command = new SetNaviDestination(
                new CoordinatesProperty(.2f, .2f), "Secret place"
        );

        Manager.getInstance().getTelematics().sendCommand(command, serial, new Telematics.CommandCallback() {

            @Override public void onCommandResponse(Bytes bytes) {
                Log.d(TAG, "onCommandReceived() called with: bytes = [" + bytes + "]");
            }

            @Override public void onCommandFailed(TelematicsError telematicsError) {
                Log.d(TAG, "onCommandFailed() called with: telematicsError = [" + telematicsError.getMessage() + "]");
            }
        });

        /*Command command = new GetLockState();
        Manager.getInstance().getTelematics().sendCommand(command, serial, new
                Telematics.CommandCallback() {
                    @Override
                    public void onCommandReceived(Bytes bytes) {

                        Command command = CommandResolver.resolve(bytes);

                        if (command instanceof LockState) {
                            LockState state = (LockState) command;
                            Log.d(TAG, "Telematics GetLockState response: ");
                            Log.d(TAG, "Front left state: " + state
                                    .getDoorLockAndPositionState(DoorLocation
                                            .FRONT_LEFT)
                                    .getDoorLock());
                            Log.d(TAG, "Front right state: " + state
                                    .getDoorLockAndPositionState(DoorLocation
                                            .FRONT_RIGHT)
                                    .getDoorLock());

                            Log.d(TAG, "Rear right state: " + state
                                    .getDoorLockAndPositionState(DoorLocation
                                            .REAR_RIGHT)
                                    .getDoorLock());

                            Log.d(TAG, "Rear left state: " + state
                                    .getDoorLockAndPositionState(DoorLocation
                                            .REAR_LEFT)
                                    .getDoorLock());

                        } else if (command instanceof VehicleStatus) {
                            Log.d(TAG, "vin: " + ((VehicleStatus) command)
                                    .getVin());
                        }
                    }

                    @Override
                    public void onCommandFailed(TelematicsError error) {
                        Log.d(TAG, "Could not send a command through " +
                                "telematics: " +
                                "" + error.getCode() + " " + error.getMessage
                                ());
                    }
                });*/

    }

    ConnectedLink link;
    BleCommandQueue bleQueue;

    /*IBleCommandQueue iQueue = new IBleCommandQueue() {
        @Override public void onCommandReceived(Command sentCommand, Bytes command) {
            Log.d(TAG, "onCommandReceived() called with: sentCommand = [" + sentCommand + "], " +
                    "response = [" + command + "]");
        }

        @Override public void onCommandAck(Command sentCommand) {
            Log.d(TAG, "onCommandAck() called with: sentCommand = [" + sentCommand + "]");
        }

        @Override public void onCommandFailed(CommandFailure reason) {
            Log.d(TAG, "onCommandFailed() called with: reason = [" + reason.getReason() + "]");
        }

        @Override public void sendCommand(final Command command) {
            if (link != null) link.sendCommand(command, new Link.CommandCallback() {
                @Override public void onCommandSent() {
                    bleQueue.onCommandSent(command);
                }

                @Override public void onCommandFailed(LinkError linkError) {
                    bleQueue.onCommandFailedToSend(command, linkError);
                }
            });
        }
    };

    private void workWithBluetooth() {
        // Start Bluetooth broadcasting, so that the car can connect to this device
        final Broadcaster broadcaster = Manager.getInstance().getBroadcaster();

        bleQueue = new BleCommandQueue(iQueue, 0, 3);

        broadcaster.setListener(new BroadcasterListener() {
            @Override
            public void onStateChanged(Broadcaster.State state) {
                Log.d(TAG, "Broadcasting state changed: " + state);
            }

            @Override
            public void onLinkReceived(ConnectedLink connectedLink) {
                QueueTestActivity.this.link = connectedLink;
                connectedLink.setListener(new ConnectedLinkListener() {
                    @Override
                    public void onAuthorizationRequested(ConnectedLink connectedLink,
                                                         ConnectedLinkListener
                                                                 .AuthorizationCallback
                                                                 authorizationCallback) {
                        // Approving without user input
                        authorizationCallback.approve();
                    }

                    @Override
                    public void onAuthorizationTimeout(ConnectedLink connectedLink) {

                    }

                    @Override
                    public void onStateChanged(final Link link, Link.State state) {
                        if (link.getState() == Link.State.AUTHENTICATED) {
                            bleQueue.queue(new GetCapabilities(), Capabilities.TYPE);
                            bleQueue.queue(new GetVehicleStatus(), VehicleStatus.TYPE);
                            bleQueue.queue(new LockUnlockDoors(DoorLock.LOCKED));
                        }
                        else {
                            bleQueue.purge();
                        }
                    }

                    @Override
                    public void onCommandReceived(final Link link, Bytes bytes) {
                        bleQueue.onCommandReceived(bytes);
                    }

                });
            }

            @Override
            public void onLinkLost(ConnectedLink connectedLink) {
                // Bluetooth disconnected
                bleQueue.purge();
            }
        });

        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {
                Log.d(TAG, "Bluetooth broadcasting started");
            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                Log.d(TAG, "Bluetooth broadcasting started: " + broadcastError);
            }
        });
    }*/
}