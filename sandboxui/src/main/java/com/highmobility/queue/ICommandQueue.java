package com.highmobility.queue;

import android.support.annotation.Nullable;

import com.highmobility.autoapi.Command;
import com.highmobility.value.Bytes;

public interface ICommandQueue {
    /**
     * Called after the user has called {@link CommandQueue#onCommandReceived(Bytes)}.
     *
     * @param command     The response.
     * @param sentCommand The sent command. Null if an irrelevant command was received.
     */
    void onCommandReceived(Bytes command, @Nullable Command sentCommand);

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