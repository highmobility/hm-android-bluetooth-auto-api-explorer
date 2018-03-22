package com.highmobility.sandboxui.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.Broadcaster.State;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Error.BroadcastError;
import com.highmobility.hmkit.Error.LinkError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;
import com.highmobility.sandboxui.SandboxUi;
import com.highmobility.sandboxui.view.RemoteControlActivity;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;

import java.util.Arrays;
import java.util.List;

import static com.highmobility.hmkit.Broadcaster.State.*;

/**
 * Created by root on 24/05/2017.
 */
public class ConnectedVehicleBleController extends ConnectedVehicleController implements BroadcasterListener, ConnectedLinkListener {
    static String IS_BROADCASTING_SERIAL_PREFS_KEY = "isBroadcastingSerial";
    Broadcaster broadcaster;
    ConnectedLink link;

    IConnectedVehicleBleView bleView;
    boolean isBroadcastingSerial;
    SharedPreferences sharedPref;

    ConnectedVehicleBleController(IConnectedVehicleView view, IConnectedVehicleBleView bleView) {
        super(true, view);
        this.bleView = bleView;
        sharedPref = view.getActivity().getPreferences(Context.MODE_PRIVATE);
        isBroadcastingSerial = sharedPref.getBoolean(IS_BROADCASTING_SERIAL_PREFS_KEY, false);
    }

    public boolean isBroadcastingSerial() {
        return isBroadcastingSerial;
    }

    public void startRemoteControl() {
        Intent i = new Intent(view.getActivity(), RemoteControlActivity.class);
        i.putExtra(RemoteControlController.LINK_IDENTIFIER_MESSAGE, link.getSerial());
        view.getActivity().startActivityForResult(i, ConnectedVehicleActivity.REQUEST_CODE_REMOTE_CONTROL);
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
        broadcaster = Manager.getInstance().getBroadcaster();
        broadcaster.setListener(this);

        // check for connected links for this vehicle and if is authenticated and show the appropriate ui
        List<ConnectedLink> links = broadcaster.getLinks();

        boolean linkExists = false;
        for (ConnectedLink link : links) {
            if (Arrays.equals(certificate.getGainerSerial(), link.getSerial())) {
                // the link is to our vehicle, show either authenticated or connected view
                linkExists = true;
                onLinkReceived(link);
                onStateChanged(link, link.getState());
            }
        }

        if (linkExists == false) startBroadcasting();
    }

    @Override
    void sendCommand(byte[] command) {
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
                    bleView.showBleInfoView(true, "Looking for links... " + manager.getBroadcaster().getName());
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
        Log.d(SandboxUi.TAG, "onLinkAuthenticated: ");
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
        }
        else {
            Log.d(SandboxUi.TAG, "unknown link lost");
        }
    }

    @Override
    public void onAuthorizationRequested(ConnectedLink connectedLink, ConnectedLinkListener.AuthorizationCallback callback) {
        callback.approve();
    }

    @Override
    public void onAuthorizationTimeout(ConnectedLink connectedLink) {
        view.onError(true, "authorization request timed out");
    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        Log.d(SandboxUi.TAG, "link state changed " + link.getState());
        if (link == this.link ) {
            if (link.getState() == Link.State.AUTHENTICATED) {
                vehicle.onLinkAuthenticated(link);
                bleView.showBleInfoView(false, "link: " + "authenticated");
                readyToSendCommands();
            }
            else if (link.getState() == Link.State.CONNECTED) {
                bleView.showBleInfoView(true, "link: " + "connected");
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, byte[] bytes) {
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

    void startBroadcasting() {
        if (isBroadcastingSerial) broadcaster.setBroadcastingTarget(certificate.getGainerSerial());
        else broadcaster.setBroadcastingTarget(null);

        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {

            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                onStateChanged(broadcaster.getState());
                Log.e(SandboxUi.TAG, "cant start broadcasting " + broadcastError.getType());
            }
        });
    }
}
