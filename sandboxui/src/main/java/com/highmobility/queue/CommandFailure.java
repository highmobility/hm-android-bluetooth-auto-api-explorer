package com.highmobility.queue;

import android.support.annotation.Nullable;

import com.highmobility.autoapi.Failure;
import com.highmobility.hmkit.error.LinkError;

/**
 * command can fail:
 * * before even sending by bad sdk state(not authorised for instance.). {@link #linkError} will be
 * present in this case.
 * * not receiving ack or not receiving a response << both mean timeout.
 * * receiving a failure response. {@link #failureResponse} will be present in this case.
 */
public class CommandFailure {
    public enum Reason {
        FAILED_TO_SEND,
        TIMEOUT,
        FAILURE_RECEIVED
    }

    Reason reason;
    @Nullable Failure failureResponse;
    @Nullable LinkError linkError;

    public CommandFailure(Reason reason, @Nullable Failure failureResponse, @Nullable LinkError
            linkError) {
        this.reason = reason;
        this.failureResponse = failureResponse;
    }
}
