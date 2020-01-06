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
import com.highmobility.autoapi.Doors;
import com.highmobility.autoapi.FailureMessage;
import com.highmobility.autoapi.Fueling;
import com.highmobility.autoapi.Trunk;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.Location;
import com.highmobility.autoapi.value.Lock;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.autoapi.value.Position;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.value.Bytes;

import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.TestCase.assertTrue;

public class CommandQueueTest {
    final Command[] responseCommand = new Command[1];
    final int[] commandsSent = {0};
    final Command[] ackCommand = new Command[1];
    final CommandFailure[] failure = new CommandFailure[1];

    final IBleCommandQueue iQueue = new IBleCommandQueue() {
        @Override public void onCommandAck(Command queuedCommand) {
            ackCommand[0] = queuedCommand;
        }

        @Override public void onCommandReceived(Command command, @Nullable Command sentCommand) {
            responseCommand[0] = command;
        }

        @Override public void onCommandFailed(CommandFailure reason, Command sentCommand) {
            failure[0] = reason;
        }

        @Override public void sendCommand(Command command) {
            commandsSent[0]++;
        }
    };

    @Before public void prepare() {
        ackCommand[0] = null;
        responseCommand[0] = null;
        commandsSent[0] = 0;
        Link.commandTimeout = 50;
    }

    @Test public void responseAckDispatched() throws InterruptedException {
        // send lock, assert ack dispatched
        BleCommandQueue queue = new BleCommandQueue(iQueue, 500, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command);
        Thread.sleep(50);
        queue.onCommandSent(command);
        assertTrue(commandIs(ackCommand[0], Doors.IDENTIFIER, Type.SET));
    }

    @Test public void responseCommandDispatched() throws InterruptedException {
        // send lock, assert response dispatched after receiving commandResponse

        BleCommandQueue queue = new BleCommandQueue(iQueue, 500, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command, Doors.State.class);

        Thread.sleep(50);
        Command response = new Doors.State.Builder().addInsideLock(new Property(new Lock
                (Location.FRONT_LEFT, LockState.LOCKED))).build();
        queue.onCommandReceived(response);

        assertEquals(1, commandsSent[0]);

        assertTrue(commandIs(responseCommand[0], Doors.IDENTIFIER, Type.SET));
    }

    @Test public void newCommandsNotSentWhenWaitingForAnAck() {
        boolean secondResult;
        BleCommandQueue queue = new BleCommandQueue(iQueue, 500, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command);
        secondResult = queue.queue(command);

        assertEquals(false, secondResult);
        assertEquals(1, commandsSent[0]);
        queue.onCommandSent(command);
        assertEquals(ackCommand[0].getType(), command.getType());
    }

    @Test public void newCommandsNotSentWhenWaitingForAResponse() {
        boolean secondResult;
        BleCommandQueue queue = new BleCommandQueue(iQueue, 500, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command, Doors.State.class);
        secondResult = queue.queue(command, Doors.State.class);

        assertEquals(false, secondResult);
        assertEquals(1, commandsSent[0]);
        Command response = new Doors.State.Builder().addInsideLock(new Property(new Lock
                (Location.FRONT_LEFT, LockState.LOCKED))).build();
        queue.onCommandReceived(response);
        assertTrue(commandIs(responseCommand[0], Doors.IDENTIFIER, Type.SET));
        assertNull(ackCommand[0]);
    }

