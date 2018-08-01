package com.highmobility.queue;

import android.support.annotation.Nullable;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Type;
import com.highmobility.value.Bytes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;

/**
 * Queue system for commands. An item will wait for its ack {@link #queue(Command)} or for its
 * response command {@link #queue(QueueItem)} before next items will be sent.
 * <p>
 * Command responses have to be dispatched to {@link #onCommandReceived(Bytes)} and {@link
 * #onAckReceived(Command)} and responses will be dispatched via {@link ICommandQueue}
 * <p>
 * Commands will be timed out after {@link #timeout} and will be tried again for {@link #retryCount}
 * times.
 * <p>
 * Will not queue commands with same type.
 */
public class CommandQueue {
    ICommandQueue listener;
    float timeout;
    int retryCount;
    ArrayList<QueueItem_> items = new ArrayList<>();
    Timer timeOutTimer;

    public CommandQueue(ICommandQueue listener, float timeout, int retryCount) {
        this.listener = listener;
        this.timeout = timeout;
        this.retryCount = retryCount;
    }

    /**
     * Queue the command and wait for its response command.
     *
     * @param item The command and its response that will be queued.
     * @return false if cannot queue at this time - maybe this command type is already queued.
     */
    public boolean queue(QueueItem item) {

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
        sendNextItem();
        return true;
    }

    public void onCommandReceived(Bytes command) {
        stopTimer();
    }

    public void onAckReceived(Command command) {
        for (int i = items.size() - 1; i >= 0; i--) {
            QueueItem_ item = items.get(i);
            if (command.getType().equals(item.commandSent.getType())) {
                items.remove(i);
                listener.onCommandAck(item.commandSent);
                sendNextItem();
                break;
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

    boolean sendNextItem() {
        if (items.size() > 0) {
            QueueItem_ item = items.get(0);

            if (item.timeSent == null) {
                item.timeSent = Calendar.getInstance();
                listener.sendCommand(item.commandSent);
                startTimer();
                return true;
            }
        } else {
            stopTimer();
        }

        return false;
    }

    void startTimer() {
        // start if not running already
        // TODO: 01/08/2018
    }

    void stopTimer() {
        // stop if queue empty
        if (items.size() == 0) {
            // TODO: 01/08/2018  
        }
    }

    class QueueItem_ {
        Command commandSent;
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


/*
public interface ICommandQueue {
    void onCommandResponse(QueueItem queuedItem, Command response);

    void onCommandAck(Command queuedItem);

    void onCommandFailed();

    void sendCommand(Bytes bytes);
}

public class QueueItem {
    Command command;
    Type commandResponse;
}
 */