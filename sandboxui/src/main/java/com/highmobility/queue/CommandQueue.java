package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.value.Bytes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * This is the Command queue base class. Depending on the environment, the subclasses {@link
 * BleCommandQueue} or {@link TelematicsCommandQueue} should be used instead of this class.
 */
public class CommandQueue {
    ICommandQueue listener;
    long timeout;
    int retryCount;
    ArrayList<QueueItem_> items = new ArrayList<>();

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> retryHandle;

    CommandQueue(ICommandQueue listener, long timeout, int retryCount) {
        this.listener = listener;
        this.timeout = timeout;
        this.retryCount = retryCount;
        CommandResolver.setRuntime(CommandResolver.RunTime.ANDROID);
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

    public void onCommandReceived(Bytes commandBytes) {
        Command command = CommandResolver.resolve(commandBytes);

        // queue is empty
        if (items.size() == 0) {
            listener.onCommandReceived(command, null);
            return;
        }

        // we only care about first item in queue.
        QueueItem_ item = items.get(0);

        if (command instanceof FailureMessageState) {
            FailureMessageState failure = (FailureMessageState) command;

            if (failure.getCommandFailed(item.commandSent.getIdentifier(),
                    item.commandSent.getType())) {
                item.failure = failure;
                failItem();
            }
        } else if (command.getLength() > 2) {
            // for telematics, there are only responses for sent commands. No random incoming
            // commands.
            if (this instanceof TelematicsCommandQueue || command.getClass() == item.responseClass) {
                // received a command of expected type
                listener.onCommandReceived(command, item.commandSent);
                items.remove(0);
                sendItem();
            } else {
                listener.onCommandReceived(command, null);
            }
        } else {
            listener.onCommandReceived(command, null);
        }
    }

    boolean typeAlreadyQueued(Command command) {
        for (int i = 0; i < items.size(); i++) {
            QueueItem_ item = items.get(i);
            if (item.commandSent.getIdentifier() == command.getIdentifier() &&
                    item.commandSent.getType() == command.getType()) return true;
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

    void onCommandFailedToSend(Command command, Object error, boolean timeout) {
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

    protected class QueueItem_<T extends Command> {
        Command commandSent;

        boolean timeout;
        Object sdkError;
        FailureMessageState failure;

        Calendar timeSent;
        int retryCount;
        @Nullable Class<T> responseClass;

        public QueueItem_(Command commandSent, @Nullable Class<T> responseClass) {
            this.commandSent = commandSent;
            this.responseClass = responseClass;
        }
    }
}