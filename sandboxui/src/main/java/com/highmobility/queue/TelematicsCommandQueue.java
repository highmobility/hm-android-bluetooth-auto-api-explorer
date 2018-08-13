package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.value.Bytes;

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
        this(listener, 0, 3);
    }

    /**
     * @param listener
     * @param extraTimeout Timeout in ms. Is added to {@link Link#commandTimeout} as an extra buffer
     *                     to receive the command response. Ack itself is timed out in the sdk after
     *                     {@link Link#commandTimeout}
     * @param retryCount
     */
    public TelematicsCommandQueue(ICommandQueue listener, long extraTimeout, int retryCount) {
        super(listener, extraTimeout, retryCount);
        allCommandsAreResponses = true;
        // TODO: 06/08/2018 for telematics, use a timeout not related to Link#commandTimeout
    }

    public void onCommandFailedToSend(Command command, TelematicsError error) {
        boolean timeout = error.getType() == TelematicsError.Type.TIMEOUT;
        super.onCommandFailedToSend(command, error, timeout);

        // TODO: 06/08/2018 try again on HTTP_ERROR, TIMEOUT, INVALID_SERVER_RESPONSE
    }
}