    @Test public void ackCommandRetriedAndFailed() throws InterruptedException {
        // assert retries are tried and command failed after. just dont send any callbacks.
        // set timeout to .05f. wait .07f, assert that command has been sent again.

        BleCommandQueue queue = new BleCommandQueue(iQueue, -(Link.commandTimeout - 70), 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        LinkError error = new LinkError(LinkError.Type.TIME_OUT, 0, "");

        queue.queue(command, Doors.State.class);
        assertEquals(1, commandsSent[0]);

        Thread.sleep(40);
        queue.onCommandFailedToSend(command, error);
        assertEquals(2, commandsSent[0]);

        Thread.sleep(40);
        queue.onCommandFailedToSend(command, error);
        assertEquals(3, commandsSent[0]);

        Thread.sleep(40);
        queue.onCommandFailedToSend(command, error);
        assertEquals(4, commandsSent[0]);

        Thread.sleep(40); // command is retried 3 times
        queue.onCommandFailedToSend(command, error);
        assertEquals(4, commandsSent[0]);

        assertSame(failure[0].getReason(), CommandFailure.Reason.TIMEOUT);
        LinkError linkError = ((LinkError) failure[0].getErrorObject());
        assertSame(linkError.getType(), LinkError.Type.TIME_OUT);
    }

    @Test public void notSentCommandNotTriedAgain() {
        // assert that when sdk failed to send command, queue tries to send again
        BleCommandQueue queue = new BleCommandQueue(iQueue, 10000, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command, Doors.State.class);

        LinkError error = new LinkError(LinkError.Type.INTERNAL_ERROR, 0, "");
        assertEquals(1, commandsSent[0]);
        queue.onCommandFailedToSend(command, error);
        assertEquals(1, commandsSent[0]);
    }

    @Test public void responseCommandAckWillStillWaitForResponseAndTryAgain() throws
            InterruptedException {
        // send command, return ack, timeout, assert command sent again
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command, Doors.State.class);

        Thread.sleep(10);
        queue.onCommandSent(command);
        assertEquals(1, commandsSent[0]);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertEquals(2, commandsSent[0]);
        queue.onCommandSent(command);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertEquals(3, commandsSent[0]);
        queue.onCommandSent(command);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertEquals(4, commandsSent[0]);
        queue.onCommandSent(command);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertEquals(4, commandsSent[0]);

        assertSame(failure[0].getReason(), CommandFailure.Reason.TIMEOUT);
        assertNull(failure[0].getErrorObject());
    }

