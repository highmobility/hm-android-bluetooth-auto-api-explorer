package com.highmobility.queue;

import com.highmobility.autoapi.Failure;

import org.jetbrains.annotations.Nullable;

/**
 * Command can fail:
 * <ul>
 * <li>Before sending it to the vehicle(not authorised for instance.). {@link #errorObject} will be
 * present in this case and is either LinkError or TelematicsError.</li>
 * <li>Not receiving ack or not receiving a response. Both mean timeout.</li>
 * <li>Receiving a failure response from the vehicle. {@link #failureResponse} will be present in
 * this case. The queue will be cleared after a failure response.</li>
 */
public class CommandFailure {
    public enum Reason {
        FAILED_TO_SEND, // the command did not reach vehicle. errorObject is filled in that case.
        TIMEOUT, // the command timed out.
        FAILURE_RESPONSE // the vehicle responded with a failure response
    }

    Reason reason;
    @Nullable Failure failureResponse; // a failure response
    @Nullable Object errorObject; // Issue in SDK before sending out the ble command.

    public Reason getReason() {
        return reason;
    }

    @Nullable public Failure getFailureResponse() {
        return failureResponse;
    }

    /**
     * @return { @link {@link com.highmobility.hmkit.error.TelematicsError} } for Telematics queue,
     * { @link {@link com.highmobility.hmkit.error.LinkError} } for Ble queue.
     */
    @Nullable public Object getErrorObject() {
        return errorObject;
    }

    CommandFailure(Reason reason, @Nullable Failure failureResponse, @Nullable Object
            errorObject) {
        this.reason = reason;
        this.failureResponse = failureResponse;
        this.errorObject = errorObject;
    }
}
