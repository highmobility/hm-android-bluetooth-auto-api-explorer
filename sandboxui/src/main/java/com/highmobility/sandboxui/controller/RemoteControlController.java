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

import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.Toast;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;

import com.highmobility.autoapi.FailureMessage;
import com.highmobility.autoapi.RemoteControl;
import com.highmobility.autoapi.value.measurement.Angle;
import com.highmobility.autoapi.value.measurement.Speed;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.HMKit;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.AuthenticationError;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.sandboxui.model.VehicleState;
import com.highmobility.sandboxui.util.ITapToControlCommandConverter;
import com.highmobility.sandboxui.util.TapToControlCommandConverter;
import com.highmobility.sandboxui.view.IRemoteControlView;
import com.highmobility.value.Bytes;

import java.util.List;

import static timber.log.Timber.d;

public class RemoteControlController implements IRemoteControlController, ConnectedLinkListener,
        ITapToControlCommandConverter {
    public static final String LINK_IDENTIFIER_MESSAGE = "com.highmobility.digitalkeydemo" +
            ".LINKIDENTIFIER";
    private static final String TAG = "LinkViewController";

    IRemoteControlView view;

    ConnectedLink link;
    VehicleState vehicle;
    CountDownTimer timeoutTimer;
    boolean initializing = true;
    boolean controlling = false;

    TapToControlCommandConverter converter;

    public RemoteControlController(IRemoteControlView view) {
        this.view = view;
        Intent intent = view.getActivity().getIntent();
        byte[] serial = intent.getByteArrayExtra(LINK_IDENTIFIER_MESSAGE);

        vehicle = ConnectedVehicleController.vehicle;
        List<ConnectedLink> links = HMKit.getInstance().getBroadcaster().getLinks();

        for (ConnectedLink link : links) {
            if (link.getSerial().equals(serial)) {
                this.link = link;
                this.link.setListener(this);
                break;
            }
        }

        // ask for initial state
        requestInitialState();
    }

    @Override
    public void onAuthenticationRequest(ConnectedLink link, ConnectedLinkListener
            .AuthenticationRequestCallback callback) {
    }

    @Override
    public void onAuthenticationRequestTimeout(ConnectedLink link) {
    }

    @Override public void onAuthenticationFailed(Link link, AuthenticationError error) {
        d("onAuthenticationFailed(): %s", error.getMessage());
    }

    @Override
    public void onStateChanged(Link link, Link.State newState, Link.State oldState) {
        if (newState != Link.State.AUTHENTICATED) {
            if (initializing) {
                onInitializeFinished(1, "Not authenticated");
            } else {
                view.getActivity().finish();
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, Bytes bytes) {
        Command command = CommandResolver.resolve(bytes);

        if (command instanceof RemoteControl.State) {
            RemoteControl.State controlMode = (RemoteControl.State) command;
            onRemoteControlStateUpdate(controlMode);
        } else if (command instanceof FailureMessage.State) {
            FailureMessage.State failure = (FailureMessage.State) command;
            d("failure %s", failure.getFailureReason().toString());
            if (initializing) {
                onInitializeFinished(1, failure.getFailedMessageID() + " " + failure.getFailedMessageType()
                        + " failed: " + failure.getFailureReason().toString());
            } else {
                onCommandFinished(failure.getFailureReason().toString());
            }
        } else {
            vehicle.update(command);
        }
    }

    // LinkView

    @Override
    public void onMoveButtonClicked(int index) {
        converter.onMoveButtonClicked(index);
    }

    @Override
    public void onStopButtonClicked() {
        converter.onStopClicked();
    }

    // TapConverter

    @Override
    public void onSpeedChanged(int speed) {
        if (speed != 0) {
            view.showStopButton(true);
            HMKit.getInstance().getBroadcaster().startAlivePinging(50);
            sendControlCommand();
        } else {
            view.showStopButton(false);
            HMKit.getInstance().getBroadcaster().stopAlivePinging();
        }
    }

    @Override
    public void onAngleChanged(int angle) {
        sendControlCommand();
    }

    void requestInitialState() {
        view.showLoadingView(true);
        startInitializeTimer();

        link.sendCommand(new RemoteControl.GetControlState(), new Link.CommandCallback() {
            @Override
            public void onCommandSent() {

            }

            @Override
            public void onCommandFailed(LinkError linkError) {
                onInitializeFinished(1, linkError.getType() + ": Cant get control mode");
            }
        });
    }

    void onRemoteControlStateUpdate(RemoteControl.State remoteControl) {
        d("onRemoteControl.StateUpdate(): %s", remoteControl.getControlMode().toString());

        RemoteControl.ControlMode controlModeValue = remoteControl.getControlMode().getValue();

        if (initializing) {
            // we are initializing
            if (controlModeValue == RemoteControl.ControlMode.AVAILABLE) {
                link.sendCommand(new RemoteControl.StartControl(), new Link.CommandCallback() {
                    @Override
                    public void onCommandSent() {
                        // wait for the control command
                    }

                    @Override
                    public void onCommandFailed(LinkError linkError) {
                        onInitializeFinished(1, linkError.getType() + ": Cant start control mode");
                    }
                });
            } else if (controlModeValue == RemoteControl.ControlMode.STARTED) {
                onInitializeFinished(0, "");
            } else {
                onInitializeFinished(1, "Bad control mode " + controlModeValue);
            }
        } else if (controlModeValue != RemoteControl.ControlMode.STARTED) {
            onInitializeFinished(1, "Bad control mode " + controlModeValue);
        }
    }

    void onCommandFinished(String error) {
        view.showLoadingView(false);

        if (error != null) showToast(error);
        timeoutTimer.cancel();
    }

    void onInitializeFinished(int errorCode, String text) {
        timeoutTimer.cancel();
        initializing = false;

        if (errorCode != 0) {
            controlling = false;
            link.setListener(null);
            showToast("QueueError " + errorCode + ":" + text);
            view.getActivity().finish();
        } else {
            converter = new TapToControlCommandConverter(this);
            view.showLoadingView(false);
            controlling = true;
            sendControlCommand();
        }
    }

    void sendControlCommand() {
        if (controlling == false) return;

        final int angle = converter.getAngle();
        final int speed = converter.getSpeed();

        link.sendCommand(new RemoteControl.ControlCommand(
                new Angle(angle, Angle.Unit.DEGREES),
                new Speed(speed, Speed.Unit.KILOMETERS_PER_HOUR)), new Link.CommandCallback() {
            @Override
            public void onCommandSent() {
                if (speed == 0 && converter.getSpeed() == 0 && converter.getAngle() == angle) {
                    // everything is up to date
                    return;
                }

                sendControlCommand();
            }

            @Override
            public void onCommandFailed(LinkError linkError) {
                if (linkError.getType() == LinkError.Type.COMMAND_IN_PROGRESS)
                    return; // previous command is waiting to be executed
                if (linkError.getType() == LinkError.Type.TIME_OUT) return; // something is wrong
            }
        });
    }

    void startInitializeTimer() {
        // 30 s
        timeoutTimer = new CountDownTimer((Link.commandTimeout * 2000), 15000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                onInitializeFinished(-12, "Initialization timeout");
            }
        }.start();
    }

    void showToast(String text) {
        Toast.makeText(view.getActivity(), text, Toast.LENGTH_LONG).show();
        d(text);
    }
}
