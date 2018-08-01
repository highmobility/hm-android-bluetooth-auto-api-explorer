package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.property.doors.DoorLock;
import com.highmobility.value.Bytes;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class CommandQueueTest {
    @Test public void responseAckDispatched() throws InterruptedException {
        // send lock, assert ack dispatched

        final boolean[] ackDispatched = new boolean[1];
        CommandQueue queue = null;
        queue = new CommandQueue(new ICommandQueue() {
            @Override public void onCommandResponse(QueueItem queuedItem, Bytes response) {

            }

            @Override public void onCommandAck(Command queuedCommand) {
                ackDispatched[0] = true;
            }

            @Override public void onCommandFailed() {

            }

            @Override public void sendCommand(Command command) {

            }
        }, .5f, 3);

        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        Thread.sleep(50);
        queue.onAckReceived(command);
        assertTrue(ackDispatched[0] == true);
    }

    @Test public void newCommandsNotSentWhenWaitingForAnAck() {
        final int[] commandsSent = {0};
        final Command[] ackCommand = new Command[1];
        CommandQueue queue;
        ICommandQueue iCommandQueue = new ICommandQueue() {
            @Override public void onCommandResponse(QueueItem queuedItem, Bytes response) {

            }

            @Override public void onCommandAck(Command queuedCommand) {
                ackCommand[0] = queuedCommand;
            }

            @Override public void onCommandFailed() {

            }

            @Override public void sendCommand(Command command) {
                commandsSent[0]++;
            }
        };

        boolean secondResult;
        queue = new CommandQueue(iCommandQueue, .5f, 3);
        Command command = new LockUnlockDoors(DoorLock.LOCKED);
        queue.queue(command);
        secondResult = queue.queue(command);

        assertTrue(secondResult == false);
        assertTrue(commandsSent[0] == 1);
        queue.onAckReceived(command);
        assertTrue(ackCommand[0].getType().equals(command.getType()));
    }

    @Test public void newCommandsNotSentWhenWaitingForAResponse() {

    }

    @Test public void responseCommandDispatched() {
        // send lock, assert response dispatched after receiving commandResponse

    }

    @Test public void commandRetriedFailed() {
        // assert retries are tried and command failed after
    }

    @Test public void commandRetriedResponseDispatched() {
        // assert retries are tried and response dispatched after time
    }

    @Test public void commandTimedOut() {
        // assert command timeout works
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
}
