package com.highmobility.queue;

import com.highmobility.autoapi.Failure;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.hmkit.error.TelematicsError;

import javax.annotation.Nullable;

/**
 * Command can fail:
 * <ul>
 * <li>Before sending it to the vehicle(not authorised for instance.). {@link #errorObject} will be
 * present in this case and is either LinkError or TelematicsError.</li>
 * <li>If not receiving an ack or not receiving a response. Both mean timeout.</li>
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

    @Nullable public String getErrorMessage() {
        if (failureResponse != null) {
            return failureResponse.getFailureDescription().getValue();
        } else if (errorObject instanceof TelematicsError) {
            TelematicsError sdkError = (TelematicsError) errorObject;
            return sdkError.getMessage();
        } else if (errorObject instanceof LinkError) {
            LinkError sdkError = (LinkError) errorObject;
            return sdkError.getMessage();
        }

        return null;
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
