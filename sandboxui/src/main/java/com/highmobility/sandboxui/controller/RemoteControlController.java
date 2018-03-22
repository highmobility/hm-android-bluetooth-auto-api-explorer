package com.highmobility.sandboxui.controller;

import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.ControlCommand;
import com.highmobility.autoapi.ControlMode;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.GetControlMode;
import com.highmobility.autoapi.StartControlMode;
import com.highmobility.sandboxui.model.VehicleStatus;
import com.highmobility.sandboxui.view.IRemoteControlView;
import com.highmobility.sandboxui.util.ITapToControlCommandConverter;
import com.highmobility.sandboxui.util.TapToControlCommandConverter;
import com.highmobility.utils.Bytes;
import com.highmobility.autoapi.Command;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Constants;
import com.highmobility.hmkit.Error.LinkError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;

import java.util.Arrays;
import java.util.List;

/**
 * Created by root on 02/06/2017.
 */

public class RemoteControlController implements IRemoteControlController, ConnectedLinkListener,
        ITapToControlCommandConverter {
    public static final String LINK_IDENTIFIER_MESSAGE = "com.highmobility.digitalkeydemo" +
            ".LINKIDENTIFIER";
    private static final String TAG = "LinkViewController";

    IRemoteControlView view;

    ConnectedLink link;
    VehicleStatus vehicle;
    CountDownTimer timeoutTimer;
    boolean initializing = true;
    boolean controlling = false;

    TapToControlCommandConverter converter;

    public RemoteControlController(IRemoteControlView view) {
        this.view = view;
        Intent intent = view.getActivity().getIntent();
        byte[] serial = intent.getByteArrayExtra(LINK_IDENTIFIER_MESSAGE);

        vehicle = ConnectedVehicleController.vehicle;
        List<ConnectedLink> links = Manager.getInstance().getBroadcaster().getLinks();

        for (ConnectedLink link : links) {
            if (Arrays.equals(link.getSerial(), serial)) {
                this.link = link;
                this.link.setListener(this);
                break;
            }
        }

        // ask for initial state
        requestInitialState();
    }

    @Override
    public void onAuthorizationRequested(ConnectedLink link, ConnectedLinkListener
            .AuthorizationCallback callback) {

    }

    @Override
    public void onAuthorizationTimeout(ConnectedLink link) {

    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        if (link.getState() != Link.State.AUTHENTICATED) {
            if (initializing) {
                onInitializeFinished(1, "Not authenticated");
            } else {
                view.getActivity().finish();
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, byte[] bytes) {
        Command command = CommandResolver.resolve(bytes);

        if (command instanceof ControlMode) {
            ControlMode controlMode = (ControlMode) command;
            onControlModeUpdate(controlMode);
        } else if (command instanceof Failure) {
            Failure failure = (Failure) command;
            Log.d(TAG, "failure " + failure.getFailureReason().toString());
            if (initializing) {
                onInitializeFinished(1, Bytes.hexFromBytes(failure.getFailedType()
                        .getIdentifierAndType())
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
            Manager.getInstance().getBroadcaster().setIsAlivePinging(true);
            sendControlCommand();
        } else {
            view.showStopButton(false);
            Manager.getInstance().getBroadcaster().setIsAlivePinging(false);
        }
    }

    @Override
    public void onAngleChanged(int angle) {
        sendControlCommand();
    }

    void requestInitialState() {
        view.showLoadingView(true);
        startInitializeTimer();

        link.sendCommand(new GetControlMode().getBytes(), new Link.CommandCallback() {
            @Override
            public void onCommandSent() {

            }

            @Override
            public void onCommandFailed(LinkError linkError) {
                onInitializeFinished(1, linkError.getType() + ": Cant get control mode");
            }
        });
    }

    void onControlModeUpdate(ControlMode controlMode) {
        Log.d(TAG, controlMode.getMode().toString());
        if (initializing) {
            // we are initializing
            if (controlMode.getMode() == com.highmobility.autoapi.property.ControlMode.AVAILABLE) {
                link.sendCommand(new StartControlMode().getBytes(), new Link.CommandCallback() {
                    @Override
                    public void onCommandSent() {
                        // wait for the control command
                    }

                    @Override
                    public void onCommandFailed(LinkError linkError) {
                        onInitializeFinished(1, linkError.getType() + ": Cant start control mode");
                    }
                });
            } else if (controlMode.getMode() == com.highmobility.autoapi.property.ControlMode
                    .STARTED) {
                onInitializeFinished(0, "");
            } else {
                onInitializeFinished(1, "Bad control mode " + controlMode.getMode().toString());
            }
        } else if (controlMode.getMode() != com.highmobility.autoapi.property.ControlMode.STARTED) {
            onInitializeFinished(1, "Bad control mode");
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
            showToast("Failure " + errorCode + ":" + text);
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

        link.sendCommand(new ControlCommand(speed, angle).getBytes(), new Link.CommandCallback() {
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
        timeoutTimer = new CountDownTimer((long) (Constants.commandTimeout * 2000 + 10000), 15000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                onInitializeFinished(-12, "Initialization timeout");
            }
        }.start();
    }

    void showToast(String text) {
        Toast.makeText(view.getActivity(), text, Toast.LENGTH_LONG).show();
        Log.d(TAG, text);
    }
}
