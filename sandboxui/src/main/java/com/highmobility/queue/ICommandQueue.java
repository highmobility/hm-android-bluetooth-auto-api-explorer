package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.value.Bytes;

import javax.annotation.Nullable;

public interface ICommandQueue {
    /**
     * Called after the user has called {@link CommandQueue#onCommandReceived(Bytes)}.
     *
     * @param command     The response.
     * @param sentCommand The sent command. Null if didn't match with a sent command.
     */
    void onCommandReceived(Command command, @Nullable Command sentCommand);

    /**
     * Called when a command failed. If this happens, the queue is cleared as well.
     *
     * @param reason      The failure reason.
     * @param sentCommand The sent command.
     */
    void onCommandFailed(CommandFailure reason, Command sentCommand);

    /**
     * Called when the command should be sent with either Ble or Telematics.
     *
     * @param command The command.
     */
    void sendCommand(Command command);
}