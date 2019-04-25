package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.ControlGasFlap;
import com.highmobility.autoapi.ControlTrunk;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.GasFlapState;
import com.highmobility.autoapi.GetGasFlapState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.FailureReason;
import com.highmobility.autoapi.value.Location;
import com.highmobility.autoapi.value.Lock;
import com.highmobility.autoapi.value.Position;
import com.highmobility.autoapi.value.doors.DoorLockState;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.utils.ByteUtils;
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
    final Bytes[] responseCommand = new Bytes[1];
    final int[] commandsSent = {0};
    final Command[] ackCommand = new Command[1];
    final CommandFailure[] failure = new CommandFailure[1];

    final IBleCommandQueue iQueue = new IBleCommandQueue() {
        @Override public void onCommandAck(Command queuedCommand) {
            ackCommand[0] = queuedCommand;
        }

        @Override public void onCommandReceived(Bytes command, @Nullable Command sentCommand) {
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
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command);
        Thread.sleep(50);
        queue.onCommandSent(command);
        assertEquals(ackCommand[0].getType(), LockUnlockDoors.TYPE);
    }

    @Test public void responseCommandDispatched() throws InterruptedException {
        // send lock, assert response dispatched after receiving commandResponse

        BleCommandQueue queue = new BleCommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command, LockState.TYPE);

        Thread.sleep(50);
        Command response = new LockState.Builder().addInsideLock(new Property(new DoorLockState
                (Location.FRONT_LEFT, Lock.LOCKED))).build();
        queue.onCommandReceived(response);

        assertEquals(1, commandsSent[0]);
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE));
    }

    @Test public void newCommandsNotSentWhenWaitingForAnAck() {

        boolean secondResult;
        BleCommandQueue queue = new BleCommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(Lock.LOCKED);
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
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command, LockState.TYPE);
        secondResult = queue.queue(command, LockState.TYPE);

        assertEquals(false, secondResult);
        assertEquals(1, commandsSent[0]);
        Command response = new LockState.Builder().addInsideLock(new Property(new DoorLockState
                (Location.FRONT_LEFT, Lock.LOCKED))).build();
        queue.onCommandReceived(response);
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE));
        assertNull(ackCommand[0]);
    }

    @Test public void ackCommandRetriedAndFailed() throws InterruptedException {
        // assert retries are tried and command failed after. just dont send any callbacks.
        // set timeout to .05f. wait .07f, assert that command has been sent again.

        BleCommandQueue queue = new BleCommandQueue(iQueue, -(Link.commandTimeout - 70), 3);
        Command command = new LockUnlockDoors(Lock.LOCKED);
        LinkError error = new LinkError(LinkError.Type.TIME_OUT, 0, "");

        queue.queue(command, LockState.TYPE);
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
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command, LockState.TYPE);

        LinkError error = new LinkError(LinkError.Type.INTERNAL_ERROR, 0, "");
        assertEquals(1, commandsSent[0]);
        queue.onCommandFailedToSend(command, error);
        assertEquals(1, commandsSent[0]);
    }

    @Test public void responseCommandAckWillStillWaitForResponseAndTryAgain() throws
            InterruptedException {
        // send command, return ack, timeout, assert command sent again
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command, LockState.TYPE);

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
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command);
        assertEquals(1, commandsSent[0]);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no ack)
        assertEquals(2, commandsSent[0]);

        Thread.sleep(10);
        queue.onCommandSent(command);

        assertEquals(ackCommand[0].getType(), LockUnlockDoors.TYPE);
    }

    @Test public void responseCommandRetriedAndDispatched() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command command = new LockUnlockDoors(Lock.LOCKED);
        queue.queue(command, LockState.TYPE);
        LockState response = new LockState.Builder().addInsideLock(new Property(new DoorLockState
                (Location.FRONT_LEFT, Lock.LOCKED))).build();

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
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE));
    }

    @Test public void multipleResponsesReceived() throws InterruptedException {
        // assert 2 queued items responses will both be dispatched
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(Lock.LOCKED);
        Command secondCommand = new GetGasFlapState();

        LockState firstResponse =
                new LockState.Builder().addInsideLock(new Property(new DoorLockState
                        (Location.FRONT_LEFT, Lock.LOCKED))).build();
        GasFlapState secondResponse =
                new GasFlapState.Builder().setPosition(new Property(Position.CLOSED)).build();

        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand, GasFlapState.TYPE);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE));

        assertEquals(2, commandsSent[0]); // assert get gas flap state sent
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(secondResponse);
        assertEquals(2, commandsSent[0]);
        assertTrue(bytesStartsWithType(responseCommand[0], GasFlapState.TYPE));

        assertNull(failure[0]);
    }

    @Test public void ackAndResponseCommandResponsesReceived() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(Lock.LOCKED);
        Command secondCommand = new ControlTrunk(Lock.LOCKED, Position.CLOSED);
        Command thirdCommand = new ControlGasFlap(Lock.LOCKED, Position.OPEN);

        LockState firstResponse =
                new LockState.Builder().addInsideLock(new Property(new DoorLockState
                        (Location.FRONT_LEFT, Lock.LOCKED))).build();

        queue.queue(firstCommand, LockState.TYPE);
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
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE));

        assertEquals(3, commandsSent[0]); // assert trunk command sent after response
        Thread.sleep(10);
        queue.onCommandSent(secondCommand);
        assertEquals(4, commandsSent[0]); // sent OpenGasFlap(third command)
        assertEquals(ackCommand[0].getType(), ControlTrunk.TYPE);
        Thread.sleep(10);
        queue.onCommandSent(thirdCommand);
        assertEquals(ackCommand[0].getType(), ControlGasFlap.TYPE);
        assertEquals(4, commandsSent[0]);

        assertNull(failure[0]);
    }

    @Test public void multipleResponsesReceivedSomeTimedOut() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(Lock.LOCKED);
        Command secondCommand = new GetGasFlapState();

        LockState firstResponse =
                new LockState.Builder().addInsideLock(new Property(new DoorLockState
                        (Location.FRONT_LEFT, Lock.LOCKED))).build();

        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand, GasFlapState.TYPE);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE));

        assertEquals(2, commandsSent[0]); // assert get gas flap state sent
        // time out the second one
        Thread.sleep((Link.commandTimeout + 20) * 3);
        assertEquals(5, commandsSent[0]); // assert get gas flap state sent 4x
        Thread.sleep((Link.commandTimeout + 20));

        assertSame(failure[0].getReason(), CommandFailure.Reason.TIMEOUT);
        assertNull(failure[0].getErrorObject());
    }

    @Test public void failureStopsQueue() throws InterruptedException {
        // assert command failure will be dispatched
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(Lock.LOCKED);
        Command secondCommand = new GetGasFlapState();

        Failure firstResponse =
                new Failure.Builder().setFailedTypeByte(new Property(LockUnlockDoors.TYPE.getType()))
                        .setFailureReason(new Property(FailureReason.UNSUPPORTED_CAPABILITY)).build();
        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand, GasFlapState.TYPE);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertNull(responseCommand[0]);

        assertSame(failure[0].getReason(), CommandFailure.Reason.FAILURE_RESPONSE);
        assertNull(failure[0].getErrorObject());
        assertEquals(failure[0].getFailureResponse().getFailedType(), LockUnlockDoors.TYPE);
        assertEquals(failure[0].getFailureResponse().getFailureReason(), FailureReason
                .UNSUPPORTED_CAPABILITY);

        assertEquals(1, commandsSent[0]); // assert get gas flap state was not sent.
        Thread.sleep(10);
    }

    @Test public void irrelevantCommandDispatchedQueueContinued() throws InterruptedException {
        BleCommandQueue queue = new BleCommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(Lock.LOCKED);

        Command firstResponse =
                new GasFlapState.Builder().setPosition(new Property(Position.CLOSED)).build();
        Command secondResponse =
                new LockState.Builder().addInsideLock(new Property(new DoorLockState
                (Location.FRONT_LEFT, Lock.LOCKED))).build();

        queue.queue(firstCommand, LockState.TYPE);

        assertEquals(1, commandsSent[0]);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        // send a random command
        queue.onCommandReceived(firstResponse);
        // make sure random command is dispatched as well
        assertTrue(bytesStartsWithType(responseCommand[0], GasFlapState.TYPE));

        assertEquals(1, commandsSent[0]); // assert still same amount of commands sent.
        Thread.sleep(10);
        queue.onCommandReceived(secondResponse); // the real response
        assertTrue(bytesStartsWithType(responseCommand[0], LockState.TYPE)); // assert real
        // command dispatched

        assertNull(failure[0]);
    }

    boolean bytesStartsWithType(@Nonnull Bytes bytes, Type type) {
        return ByteUtils.startsWith(bytes.getByteArray(), type.getIdentifierAndType());
    }
}
