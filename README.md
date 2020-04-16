# Explore AutoAPIs

This is a sample app that shows a possible use case for HMKit. We connect to a vehicle/emulator via
Bluetooth and send some commands like lock doors and turn on lights.

## Requirements

* Android 5.0 Lollipop or higher.

## Dependencies

* hmkit-android

Dependencies are managed via gradle repositories.

## Install
* run `git submodule update --init --recursive`

* Import the Gradle project with Android Studio 3+.

* Initialise the HMKit with a certificate from the Developer Center. The flow is described in the
comments of the [BleExplorerActivity.java](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/ble-explorer-app/src/main/java/com/highmobility/exploreautoapis/BleExplorerActivity.java#L33)

* Run [BleExplorerActivity.java](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/ble-explorer-app/src/main/java/com/highmobility/exploreautoapis/BleExplorerActivity.java#L21).

After initialisation, the [sandboxui](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/tree/master/sandboxui/src/main/java/com/highmobility/sandboxui) module is loaded. All of the app logic is in that package.

## Instrumented tests

The bluetooth and telematics commands are covered by instrumented tests. For them to work one needs to 
add his keys to androidTest/res/values/credentials.xml:

```
<resources>
<string name="accessToken">the vehicle access token</string>
<string name="deviceCert">the device cert</string>
<string name="privateKey">the private key</string>
<string name="issuerPublicKey">the issuer public key</string>
</resources>
```


## Questions or Comments ?

If you have questions or if you would like to send us feedback, join our [Slack Channel](https://slack.high-mobility.com/) or email us at [support@high-mobility.com](mailto:support@high-mobility.com).
