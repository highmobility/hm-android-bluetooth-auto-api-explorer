# Explore AutoAPIs

This repository contains a sample app that uses HMKit to connect to a 
vehicle/emulator via Bluetooth and send some commands like lock doors and turn on lights.

### Dependencies

* hmkit-android

All of the dependencies are managed via gradle repositories.

### Install

* Import the Gradle project with Android Studio 3+.
* Build the app for a device with Android 5.0 Lollipop or higher.

To make the app work you need to initialise the HMKit with a certificate from the Developer 
Center. The flow is described in the comments of the[VehicleController.java](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/app/src/main/java/com/highmobility/exploreautoapis/VehicleController.java#L76)



