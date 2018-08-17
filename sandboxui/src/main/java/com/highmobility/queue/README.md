This queue system can be used to send multiple commands in a row. It is meant to be used as a layer between hmkit and the app, where all link communication is forwarded to the queue. It returns the commands or failure indications if the commands fail.

```java
ConnectedLink link.
BleCommandQueue queue;

void sendCommands() {
  queue = new BleCommandQueue(iQueue);
  queue.queue(new GetVehicleStatus(), VehicleStatus.TYPE); // get the VS and wait for a response before sending OpenGasFlap.
  queue.queue(new OpenGasFlap()); // send open gas flap, only wait for the ack, not the GasFlapState response.
  queue.queue(new HonkAndFlash(3, 3)); // sent straight after OpenGasFlap ack.
}

@Override
public void onCommandReceived(Link link, Bytes bytes) {
  queue.onCommandReceived(bytes);
}

@Override
public void onLinkLost(ConnectedLink connectedLink) {
  queue.purge;
}

// MARK: CommandQueue

IBleCommandQueue iQueue = new IBleCommandQueue() {
  @Override public void onCommandAck(Command sentCommand) {
    // An ack was received for a queue command.
  }

  @Override public void onCommandReceived(Bytes command, Command sentCommand) {
    // One of the queue commands got a response.
  }

  @Override public void onCommandFailed(CommandFailure reason, Command sentCommand) {
    // The command failed. All commands in queue are cancelled.
  }

  @Override public void sendCommand(Command command) {
    link.sendCommand(command, new Link.CommandCallback() {
        @Override public void onCommandSent() {
          queue.onCommandSent(command);
        }

        @Override public void onCommandFailed(LinkError linkError) {
          queue.onCommandFailedToSend(command, linkError);
        }
    });
  }
};
```
