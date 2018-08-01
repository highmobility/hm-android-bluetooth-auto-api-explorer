package com.highmobility.queue;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.Type;

public class QueueItem {
    Command command;
    Type commandResponse;
    
    public QueueItem(Command command, Type commandResponse) {
        this.command = command;
        this.commandResponse = commandResponse;
    }
}