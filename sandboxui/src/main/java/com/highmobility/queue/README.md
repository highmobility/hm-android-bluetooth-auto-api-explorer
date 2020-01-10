Queue can be used to send multiple commands in a row. It is meant to be used as a layer between 
HMKit and the app, where all link communication is forwarded to the queue. It returns the response 
commands or failure indications if the commands fail.

See [BleCommandQueue](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/sandboxui/src/main/java/com/highmobility/queue/BleCommandQueue.java) or [TelematicsCommandQueue](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/sandboxui/src/main/java/com/highmobility/queue/TelematicsCommandQueue.java) for more info about using the queue.

```java
ConnectedLink link;
BleCommandQueue queue;

void sendCommands() {
  queue = new BleCommandQueue(iQueue);
  // get the VehicleStatus and wait for a response before sending OpenGasFlap.
  queue.queue(new VehicleStatus.GetVehicleStatus(), VehicleStatus.State.class);
  // send OpenGasFlap and only wait for the ack, not the GasFlapState response.
  queue.queue(new Fueling.ControlGasFlap(LockState.LOCKED, Position.CLOSED));
  // send HonkAndFlash straight after the OpenGasFlap ack.
  queue.queue(new HonkHornFlashLights.HonkFlash(3, 3));
}

// forward all link communication to the queue.

// MARK: LinkListener

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

  @Override public void onCommandReceived(Command command, Command sentCommand) {
    // One of the queue commands got a response.
  }

  @Override public void onCommandFailed(CommandFailure reason, Command sentCommand) {
    // A command failed. All commands in queue are cancelled.
  }

  @Override public void sendCommand(Command command) {
    // Send a queue command via link 
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
