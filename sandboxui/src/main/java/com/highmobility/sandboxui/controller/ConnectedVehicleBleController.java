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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.highmobility.autoapi.Command;
import com.highmobility.commandqueue.BleCommandQueue;
import com.highmobility.commandqueue.BleQueueConfiguration;
import com.highmobility.commandqueue.QueueItemFailure;
import com.highmobility.commandqueue.IBleCommandQueue;
import com.highmobility.commandqueue.QueueItem;
import com.highmobility.hmkit.BroadcastConfiguration;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.Broadcaster.State;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.AuthenticationError;
import com.highmobility.hmkit.error.BroadcastError;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.hmkit.error.RevokeError;
import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.sandboxui.view.IConnectedVehicleBleView;
import com.highmobility.sandboxui.view.IConnectedVehicleView;
import com.highmobility.sandboxui.view.RemoteControlActivity;
import com.highmobility.value.Bytes;

import java.util.List;

import javax.annotation.Nullable;

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
    Resources resources;

    ConnectedVehicleBleController(IConnectedVehicleView view, IConnectedVehicleBleView bleView,
                                  int alivePingInterval) {
        super(true, view);
        this.bleView = bleView;
        sharedPref = view.getActivity().getPreferences(Context.MODE_PRIVATE);
        resources = view.getActivity().getResources();
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
        if (broadcaster == null) return; // in tests the cert is downloaded first
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
        queue = new BleCommandQueue(iQueue, new BleQueueConfiguration(5000, 3, 700));

        // check for connected links for this vehicle and if is authenticated and show the
        // appropriate ui
        List<ConnectedLink> links = broadcaster.getLinks();

        boolean linkExists = false;
        for (ConnectedLink link : links) {
            if (certificate.getGainerSerial().equals(link.getSerial())) {
                linkExists = true;
                onLinkReceived(link);
                onStateChanged(link, link.getState(), link.getState());
            }
        }

        if (linkExists == false) startBroadcasting();
    }

    @Override <T extends Command> void queueCommand(Command command, Class<T> response) {
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
                bleView.setBleInfo(resources.getString(R.string.idle));

                if (state == BLUETOOTH_UNAVAILABLE) {
                    startBroadcasting();
                }

                break;
            case BLUETOOTH_UNAVAILABLE:
                bleView.setBleInfo(resources.getString(R.string.ble_na));

                break;
            case BROADCASTING:
                if (link == null) {
                    bleView.setBleInfo(String.format(resources.getString(R.string.looking_for_links), hmKit
                            .getBroadcaster().getName()));
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
        vehicle.onLinkConnected();
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
    public void onAuthenticationRequest(ConnectedLink connectedLink, ConnectedLinkListener
            .AuthenticationRequestCallback callback) {
        callback.approve();
    }

    @Override
    public void onAuthenticationRequestTimeout(ConnectedLink connectedLink) {
        view.onError(true, "authorization request timed out");
        view.getActivity().finish();
    }

    @Override public void onAuthenticationFailed(Link link, AuthenticationError error) {
        d("onAuthenticationFailed(): %s", error.getMessage());
    }

    @Override
    public void onStateChanged(Link link, Link.State newState, Link.State oldState) {
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

        @Override public void onCommandReceived(Command command, @Nullable QueueItem sentCommand) {
            ConnectedVehicleBleController.this.onCommandReceived(command,
                    sentCommand.getCommandSent());
        }

        @Override public void onCommandFailed(QueueItemFailure queueItemFailure) {
            ConnectedVehicleBleController.this.onCommandFailed(queueItemFailure);
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
