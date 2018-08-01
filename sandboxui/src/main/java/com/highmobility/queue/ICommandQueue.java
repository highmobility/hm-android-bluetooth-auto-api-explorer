package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.value.Bytes;

public interface ICommandQueue {
    void onCommandResponse(Command sentCommand, Command response);

    void onCommandAck(Command sentCommand);

    void onCommandFailed();

    void sendCommand(Command command);
}