# Explore AutoAPIs

This repository contains a sample app that uses HMKit to connect to a 
vehicle/emulator via Bluetooth and send some commands like lock doors and turn on lights.

### Dependencies

* hmkit-android

### Install

Import the Gradle project with Android Studio(3.0.1 was used for development). HMKit requires 
a device with Lollipop or higher.

To make the app work you need to initialise the HMKit with a certificate from the developer 
center. The flow is described in the comments of VehicleController.java.

