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
 * Queue system for BLE commands. An item will wait for its ack {@link #queue(Command)} or for its
 * response command {@link #queue(Command)} before next items will be sent.
 * <p>
 * Command responses have to be dispatched to
 * <p>
 * {@link #onCommandReceived(Command)}
 * {@link #onAckReceived(Command)} and
 * {@link #onCommandFailedToSend(Command, LinkError)}
 * <p>
 * Command will fail without retrying when SDK doesn't send the command or a response is a failure.
 * Command will succeed after ack or the expected response command via {@link ICommandQueue}
 * Commands will be timed out after {@link Link#commandTimeout} + extraTimeout.
 * Commands will be tried again for {@link #retryCount} times.
 * Commands with same type will not be queued.
 */
public class CommandQueue {
    ICommandQueue listener;
    long timeout;
    int retryCount;
    ArrayList<QueueItem_> items = new ArrayList<>();
    Timer timeOutTimer;

    /**
     * @param extraTimeout Timeout in ms. Is added to {@link Link#commandTimeout} as an extra buffer
     *                     to receive the command response.
     */
    public CommandQueue(ICommandQueue listener, long extraTimeout, int retryCount) {
        this.listener = listener;
        this.timeout = Link.commandTimeout + extraTimeout;
        if (Link.commandTimeout < timeout) Link.commandTimeout = (int) timeout - 20;
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
        QueueItem_ item = new QueueItem_(command, retryCount, responseType);
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
        QueueItem_ item = new QueueItem_(command, retryCount, null);
        items.add(item);
        sendItem();
        return true;
    }

    public void onCommandFailedToSend(Command command, LinkError linkError) {
        // TODO: 02/08/2018 retry only if timeout, otherwise go straight to failure.
        QueueItem_ item = getItem(command);
        item.linkError = linkError;

        if (linkError.getType() == LinkError.Type.TIME_OUT && item.retryCount < retryCount) {
            item.linkError = linkError;
            item.retryCount++;
            sendItem();
        } else {
            failItem();
        }
    }

    public void onCommandReceived(Command command) {
        for (int i = items.size() - 1; i >= 0; i--) {
            QueueItem_ item = items.get(i);
            if (item.responseType == null) continue;
            if (command.getType().equals(item.responseType)) {
                items.remove(i);
                listener.onCommandResponse(item.commandSent, command);
                sendItem();
                break;
            }
            // TODO: 02/08/2018 if failure fail instantly, dont retry again
        }
    }

    public void onAckReceived(Command command) {
        for (int i = items.size() - 1; i >= 0; i--) {
            QueueItem_ item = items.get(i);
            // TODO:
            if (command.getType().equals(item.commandSent.getType())) {
                items.remove(i);
                listener.onCommandAck(item.commandSent);
                sendItem();
                break;
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

    // the first item is always sent. if has timeout, the timer task will set timeSent to null.
    /*boolean sendNextItem() {
        if (items.size() > 0) {
            QueueItem_ item = items.get(0);

            if (item.timeSent == null) {
                item.timeSent = Calendar.getInstance();
                System.out.println("send " + item.commandSent);
                listener.sendCommand(item.commandSent);
                startTimer();
                return true;
            }
        } else {
            stopTimer();
        }

        return false;
    }*/

    void sendItem() {
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);

        if (item.timeSent == null) {
            item.timeSent = Calendar.getInstance();
            System.out.println("send " + item.commandSent);
            listener.sendCommand(item.commandSent);
            startTimer();
        }
    }

    void retryItem() {
// TODO: 02/08/2018
    }

    void failItem() {
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);

        CommandFailure.Reason reason;
        if (item.failure != null) {
            reason = CommandFailure.Reason.FAILURE_RECEIVED;
        } else if (item.linkError != null && item.linkError.getType() != LinkError.Type.TIME_OUT) {
            reason = CommandFailure.Reason.FAILED_TO_SEND;
        } else {
            reason = CommandFailure.Reason.TIMEOUT;
        }

        items.remove(item);
        CommandFailure failure = new CommandFailure(reason, item.failure, item.linkError);
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

            if (now - item.timeSent.getTimeInMillis() > timeout) {
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

        public QueueItem_(Command commandSent, int retryCount, @Nullable Type
                responseType) {
            this.commandSent = commandSent;
            this.retryCount = retryCount;
            this.responseType = responseType;
        }
    }
}

//    TODO: can add option to queue 1 more command of same type that will be overwritten by new
// queue
//    items. For instance lock, unlock, lock, lock, lock will start lock and queue lock

/*
public interface ICommandQueue {
    void onCommandResponse(QueueItem queuedItem, Command response);

    void onCommandAck(Command queuedItem);

    void onCommandFailed();

    void sendCommand(Bytes bytes);
}

public class QueueItem {
    Command command;
    Reason commandResponse;
}
 */