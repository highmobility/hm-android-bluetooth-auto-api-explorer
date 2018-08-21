package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.value.Bytes;

/**
 * Queue system for Telematics commands that is meant to be used as a layer between the app and
 * HMKit. A command will wait for its response before sending next items. Command responses are
 * dispatched via {@link ICommandQueue}.
 * <p>
 * Command responses from the HMKit have to be forwarded to:
 * <ul>
 * <li>{@link #onCommandReceived(Bytes)}</li>
 * <li>{@link #onCommandFailedToSend(Command, TelematicsError)}</li>
 * </ul>
 * Be aware:
 * <ul>
 * <li>If there is an issue with a command, the queue will be cleared. There is no
 * retrying.</li></ul>
 * <p>
 * Call {@link #purge()} to clear the queue if necessary.
 */
public class TelematicsCommandQueue extends CommandQueue {

    public TelematicsCommandQueue(ICommandQueue listener) {
        // timeout is irrelevant here, SDK will fail the command if there is a timeout.
        super(listener, 120000, 0);
        allCommandsAreResponses = true;
    }

    public void onCommandFailedToSend(Command command, TelematicsError error) {
        super.onCommandFailedToSend(command, error, false);
    }
}
