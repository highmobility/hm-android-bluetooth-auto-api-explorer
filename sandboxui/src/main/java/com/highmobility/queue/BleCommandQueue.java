package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Type;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.value.Bytes;

/**
 * Queue system for BLE commands that is meant to be used as a layer between the app and HMKit. An
 * item will wait for its ack ({@link #queue(Command)}) or for its response command ({@link
 * #queue(Command, Type)}) before next items will be sent. Ack timeout will come from the sdk.
 * Response command timeout is handled by this class(hence extraTimeout in ctor).
 * <p>
 * Command will succeed after ack or the expected response command via {@link ICommandQueue}
 * <p>
 * For this to work, command responses have to be dispatched to:
 * <ul>
 * <li>{@link #onCommandReceived(Bytes)}</li>
 * <li>{@link #onCommandSent(Command)} (ack)</li>
 * <li>{@link #onCommandFailedToSend(Command, LinkError)}</li>
 * </ul>
 * <p>
 * Be aware:
 * <ul>
 * <li> Command will fail without retrying if the SDK returns a LinkError or the response is a
 * Failure command. The queue will be cleared then as well.</li>
 * <li>Commands will be timed out after {@link Link#commandTimeout} + extraTimeout.</li>
 * <li>Commands will be tried again for {@link #retryCount} times. Commands with same type will not
 * be queued.</li>
 * </ul>
 * Call {@link #purge()} to clear the queue when link is lost.
 */
public class BleCommandQueue extends CommandQueue {

    /**
     * Create the queue with default 3s extra timeout and 3x retry count.
     *
     * @param listener The queue interface.
     */
    public BleCommandQueue(IBleCommandQueue listener) {
        this(listener, 20000, 3);
    }

    /**
     * @param listener     The queue interface.
     * @param extraTimeout Timeout in ms. Is added to {@link Link#commandTimeout} as an extra buffer
     *                     to receive the command response. Ack itself is timed out in the sdk after
     *                     {@link Link#commandTimeout}
     * @param retryCount   The amount of times a queue item is retried.
     */
    public BleCommandQueue(IBleCommandQueue listener, long extraTimeout, int retryCount) {
        super(listener, Link.commandTimeout + extraTimeout, retryCount);
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
     * Call after {@link Link.CommandCallback#onCommandSent()}.
     *
     * @param command The command that was sent.
     */
    public void onCommandSent(Command command) {
        // we only care about first item in queue
        if (items.size() == 0) return;
        QueueItem_ item = items.get(0);

        if (command.getType().equals(item.commandSent.getType())) {
            // if only waiting for an ack then finish the item
            ((IBleCommandQueue) listener).onCommandAck(item.commandSent);
            if (item.responseType == null) {
                items.remove(0);
                sendItem();
            }
        }
    }

    /**
     * Call after {@link Link.CommandCallback#onCommandFailed(LinkError)}.
     *
     * @param command The command that was sent.
     */
    public void onCommandFailedToSend(Command command, LinkError error) {
        boolean timeout = error.getType() == LinkError.Type.TIME_OUT;
        super.onCommandFailedToSend(command, error, timeout);
    }
}
