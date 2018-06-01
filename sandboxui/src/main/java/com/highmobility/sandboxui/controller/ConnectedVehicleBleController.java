package com.highmobility.sandboxui.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.highmobility.hmkit.BroadcastConfiguration;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.Broadcaster.State;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.error.BroadcastError;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;
import com.highmobility.hmkit.error.RevokeError;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.sandboxui.view.RemoteControlActivity;
import com.highmobility.value.Bytes;
import java.util.List;

import static com.highmobility.hmkit.Broadcaster.State.BLUETOOTH_UNAVAILABLE;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleBleController extends ConnectedVehicleController implements
        BroadcasterListener, ConnectedLinkListener {
    static String IS_BROADCASTING_SERIAL_PREFS_KEY = "isBroadcastingSerial";
    Broadcaster broadcaster;
    ConnectedLink link;

    IConnectedVehicleBleView bleView;
    boolean isBroadcastingSerial;
    int alivePingInterval = -1;
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
        i.putExtra(RemoteControlController.LINK_IDENTIFIER_MESSAGE, link.getSerial().getByteArray());
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
    @Override public void init() {
        super.init();
        broadcaster = Manager.getInstance().getBroadcaster();
        broadcaster.setListener(this);

        // check for connected links for this vehicle and if is authenticated and show the
        // appropriate ui
        List<ConnectedLink> links = broadcaster.getLinks();

        boolean linkExists = false;
        for (ConnectedLink link : links) {
            if (certificate.getGainerSerial().equals(link.getSerial())) {
                // the link is to our vehicle, show either authenticated or connected view
                linkExists = true;
                onLinkReceived(link);
                onStateChanged(link, link.getState());
            }
        }

        if (linkExists == false) startBroadcasting();
    }

    @Override
    void sendCommand(Bytes command) {
        // link could be lost at any time and for instance on initialize it could try to send
        // commands without checking
        if (link == null) {
            onCommandError(-1, "No connection to a link.");
            return;
        }

        link.sendCommand(command, new Link.CommandCallback() {
            @Override
            public void onCommandSent() {
                onBleAckReceived();
            }

            @Override
            public void onCommandFailed(LinkError linkError) {
                onCommandError(1, linkError.getType() + " " + linkError.getMessage());
            }
        });
    }

    @Override
    public void onStateChanged(State state) {
        switch (broadcaster.getState()) {
            case IDLE:
                bleView.showBleInfoView(true, "Idle");

                if (state == BLUETOOTH_UNAVAILABLE) {
                    startBroadcasting();
                }
                break;
            case BLUETOOTH_UNAVAILABLE:
                bleView.showBleInfoView(true, "Bluetooth N/A");
                break;
            case BROADCASTING:
                if (link == null) {
                    bleView.showBleInfoView(true, "Looking for links... " + manager
                            .getBroadcaster().getName());
                }
                break;
        }
    }

    @Override
    public void onLinkReceived(ConnectedLink connectedLink) {
        if (link != null) {
            Log.d(SandboxUi.TAG, "received new link, ignore");
            return;
        }

        link = connectedLink;
        link.setListener(this);
        bleView.showBleInfoView(true, "link: " + connectedLink.getState());
        bleView.onLinkReceived(true);

        vehicle.onLinkReceived();
        Log.d(SandboxUi.TAG, "onLinkReceived: ");
    }

    // Link listener

    @Override
    public void onLinkLost(ConnectedLink connectedLink) {
        if (connectedLink == link) {
            link.setListener(null);
            link = null;
            onStateChanged(broadcaster.getState());
            bleView.onLinkReceived(false);
            vehicle.vehicleConnectedWithBle = null;
            Log.d(SandboxUi.TAG, "onLinkLost: ");

            if (initializing) {
                // stop the initialisation if link was lost
                retryCount = 0;
                initializing = false;
                cancelInitTimer();
            }
        } else {
            Log.d(SandboxUi.TAG, "unknown link lost");
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
    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        Log.d(SandboxUi.TAG, "link state changed " + link.getState());
        if (link == this.link) {
            if (link.getState() == Link.State.AUTHENTICATED) {
                vehicle.onLinkAuthenticated(link);
                bleView.showBleInfoView(false, "link: " + "authenticated");
                readyToSendCommands();
            } else if (link.getState() == Link.State.CONNECTED) {
                bleView.showBleInfoView(true, "link: " + "connected");
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, Bytes bytes) {
        onCommandReceived(bytes);
    }

    @Override public void willDestroy() {
        // clear references to hmkit
        broadcaster.setListener(null);

        for (ConnectedLink link : broadcaster.getLinks()) {
            link.setListener(null);
        }

        broadcaster.disconnectAllLinks();
        broadcaster = null;

        super.willDestroy();
    }

    @Override public void onRevokeClicked() {
        view.showLoadingView(true);

        link.revoke(Manager.getInstance().getDeviceCertificate().getSerial(), new Link.RevokeCallback() {

            @Override public void onRevokeSuccess() {
                // should hide the view here probably, or maybe link will de authorise
            }

            @Override public void onRevokeFailed(RevokeError revokeError) {
                view.onError(false, "Revoke failed: " + revokeError.getMessage());
            }
        });
    }

    void startBroadcasting() {
        BroadcastConfiguration conf = null;
        if (isBroadcastingSerial) {
            conf = new BroadcastConfiguration.Builder().setBroadcastingTarget(certificate
                    .getGainerSerial()).build();
        }

        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {
                if (alivePingInterval != -1) broadcaster.startAlivePinging(alivePingInterval);
                onStateChanged(broadcaster.getState());
            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                onStateChanged(broadcaster.getState());
                Log.e(SandboxUi.TAG, "cant start broadcasting " + broadcastError.getType());
            }
        }, conf);
    }
}
