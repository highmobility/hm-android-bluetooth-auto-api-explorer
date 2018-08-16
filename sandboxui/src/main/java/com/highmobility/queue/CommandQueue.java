package com.highmobility.queue;

import android.support.annotation.Nullable;
import android.util.Log;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.Type;
import com.highmobility.utils.ByteUtils;
import com.highmobility.value.Bytes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This is the Command queue base class. Depending on the environment, the subclasses {@link
 * BleCommandQueue} or {@link TelematicsCommandQueue} should be used instead of this class.
 */
public class CommandQueue {

    ICommandQueue listener;
    long timeout;
    int retryCount;
    ArrayList<QueueItem_> items = new ArrayList<>();
    // for telematics, there can only be responses for commands. No other commands.
    boolean allCommandsAreResponses;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> retryHandle;

    CommandQueue(ICommandQueue listener, long timeout, int retryCount) {
        this.listener = listener;
        this.timeout = timeout;
        this.retryCount = retryCount;
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

    /**
     * Clear the queue and timers. Call when connection is lost.
     */
    public void purge() {
        items.clear();
        stopTimer();
    }

    void onCommandFailedToSend(Command command, Object error, boolean timeout) {
        Log.d("", "onCommandFailedToSend() called with: command = [" + command + "], error = ["
                + error + "], timeout = [" + timeout + "]");
        // retry only if timeout, otherwise go straight to failure.
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);
        if (item.commandSent.getType().equals(command.getType()) == false) return;

        item.sdkError = error;
        if (timeout && item.retryCount < retryCount) {
            item.timeout = true;
            item.retryCount++;
            item.timeSent = null;
            sendItem();
        } else {
            failItem();
        }
    }

    public void onCommandReceived(Bytes command) {
        // queue is empty
        if (items.size() == 0) {
            listener.onCommandReceived(command, null);
            return;
        }

        // we only care about first item in queue.
        QueueItem_ item = items.get(0);

        if (command instanceof Failure) {
            Failure failure = (Failure) command;

            if (item.commandSent.getType().equals(failure.getFailedType())) {
                item.failure = failure;
                failItem();
            }
        } else if (command.getLength() > 2) {
            if (allCommandsAreResponses || (
                    item.responseType != null &&
                            ByteUtils.startsWith(command.getByteArray(), item.responseType
                                    .getIdentifierAndType()))) {
                // received a command of expected type
                items.remove(0);
                sendItem();
                listener.onCommandReceived(command, item.commandSent);
            }
        }
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
        } else if (item.sdkError != null && item.timeout == false) {
            reason = CommandFailure.Reason.FAILED_TO_SEND;
        } else {
            reason = CommandFailure.Reason.TIMEOUT;
        }

        items.remove(item);
        CommandFailure failure = new CommandFailure(reason, item.failure, item.sdkError);
        purge();
        listener.onCommandFailed(failure, item.commandSent);
    }

    void startTimer() {
        // start if not running already
        if (retryHandle == null || retryHandle.isDone()) {
            retryHandle = scheduler.scheduleAtFixedRate(retry, 0, 30, TimeUnit.MILLISECONDS);
        }
    }

    void stopTimer() {
        // stop if queue empty
        if (items.size() == 0) {
            if (retryHandle != null) retryHandle.cancel(true);
        }
    }

    final Runnable retry = () -> sendCommandAgainIfTimeout();

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

    protected class QueueItem_ {
        Command commandSent;

        boolean timeout;
        Object sdkError;
        Failure failure;

        Calendar timeSent;
        int retryCount;
        @Nullable Type responseType;

        public QueueItem_(Command commandSent, @Nullable Type responseType) {
            this.commandSent = commandSent;
            this.responseType = responseType;
        }
    }
}