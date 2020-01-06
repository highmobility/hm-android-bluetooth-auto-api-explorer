/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.queue;

import com.highmobility.autoapi.FailureMessage;
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
    @Nullable FailureMessage.State failureResponse; // a failure response
    @Nullable Object errorObject; // Issue in SDK before sending out the ble command.

    public Reason getReason() {
        return reason;
    }

    @Nullable public FailureMessage.State getFailureResponse() {
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

    CommandFailure(Reason reason, @Nullable FailureMessage.State failureResponse, @Nullable Object
            errorObject) {
        this.reason = reason;
        this.failureResponse = failureResponse;
        this.errorObject = errorObject;
    }
}
