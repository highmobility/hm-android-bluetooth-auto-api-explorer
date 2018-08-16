package com.highmobility.queue;

import com.highmobility.autoapi.Command;

public interface IBleCommandQueue extends ICommandQueue {
    /**
     * Called after the user has called {@link BleCommandQueue#onCommandSent(Command)}.
     *
     * @param sentCommand The sent command.
     */
    void onCommandAck(Command sentCommand);
}