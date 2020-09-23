# Explore AutoAPIs

This is a sample app that shows a possible use case for HMKit. We connect to a vehicle/emulator via
Bluetooth or Telematics and send some commands like lock doors and turn on lights.

## Requirements

* Android 5.0 Lollipop or higher.

## Dependencies

* hmkit-android

Dependencies are managed via gradle repositories.

## Install
* run `git submodule update --init --recursive`

* Import the Gradle project with Android Studio 3+.

* Initialise the HMKit with a certificate from the Developer Center. The flow is described in the
comments of the [BaseActivity.java](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/ble-explorer-app/src/main/java/com/highmobility/exploreautoapis/BaseActivity.java#L33)

* Run [BaseActivity.java](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/blob/master/ble-explorer-app/src/main/java/com/highmobility/exploreautoapis/BaseActivity.java#L21).

After initialisation, the [sandboxui](https://github.com/highmobility/hm-android-bluetooth-auto-api-explorer/tree/master/sandboxui/src/main/java/com/highmobility/sandboxui) module is loaded. All of the app logic is in that package.

## Instrumented tests

Instrumented tests cover the bluetooth and telematics commands. For them to work androidTest/res/values/credentials.xml needs to have the following keys:

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
