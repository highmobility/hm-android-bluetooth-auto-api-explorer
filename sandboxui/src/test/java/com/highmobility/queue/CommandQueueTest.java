package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.property.doors.DoorLocation;
import com.highmobility.autoapi.property.doors.DoorLock;
import com.highmobility.autoapi.property.doors.DoorLockState;
import com.highmobility.hmkit.error.LinkError;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class CommandQueueTest {
    final Command[] responseCommand = new Command[1];
    final int[] commandsSent = {0};
    final Command[] ackCommand = new Command[1];
    final CommandFailure[] failure = new CommandFailure[1];

    final ICommandQueue iQueue = new ICommandQueue() {
        @Override public void onCommandResponse(Command queuedItem, Command response) {
            responseCommand[0] = response;
        }

        @Override public void onCommandAck(Command queuedCommand) {
            ackCommand[0] = queuedCommand;
        }

        @Override public void onCommandFailed(CommandFailure reason) {
            failure[0] = reason;
        }

        @Override public void sendCommand(Command command) {
            commandsSent[0]++;
        }
    };

    @Before public void clearIQueue() {
        ackCommand[0] = null;
        responseCommand[0] = null;
        commandsSent[0] = 0;
    }

    @Test public void responseAckDispatched() throws InterruptedException {
        // send lock, assert ack dispatched
        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        Thread.sleep(50);
        queue.onAckReceived(command);
        assertTrue(ackCommand[0].getType().equals(LockUnlockDoors.TYPE));
    }

    @Test public void responseCommandDispatched() throws InterruptedException {
        // send lock, assert response dispatched after receiving commandResponse

        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);

        Thread.sleep(50);
        Command response = new LockState.Builder().addInsideLockState(new DoorLockState
                (DoorLocation.FRONT_LEFT, DoorLock.LOCKED)).build();
        queue.onCommandReceived(response);

        assertTrue(commandsSent[0] == 1);
        assertTrue(((Command) responseCommand[0]).getType().equals(LockState.TYPE));
    }

    @Test public void newCommandsNotSentWhenWaitingForAnAck() {

        boolean secondResult;
        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        secondResult = queue.queue(command);

        assertTrue(secondResult == false);
        assertTrue(commandsSent[0] == 1);
        queue.onAckReceived(command);
        assertTrue(ackCommand[0].getType().equals(command.getType()));
    }

    @Test public void newCommandsNotSentWhenWaitingForAResponse() {
        boolean secondResult;
        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);
        secondResult = queue.queue(command, LockState.TYPE);

        assertTrue(secondResult == false);
        assertTrue(commandsSent[0] == 1);
        Command response = new LockState.Builder().addInsideLockState(new DoorLockState
                (DoorLocation.FRONT_LEFT, DoorLock.LOCKED)).build();
        queue.onCommandReceived(response);
        assertTrue(responseCommand[0].getType().equals(LockState.TYPE));
        assertTrue(ackCommand[0] == null);
    }

    @Test public void ackCommandRetriedAndFailed() throws InterruptedException {
        // assert retries are tried and command failed after. just dont send any callbacks.
        // set timeout to .05f. dont sent callback. wait .07f, assert that command has been sent
        // again

        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);

        assertTrue(commandsSent[0] == 1);
        Thread.sleep(70);
        assertTrue(commandsSent[0] == 2);
        Thread.sleep(70);
        assertTrue(commandsSent[0] == 3);
        Thread.sleep(70);
        assertTrue(commandsSent[0] == 4);
        Thread.sleep(70);
        assertTrue(commandsSent[0] == 4);
    }

    @Test public void notSentCommandTriedAgain() throws InterruptedException {
        // assert that when sdk failed to send command, queue tries to send again
        CommandQueue queue = new CommandQueue(iQueue, 10000, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);

        LinkError error = new LinkError(LinkError.Type.INTERNAL_ERROR, 0, "");
        assertTrue(commandsSent[0] == 1);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 2);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 3);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 4);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 4);
    }

    @Test public void responseCommandAckWillStillWaitForResponseAndTryAgain() {

    }

    @Test public void responseCommandRetriedAndFailed() {

    }

    @Test public void ackCommandRetriedAndDispatched() {
        // assert retries are tried and response dispatched after time
    }

    @Test public void responseCommandRetriedAndDispatched() {

    }

    @Test public void multipleResponsesReceived() {
        // assert 2 queued items responses will both be dispatched
    }

    @Test public void multipleResponsesReceivedSomeTimedOut() {
        // assert 2 queued items one response will be dispatched, others timeout

    }

    @Test public void multipleResponsesReceivedSomeFailedWithError() {
        // assert command failure will be dispatched
    }

    @Test public void timeoutErrorDispatched() {

    }

    @Test public void noAckErrorDispatched() {

    }

    @Test public void noResponseErrorDispatched() {

    }

    @Test public void FailureErrorDispatched() {

    }
}
