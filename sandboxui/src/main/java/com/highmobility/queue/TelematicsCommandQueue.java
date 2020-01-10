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
    }

    public void onCommandFailedToSend(Command command, TelematicsError error) {
        super.onCommandFailedToSend(command, error, false);
    }
}
