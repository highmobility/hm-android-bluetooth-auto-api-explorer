package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Failure;
import com.highmobility.autoapi.GasFlapState;
import com.highmobility.autoapi.GetGasFlapState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.OpenCloseTrunk;
import com.highmobility.autoapi.property.FailureReason;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;
import com.highmobility.autoapi.property.doors.DoorLocation;
import com.highmobility.autoapi.property.doors.DoorLock;
import com.highmobility.autoapi.property.doors.DoorLockState;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.error.LinkError;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

// TODO: 07/08/2018

public class CommandQueueTest {
    /*final Command[] responseCommand = new Command[1];
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

    @Before public void prepare() {
        ackCommand[0] = null;
        responseCommand[0] = null;
        commandsSent[0] = 0;
        Link.commandTimeout = 50;
    }

    @Test public void responseAckDispatched() throws InterruptedException {
        // send lock, assert ack dispatched
        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        Thread.sleep(50);
        queue.onCommandSent(command);
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
        assertTrue((responseCommand[0]).getType().equals(LockState.TYPE));
    }

    @Test public void newCommandsNotSentWhenWaitingForAnAck() {

        boolean secondResult;
        CommandQueue queue = new CommandQueue(iQueue, 500, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        secondResult = queue.queue(command);

        assertTrue(secondResult == false);
        assertTrue(commandsSent[0] == 1);
        queue.onCommandSent(command);
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

        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        LinkError error = new LinkError(LinkError.Type.TIME_OUT, 0, "");

        queue.queue(command, LockState.TYPE);
        assertTrue(commandsSent[0] == 1);

        Thread.sleep(Link.commandTimeout);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 2);

        Thread.sleep(Link.commandTimeout);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 3);

        Thread.sleep(Link.commandTimeout);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 4);

        Thread.sleep(Link.commandTimeout); // command is retried 3 times
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 4);

        assertTrue(failure[0].getReason() == CommandFailure.Reason.TIMEOUT);
        assertTrue(failure[0].getErrorObject().getType() == LinkError.Type.TIME_OUT);
    }

    @Test public void notSentCommandNotTriedAgain() throws InterruptedException {
        // assert that when sdk failed to send command, queue tries to send again
        CommandQueue queue = new CommandQueue(iQueue, 10000, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);

        LinkError error = new LinkError(LinkError.Type.INTERNAL_ERROR, 0, "");
        assertTrue(commandsSent[0] == 1);
        queue.onCommandFailedToSend(command, error);
        assertTrue(commandsSent[0] == 1);
    }

    @Test public void responseCommandAckWillStillWaitForResponseAndTryAgain() throws
            InterruptedException {
        // send command, return ack, timeout, assert command sent again
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);

        Thread.sleep(10);
        queue.onCommandSent(command);
        assertTrue(commandsSent[0] == 1);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertTrue(commandsSent[0] == 2);
        queue.onCommandSent(command);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertTrue(commandsSent[0] == 3);
        queue.onCommandSent(command);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertTrue(commandsSent[0] == 4);
        queue.onCommandSent(command);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertTrue(commandsSent[0] == 4);

        assertTrue(failure[0].getReason() == CommandFailure.Reason.TIMEOUT);
        assertTrue(failure[0].getErrorObject() == null);
    }

    @Test public void ackCommandRetriedAndDispatched() throws InterruptedException {
        // assert retries are tried and response dispatched after time
        // send command, return ack, timeout, assert command sent again
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        assertTrue(commandsSent[0] == 1);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no ack)
        assertTrue(commandsSent[0] == 2);

        Thread.sleep(10);
        queue.onCommandSent(command);

        assertTrue(ackCommand[0].getType().equals(LockUnlockDoors.TYPE));
    }

    @Test public void responseCommandRetriedAndDispatched() throws InterruptedException {
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command, LockState.TYPE);
        LockState response = new LockState.Builder().addInsideLockState(new DoorLockState
                (DoorLocation.FRONT_LEFT, DoorLock.LOCKED)).build();

        Thread.sleep(10);
        queue.onCommandSent(command);
        assertTrue(commandsSent[0] == 1);

        Thread.sleep(Link.commandTimeout + 20); //sends command again(no response)
        assertTrue(commandsSent[0] == 2);
        queue.onCommandSent(command);

        Thread.sleep(20);

        queue.onCommandReceived(response);
        assertTrue(commandsSent[0] == 2);

        assertTrue(failure[0] == null);
        assertTrue(responseCommand[0].getType() == LockState.TYPE);
    }

    @Test public void multipleResponsesReceived() throws InterruptedException {
        // assert 2 queued items responses will both be dispatched
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(DoorLock.LOCKED);
        Command secondCommand = new GetGasFlapState();

        LockState firstResponse = new LockState.Builder().addInsideLockState(new DoorLockState
                (DoorLocation.FRONT_LEFT, DoorLock.LOCKED)).build();
        GasFlapState secondResponse = new GasFlapState.Builder().setState(com.highmobility.autoapi
                .property.GasFlapState.CLOSED).build();

        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand, GasFlapState.TYPE);

        assertTrue(commandsSent[0] == 1);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(responseCommand[0].getType().equals(LockState.TYPE));

        assertTrue(commandsSent[0] == 2); // assert get gas flap state sent
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(secondResponse);
        assertTrue(commandsSent[0] == 2);
        assertTrue(responseCommand[0].getType().equals(GasFlapState.TYPE));

        assertTrue(failure[0] == null);
    }

    @Test public void ackAndResponseCommandResponsesReceived() throws InterruptedException {
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(DoorLock.LOCKED);
        Command secondCommand = new OpenCloseTrunk(TrunkLockState.LOCKED, TrunkPosition.CLOSED);

        LockState firstResponse = new LockState.Builder().addInsideLockState(new DoorLockState
                (DoorLocation.FRONT_LEFT, DoorLock.LOCKED)).build();

        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand);

        assertTrue(commandsSent[0] == 1);
        Thread.sleep(Link.commandTimeout + 20);
        assertTrue(commandsSent[0] == 2); //assert retried

        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        assertTrue(commandsSent[0] == 2); //assert still no new commands sent
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(responseCommand[0].getType().equals(LockState.TYPE));

        assertTrue(commandsSent[0] == 3); // assert trunk command sent after response
        Thread.sleep(10);
        queue.onCommandSent(secondCommand);
        assertTrue(commandsSent[0] == 3);
        assertTrue(ackCommand[0].getType().equals(OpenCloseTrunk.TYPE));

        assertTrue(failure[0] == null);
    }

    @Test public void multipleResponsesReceivedSomeTimedOut() throws InterruptedException {
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(DoorLock.LOCKED);
        Command secondCommand = new GetGasFlapState();

        LockState firstResponse = new LockState.Builder().addInsideLockState(new DoorLockState
                (DoorLocation.FRONT_LEFT, DoorLock.LOCKED)).build();

        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand, GasFlapState.TYPE);

        assertTrue(commandsSent[0] == 1);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(responseCommand[0].getType().equals(LockState.TYPE));

        assertTrue(commandsSent[0] == 2); // assert get gas flap state sent
        // time out the second one
        Thread.sleep((Link.commandTimeout + 20) * 3);
        assertTrue(commandsSent[0] == 5); // assert get gas flap state sent 4x
        Thread.sleep((Link.commandTimeout + 20));

        assertTrue(failure[0].getReason() == CommandFailure.Reason.TIMEOUT);
        assertTrue(failure[0].getErrorObject() == null);
    }

    @Test public void multipleResponsesReceivedSomeFailedWithError() throws InterruptedException {
        // assert command failure will be dispatched
        CommandQueue queue = new CommandQueue(iQueue, 0, 3);
        Command firstCommand = new LockUnlockDoors(DoorLock.LOCKED);
        Command secondCommand = new GetGasFlapState();

        Failure firstResponse = new Failure.Builder().setFailedType(LockUnlockDoors.TYPE)
                .setFailureReason(FailureReason.UNSUPPORTED_CAPABILITY).build();
        GasFlapState secondResponse = new GasFlapState.Builder().setState(com.highmobility.autoapi
                .property.GasFlapState.CLOSED).build();

        queue.queue(firstCommand, LockState.TYPE);
        queue.queue(secondCommand, GasFlapState.TYPE);

        assertTrue(commandsSent[0] == 1);
        Thread.sleep(10);
        queue.onCommandSent(firstCommand);
        Thread.sleep(10);
        queue.onCommandReceived(firstResponse);
        assertTrue(responseCommand[0] == null);

        assertTrue(failure[0].getReason() == CommandFailure.Reason.FAILURE_RESPONSE);
        assertTrue(failure[0].getErrorObject() == null);
        assertTrue(failure[0].getFailureResponse().getFailedType().equals(LockUnlockDoors.TYPE));
        assertTrue(failure[0].getFailureResponse().getFailureReason().equals(FailureReason.UNSUPPORTED_CAPABILITY));

        assertTrue(commandsSent[0] == 2); // assert get gas flap state sent
        Thread.sleep(10);
        queue.onCommandReceived(secondResponse);
        assertTrue(responseCommand[0].getType().equals(GasFlapState.TYPE));
    }*/
}
