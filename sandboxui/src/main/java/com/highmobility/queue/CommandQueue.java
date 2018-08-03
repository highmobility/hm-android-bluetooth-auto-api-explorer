package com.highmobility.queue;

import android.support.annotation.Nullable;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.Type;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.LinkError;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Queue system for BLE commands. An item will wait for its ack ({@link #queue(Command)}) or for its
 * response command ({@link #queue(Command, Type)}) before next items will be sent. Ack timeout will
 * come from the sdk. Response command timeout is handled by this class(hence extraTimeout in
 * ctor).
 *
 * <ul><li> Command will fail without retrying when SDK returns a LinkError or the response is a
 * Failure command.</li>
 * <li>Command will succeed after ack or the expected response command via
 * {@link ICommandQueue}</li>
 * <li>Commands will be timed out after {@link Link#commandTimeout} + extraTimeout.</li>
 * <li>Commands will be tried again for {@link #retryCount} times.  Commands with same type will
 * not be queued.</li></ul>
 * <p>
 * Command responses have to be dispatched to:
 * <ul>
 * <li>{@link #onCommandReceived(Command)}</li>
 * <li>{@link #onCommandSent(Command)} (ack)</li>
 * <li>{@link #onCommandFailedToSend(Command, LinkError)}</li>
 * </ul>
 */
public class CommandQueue {
    ICommandQueue listener;
    long timeout;
    int retryCount;
    ArrayList<QueueItem_> items = new ArrayList<>();
    Timer timeOutTimer;

    /**
     * @param extraTimeout Timeout in ms. Is added to {@link Link#commandTimeout} as an extra buffer
     *                     to receive the command response. Ack itself is timed out in the sdk after
     *                     {@link Link#commandTimeout}
     */
    public CommandQueue(ICommandQueue listener, long extraTimeout, int retryCount) {
        this.listener = listener;
        this.timeout = Link.commandTimeout + extraTimeout;
        timeout = Link.commandTimeout + extraTimeout;
        this.retryCount = retryCount;
    }

    /**
     * Queue the command and wait for its response command.
     *
     * @param command The command and its response that will be queued.
     * @return false if cannot queue at this time - maybe this command type is already queued.
     */
    public boolean queue(Command command, Type responseType) {
        if (typeAlreadyQueued(command)) return false;
        QueueItem_ item = new QueueItem_(command, responseType);
        items.add(item);
        sendItem();
        return true;
    }

    /**
     * Queue the command and wait for its ack.
     *
     * @param command The command will be queued.
     * @return false if cannot queue at this time - maybe this command type is already queued.
     */
    public boolean queue(Command command) {
        if (typeAlreadyQueued(command)) return false;
        QueueItem_ item = new QueueItem_(command, null);
        items.add(item);
        sendItem();
        return true;
    }

    public void onCommandFailedToSend(Command command, LinkError linkError) {
        // retry only if timeout, otherwise go straight to failure.
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);
        if (item.commandSent.getType().equals(command.getType()) == false) return;

        item.linkError = linkError;
        if (linkError.getType() == LinkError.Type.TIME_OUT && item.retryCount < retryCount) {
            item.retryCount++;
            item.timeSent = null;
            sendItem();
        } else {
            failItem();
        }
    }

    public void onCommandReceived(Command command) {
        // we only care about first item in queue
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);
        if (item.responseType == null) return;

        Failure failure = null;
        if (command instanceof Failure) {
            failure = (Failure) command;
        }

        if (failure != null) {
            if (item.commandSent.getType().equals(failure.getFailedType())) {
                item.failure = failure;
                failItem();
            }
        } else if (command.getType().equals(item.responseType)) {
            items.remove(0);
            sendItem();
            listener.onCommandResponse(item.commandSent, command);
        }
    }

    public void onCommandSent(Command command) {
        // we only care about first item in queue
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);

        if (command.getType().equals(item.commandSent.getType())) {
            if (item.responseType == null) {
                items.remove(0);
                sendItem();
                listener.onCommandAck(item.commandSent);
            }
        }
    }

    QueueItem_ getItem(Command command) {
        for (int i = 0; i < items.size(); i++) {
            QueueItem_ item = items.get(i);
            if (item.commandSent.getType().equals(command.getType())) return item;
        }

        return null;
    }

    boolean typeAlreadyQueued(Command command) {
        for (int i = 0; i < items.size(); i++) {
            QueueItem_ item = items.get(i);
            if (item.commandSent.getType().equals(command.getType())) return true;
        }
        return false;
    }

    void sendItem() {
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);

        if (item.timeSent == null) {
            item.timeSent = Calendar.getInstance();
            System.out.println("queue: send " + item.commandSent);
            listener.sendCommand(item.commandSent);
            startTimer();
        }
    }

    void failItem() {
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);

        CommandFailure.Reason reason;
        if (item.failure != null) {
            reason = CommandFailure.Reason.FAILURE_RESPONSE;
        } else if (item.linkError != null && item.linkError.getType() != LinkError.Type.TIME_OUT) {
            reason = CommandFailure.Reason.FAILED_TO_SEND;
        } else {
            reason = CommandFailure.Reason.TIMEOUT;
        }
        items.remove(item);
        CommandFailure failure = new CommandFailure(reason, item.failure, item.linkError);
        sendItem(); // fail one, send the next one before dispatching to lock the sdk
        listener.onCommandFailed(failure);
    }

    void startTimer() {
        // start if not running already
        if (timeOutTimer == null) {
            timeOutTimer = new Timer();
            timeOutTimer.scheduleAtFixedRate(retryTask, new Date(), 30);
        }
    }

    void stopTimer() {
        // stop if queue empty
        if (items.size() == 0) {
            retryTask.cancel();
            timeOutTimer.purge();
            timeOutTimer.cancel();
            timeOutTimer = null;
        }
    }

    TimerTask retryTask = new TimerTask() {
        @Override public void run() {
            sendCommandAgainIfTimeout();
        }
    };

    void sendCommandAgainIfTimeout() {
        if (items.size() > 0) {
            QueueItem_ item = items.get(0);
            long now = Calendar.getInstance().getTimeInMillis();
            long sent = item.timeSent.getTimeInMillis();

            if (now - sent > timeout) {
                item.timeSent = null;
                item.retryCount++;

                if (item.retryCount > retryCount) {
                    failItem();
                } else {
                    sendItem();
                }
            }
        } else {
            stopTimer();
        }
    }

    class QueueItem_ {
        Command commandSent;

        LinkError linkError;
        Failure failure;

        Calendar timeSent;
        int retryCount;
        @Nullable Type responseType;

        public QueueItem_(Command commandSent, @Nullable Type
                responseType) {
            this.commandSent = commandSent;
            this.responseType = responseType;
        }
    }
}