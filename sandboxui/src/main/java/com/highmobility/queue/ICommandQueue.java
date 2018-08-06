package com.highmobility.queue;

import com.highmobility.autoapi.Command;

public interface ICommandQueue {
    void onCommandResponse(Command sentCommand, Command response);

    void onCommandAck(Command sentCommand);

    void onCommandFailed(CommandFailure reason);

    void sendCommand(Command command);
}