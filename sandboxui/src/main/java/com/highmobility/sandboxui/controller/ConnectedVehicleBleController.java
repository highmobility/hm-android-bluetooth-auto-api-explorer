package com.highmobility.sandboxui.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Type;
import com.highmobility.hmkit.BroadcastConfiguration;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.Broadcaster.State;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.BroadcastError;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.hmkit.error.RevokeError;
import com.highmobility.queue.BleCommandQueue;
import com.highmobility.queue.CommandFailure;
import com.highmobility.queue.IBleCommandQueue;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.sandboxui.view.RemoteControlActivity;
import com.highmobility.value.Bytes;

import java.util.List;

import static com.highmobility.hmkit.Broadcaster.State.BLUETOOTH_UNAVAILABLE;
import static timber.log.Timber.d;
import static timber.log.Timber.e;

public class ConnectedVehicleBleController extends ConnectedVehicleController implements
        BroadcasterListener, ConnectedLinkListener {
    static String IS_BROADCASTING_SERIAL_PREFS_KEY = "isBroadcastingSerial";
    Broadcaster broadcaster;
    ConnectedLink link;

    IConnectedVehicleBleView bleView;
    BleCommandQueue queue;
    boolean isBroadcastingSerial;
    int alivePingInterval;
    SharedPreferences sharedPref;

    ConnectedVehicleBleController(IConnectedVehicleView view, IConnectedVehicleBleView bleView,
                                  int alivePingInterval) {
        super(true, view);
        this.bleView = bleView;
        sharedPref = view.getActivity().getPreferences(Context.MODE_PRIVATE);
        isBroadcastingSerial = sharedPref.getBoolean(IS_BROADCASTING_SERIAL_PREFS_KEY, false);
        this.alivePingInterval = alivePingInterval;
    }

    public boolean isBroadcastingSerial() {
        return isBroadcastingSerial;
    }

    public void startRemoteControl() {
        Intent i = new Intent(view.getActivity(), RemoteControlActivity.class);
        i.putExtra(RemoteControlController.LINK_IDENTIFIER_MESSAGE, link.getSerial().getByteArray
                ());
        view.getActivity().startActivityForResult(i, ConnectedVehicleActivity
                .REQUEST_CODE_REMOTE_CONTROL);
    }

    public void onReturnFromRemoteControl() {
        if (link != null) {
            // take over the link listener, update the views.
            link.setListener(this);
            view.onVehicleStatusUpdate(vehicle);
        }
        // else link disconnected
    }

    public void onBroadcastSerialSwitchChanged(boolean on) {
        broadcaster.stopBroadcasting();

        isBroadcastingSerial = on;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(IS_BROADCASTING_SERIAL_PREFS_KEY, on);
        editor.commit();

        startBroadcasting();
    }

    // MARK: ConnectedVehicleController

    // this is called after the constructor so the view has access to this controller and vice versa
    @Override public void onViewInitialised() {
        super.onViewInitialised();
    }

    @Override protected void onCertificateDownloaded() {
        super.onCertificateDownloaded();
        broadcaster = hmKit.getInstance().getBroadcaster();
        broadcaster.setListener(this);
        queue = new BleCommandQueue(iQueue);

        // check for connected links for this vehicle and if is authenticated and show the
        // appropriate ui
        List<ConnectedLink> links = broadcaster.getLinks();

        boolean linkExists = false;
        for (ConnectedLink link : links) {
            if (certificate.getGainerSerial().equals(link.getSerial())) {
                linkExists = true;
                onLinkReceived(link);
                onStateChanged(link, link.getState());
            }
        }

        if (linkExists == false) startBroadcasting();
    }

    @Override
    void queueCommand(Command command, Type response) {
        // link could be lost at any time and for instance on initialize it could try to send
        // commands without checking
        if (link == null) {
            e("queueCommand: no connected link");
            return;
        }

        queue.queue(command, response);
    }

    @Override public void onRevokeClicked() {
        view.showAlert("Revoke authorisation?", "", "Yes", "No", (dialog, which) -> {

            view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
            link.revoke(new Link.RevokeCallback() {
                @Override public void onRevokeSuccess(Bytes customData) {
                    // will get Link State not_authenticated
                }

                @Override public void onRevokeFailed(RevokeError revokeError) {
                    view.onError(false, "Revoke failed: " + revokeError.getMessage());
                    view.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED);
                }
            });
        }, null);
    }

    @Override public Intent willDestroy() {
        // clear references to hmkit
        broadcaster.setListener(null);

        for (ConnectedLink link : broadcaster.getLinks()) {
            link.setListener(null);
        }

        broadcaster.disconnectAllLinks();
        broadcaster.stopBroadcasting();
        broadcaster.stopAlivePinging();
        broadcaster = null;

        return super.willDestroy();
    }

    @Override public void onDestroy() {
        queue.purge();
    }

    // BroadcasterListener

    @Override
    public void onStateChanged(State state) {
        switch (broadcaster.getState()) {
            case IDLE:
                bleView.setBleInfo("Idle");

                if (state == BLUETOOTH_UNAVAILABLE) {
                    startBroadcasting();
                }
                break;
            case BLUETOOTH_UNAVAILABLE:
                bleView.setBleInfo("Bluetooth N/A");
                break;
            case BROADCASTING:
                if (link == null) {
                    bleView.setBleInfo("Looking for links... " + hmKit
                            .getBroadcaster().getName());
                }
                break;
        }
    }

    @Override
    public void onLinkReceived(ConnectedLink connectedLink) {
        if (link != null) {
            d("received new link, ignore");
            return;
        }

        link = connectedLink;
        link.setListener(this);
        bleView.setBleInfo("link: " + connectedLink.getState());
        vehicle.onLinkConnected(link);
        bleView.setViewState(IConnectedVehicleView.ViewState.CONNECTED);

        d("onLinkReceived: ");
    }

    // Link listener

    @Override
    public void onLinkLost(ConnectedLink connectedLink) {
        if (connectedLink == link) {
            link.setListener(null);
            link = null;
            onStateChanged(broadcaster.getState());
            bleView.setViewState(IConnectedVehicleView.ViewState.BROADCASTING);
            vehicle.vehicleConnectedWithBle = null;
            d("onLinkLost: ");

            if (initialising) {
                queue.purge();
                // stop the initialisation if link was lost
                initialising = false;
            }
        } else {
            d("unknown link lost");
        }
    }

    @Override
    public void onAuthorizationRequested(ConnectedLink connectedLink, ConnectedLinkListener
            .AuthorizationCallback callback) {
        callback.approve();
    }

    @Override
    public void onAuthorizationTimeout(ConnectedLink connectedLink) {
        view.onError(true, "authorization request timed out");
        view.getActivity().finish();
    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        d("link state changed %s", link.getState());
        if (link == this.link) {
            String stateString = "link: " + link.getState().toString().toLowerCase();

            if (link.getState() == Link.State.AUTHENTICATED) {
                readyToSendCommands();
                vehicle.onLinkAuthenticated(link);
                bleView.setBleInfo(stateString);
                bleView.setViewState(IConnectedVehicleView.ViewState.AUTHENTICATED_LOADING);
            } else {
                bleView.setBleInfo(stateString);
                bleView.setViewState(IConnectedVehicleView.ViewState.CONNECTED);
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, Bytes bytes) {
        queue.onCommandReceived(bytes);
    }

    // private

    IBleCommandQueue iQueue = new IBleCommandQueue() {
        @Override public void onCommandAck(Command sentCommand) {
            // we dont care about ack
        }

        @Override public void onCommandReceived(Bytes command, Command sentCommand) {
            ConnectedVehicleBleController.this.onCommandReceived(command, sentCommand);
        }

        @Override public void onCommandFailed(CommandFailure reason, Command sentCommand) {
            ConnectedVehicleBleController.this.onCommandFailed(sentCommand, reason);
        }

        @Override public void sendCommand(Command command) {
            if (link == null) {
                queue.onCommandFailedToSend(command,
                        new LinkError(LinkError.Type.BLUETOOTH_FAILURE, 0, ""));
                return;
            }
            link.sendCommand(command, new Link.CommandCallback() {
                @Override public void onCommandSent() {
                    queue.onCommandSent(command);
                }

                @Override public void onCommandFailed(LinkError linkError) {
                    queue.onCommandFailedToSend(command, linkError);
                }
            });
        }
    };

    void startBroadcasting() {
        BroadcastConfiguration conf = null;
        if (isBroadcastingSerial) {
            conf = new BroadcastConfiguration.Builder().setBroadcastingTarget(certificate
                    .getGainerSerial()).build();
        }

        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {
                bleView.setViewState(IConnectedVehicleView.ViewState.BROADCASTING);

                if (SandboxUi.isRunningTest && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    VibrationEffect effect = VibrationEffect.createOneShot(2000,
                            VibrationEffect.DEFAULT_AMPLITUDE);
                    ((Vibrator) view.getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(effect);
                }

                if (alivePingInterval != -1) broadcaster.startAlivePinging(alivePingInterval);
                onStateChanged(broadcaster.getState());
            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                onStateChanged(broadcaster.getState());
                e("cant start broadcasting %s", broadcastError.getType());
            }
        }, conf);
    }
}
