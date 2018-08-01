package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.value.Bytes;

public interface ICommandQueue {
    void onCommandResponse(QueueItem queuedItem, Bytes response);

    void onCommandAck(Command queuedCommand);

    void onCommandFailed();

    void sendCommand(Command command);
}