package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.value.Bytes;

// TODO: 15/08/2018 proof read

/**
 * Queue system for Telematics commands. An item will wait for its response command ({@link
 * #queue(Command)}) before next items will be sent. Web request/command timeout will come from the
 * sdk.
 *
 * <ul><li> Command will fail without retrying if the SDK returns a TelematicsError with something
 * else than TIME_OUT or the response is a Failure command. The queue will be cleared then as
 * well.</li>
 * <li>Command will succeed after the response via {@link ICommandQueue}</li>
 * <li>Commands will be timed out after TODO: need web service timeout here </li>
 * <li>Commands will be tried again for {@link #retryCount} times.  Commands with same type will
 * not be queued.</li></ul>
 * <p>
 * Command responses have to be dispatched to:
 * <ul>
 * <li>{@link #onCommandReceived(Bytes)}</li>
 * <li>{@link #onCommandFailedToSend(Command, TelematicsError)}</li>
 * </ul>
 * <p>
 * Call {@link #purge()} to clear the queue when link is lost.
 */

public class TelematicsCommandQueue extends CommandQueue {

    /**
     * Create the queue with default values.
     *
     * @param listener The queue interface.
     */
    public TelematicsCommandQueue(ICommandQueue listener) {
        this(listener, 3);
    }

    // TODO: 15/08/2018 comment

    /**
     * Create the queue with custom retry count.
     *
     * @param listener   The queue interface.
     * @param retryCount The retry count.
     */
    public TelematicsCommandQueue(ICommandQueue listener, int retryCount) {
        // TODO: 15/08/2018 get timeout from somewhere
        super(listener, 10000, retryCount);
        allCommandsAreResponses = true;
    }

    public void onCommandFailedToSend(Command command, TelematicsError error) {
        boolean timeout = error.getType() == TelematicsError.Type.TIMEOUT;
        super.onCommandFailedToSend(command, error, timeout);
    }
}
