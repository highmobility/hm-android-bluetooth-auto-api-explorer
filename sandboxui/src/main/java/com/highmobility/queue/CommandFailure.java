package com.highmobility.queue;

import com.highmobility.autoapi.Failure;
import com.highmobility.hmkit.error.LinkError;

import org.jetbrains.annotations.Nullable;

/**
 * command can fail:
 * <ul>
 * <li>before even sending by bad sdk state(not authorised for instance.). {@link #linkError}
 * will be present in this case.</li>
 * <li>not receiving ack or not receiving a response << both mean timeout.</li>
 * <li>receiving a failure response. {@link #failureResponse} will be present in this case.</li>
 */
public class CommandFailure {
    public enum Reason {
        FAILED_TO_SEND,
        TIMEOUT,
        FAILURE_RESPONSE
    }

    Reason reason;
    @Nullable Failure failureResponse; // a failure response
    @Nullable LinkError linkError; // Issue in SDK before sending out the ble command.

    public Reason getReason() {
        return reason;
    }

    @Nullable public Failure getFailureResponse() {
        return failureResponse;
    }

    @Nullable public LinkError getLinkError() {
        return linkError;
    }

    public CommandFailure(Reason reason, @Nullable Failure failureResponse, @Nullable LinkError
            linkError) {
        this.reason = reason;
        this.failureResponse = failureResponse;
        this.linkError = linkError;
    }
}