    @Test public void ackCommandRetriedAndDispatched() throws InterruptedException {
        // assert retries are tried and response dispatched after time
        // send command, return ack, timeout, assert command sent again
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command);
        assertEquals(1, commandsSent[0]);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no ack)
        assertEquals(2, commandsSent[0]);

        Thread.sleep(10);
        queue.onCommandSent(command);

        assertEquals(ackCommand[0].getIdentifier(), Doors.IDENTIFIER, Type.SET);
    }

    @Test public void responseCommandRetriedAndDispatched() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command command = new Doors.LockUnlockDoors(LockState.LOCKED);
        queue.queue(command, Doors.State.class);
        Doors.State response = new Doors.State.Builder().addInsideLock(new Property(new Lock
                (Location.FRONT_LEFT, LockState.LOCKED))).build();

        Thread.sleep(10);
        queue.onCommandSent(command);
        assertEquals(1, commandsSent[0]);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertEquals(2, commandsSent[0]);
        queue.onCommandSent(command);

        Thread.sleep(20);

        queue.onCommandReceived(response);
        assertEquals(2, commandsSent[0]);

        assertNull(failure[0]);
        assertEquals(ackCommand[0].getIdentifier(), Doors.IDENTIFIER, Type.SET);
    }

    @Test public void multipleResponsesReceived() throws InterruptedException {
        // assert 2 queued items responses will both be dispatched
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new Doors.LockUnlockDoors(LockState.LOCKED);
        Command secondCommand = new Fueling.GetGasFlapState();

        Doors.State firstResponse =
                new Doors.State.Builder().addInsideLock(new Property(new Lock
                        (Location.FRONT_LEFT, LockState.LOCKED))).build();
        Fueling.State secondResponse =
                new Fueling.State.Builder().setGasFlapPosition(new Property(Position.CLOSED)).build();

        queue.queue(firstCommand, Doors.State.class);
        queue.queue(secondCommand, Fueling.State.class);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(commandIs(responseCommand[0], Doors.IDENTIFIER, Type.SET));

        assertEquals(2, commandsSent[0]); // assert get gas flap state sent
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(secondResponse);
        assertEquals(2, commandsSent[0]);
        assertTrue(commandIs(responseCommand[0], Fueling.IDENTIFIER, Type.SET));

        assertNull(failure[0]);
    }

    @Test public void ackAndResponseCommandResponsesReceived() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new Doors.LockUnlockDoors(LockState.LOCKED);
        Command secondCommand = new Trunk.ControlTrunk(LockState.LOCKED, Position.CLOSED);
        Command thirdCommand = new Fueling.ControlGasFlap(LockState.LOCKED, Position.OPEN);

        Doors.State firstResponse =
                new Doors.State.Builder().addInsideLock(new Property(new Lock
                        (Location.FRONT_LEFT, LockState.LOCKED))).build();

        queue.queue(firstCommand, Doors.State.class);
        queue.queue(secondCommand);
        queue.queue(thirdCommand);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(Link.commandTimeout + 20);
        assertEquals(2, commandsSent[0]); //assert retried

        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        assertEquals(2, commandsSent[0]); //assert still no new commands sent
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertEquals(ackCommand[0].getIdentifier(), Doors.IDENTIFIER, Type.SET);

        assertEquals(3, commandsSent[0]); // assert trunk command sent after response
        Thread.sleep(10);
        queue.onCommandSent(secondCommand);
        assertEquals(4, commandsSent[0]); // sent OpenGasFlap(third command)
        assertSame(ackCommand[0].getIdentifier(), Trunk.IDENTIFIER);
        Thread.sleep(10);
        queue.onCommandSent(thirdCommand);
        assertEquals(ackCommand[0].getIdentifier(), Fueling.IDENTIFIER);
        assertEquals(4, commandsSent[0]);

        assertNull(failure[0]);
    }

    @Test public void multipleResponsesReceivedSomeTimedOut() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new Doors.LockUnlockDoors(LockState.LOCKED);
        Command secondCommand = new Fueling.GetGasFlapState();

        Doors.State firstResponse =
                new Doors.State.Builder().addInsideLock(new Property(new Lock(Location.FRONT_LEFT,
                        LockState.LOCKED))).build();

        queue.queue(firstCommand, Doors.State.class);
        queue.queue(secondCommand, Fueling.State.class);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertEquals(ackCommand[0].getIdentifier(), Doors.IDENTIFIER, Type.SET);

        assertEquals(2, commandsSent[0]); // assert get gas flap state sent
        // time out the second one
        Thread.sleep((Link.commandTimeout + 40) * 3);
        assertEquals(5, commandsSent[0]); // assert get gas flap state sent 4x
        Thread.sleep((Link.commandTimeout + 20));

        assertSame(failure[0].getReason(), CommandFailure.Reason.TIMEOUT);
        assertNull(failure[0].getErrorObject());
    }

    @Test public void failureStopsQueue() throws InterruptedException {
        // assert command failure will be dispatched
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new Doors.LockUnlockDoors(LockState.LOCKED);
        Command secondCommand = new Fueling.GetGasFlapState();

        FailureMessage.State errorResponse =
                new FailureMessage.State.Builder()
                        .setFailedMessageType(new Property(Type.SET))
                        .setFailedMessageID(new Property(Doors.IDENTIFIER))
                        .setFailedPropertyIDs(new Property(new Bytes(Doors.PROPERTY_LOCKS_STATE)))
                        .setFailureReason(new Property(FailureMessage.FailureReason.UNSUPPORTED_CAPABILITY)).build();

        queue.queue(firstCommand, Doors.State.class);
        queue.queue(secondCommand, Fueling.State.class);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(errorResponse);
        assertNull(responseCommand[0]);

        assertSame(failure[0].getReason(), CommandFailure.Reason.FAILURE_RESPONSE);
        assertNull(failure[0].getErrorObject());
        assertSame(failure[0].getFailureResponse().getFailedMessageID().getValue(),
                Doors.IDENTIFIER);

        assertEquals(1, commandsSent[0]); // assert get gas flap state was not sent.
        Thread.sleep(10);
    }

    @Test public void irrelevantCommandDispatchedQueueContinued() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new Doors.LockUnlockDoors(LockState.LOCKED);

        Command firstResponse =
                new Fueling.State.Builder().setGasFlapPosition(new Property(Position.CLOSED)).build();
        Command secondResponse =
                new Doors.State.Builder().addInsideLock(new Property(new Lock
                        (Location.FRONT_LEFT, LockState.LOCKED))).build();

        queue.queue(firstCommand, Doors.State.class);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        // send a random command
        queue.onCommandReceived(firstResponse);
        // make sure random command is dispatched as well
        assertTrue(commandIs(responseCommand[0], Fueling.IDENTIFIER, Type.SET));

        assertEquals(1, commandsSent[0]); // assert still same amount of commands sent.
        Thread.sleep(10);
        queue.onCommandReceived(secondResponse); // the real response
        assertTrue(commandIs(responseCommand[0], Doors.IDENTIFIER, Type.SET)); // assert real
        // command dispatched

        assertNull(failure[0]);
    }

    /*boolean commandIs(@Nonnull Bytes bytes, Type type) {
        return ByteUtils.startsWith(bytes.getByteArray(), type.getIdentifierAndType());
    }*/

    boolean commandIs(@Nonnull Command command, Integer identifier, int commandType) {
        return command.getIdentifier() == identifier && command.getType() == commandType;
    }
}
