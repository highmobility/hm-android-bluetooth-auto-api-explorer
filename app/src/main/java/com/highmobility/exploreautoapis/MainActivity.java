package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.highmobility.crypto.AccessCertificate;
import com.highmobility.hmkit.Manager;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;
import com.highmobility.sandboxui.controller.ConnectedVehicleController;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.value.DeviceSerial;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.status_text_view) TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // ruptela maidu test 2
        /*Manager.environment = Manager.Environment.STAGING;
        Manager.getInstance().initialize(
                "dGVzdDqCyqfi9UWaGs+ta7jIegqxVX+sIy3S+DULxj2Cs4FI5oomQmsjEd+6icfGudGw1vs1NfIkmLocK/zi44tVwPH6X/GyoQih45l2/R8MqB8PTqRZB6DOi4b+IN9cltHCgAjsxdmKfAhuj+HkOGep0qQ+T+hOfJ0YVfL5r4VmeO0H1ueUrxT6aBmAupN4xNruZdAgVVRY",
                "uew/c8a9b3T5B+O1p2lQkDqNkOJxHWK5EvoQwbdthsY=",
                "P1myzyqRK3oERcM2QLlhoK+B3BIfN+l61zo9wGQj/T8ARhS9ue3q+5HAiYNZySBN85P3J+hlJEm2T1fNdJfBwQ==",
                getApplicationContext()
        );*/

        // mission e
        Manager.getInstance().initialize(
                "dGVzdLnVeFXsIJTMMDWwwF7qX/RAYcXdpTzQFbxNYs0vJcq8RpyN1HbA5PCTqJ8CI2urU8PO1YvV1mP4nJuEqHQpq9Dzl0UJiGglp3a3uBqXVTGy0+LwQ0MROMNAYh+Tdp2yIqvU6Uy5yboLcrHLLUHDZEguiGEnVP0pNH+uCaHca4/CiNnmKEm67pZqXtnDDH0NHqP2LEsi",
                "CDh9oEK5koiw/4VhUT16FEeB6Z+6TRw9mup2aGoYlCM=",
                "K5mVFoq2rqKwAttWdIyPhwgVL80FNxkkNpgr/ca+ueq3JFn5iMLAMTJOKzG26qwtqrLO+z2sxxdwWNaItdBUWg==",
                getApplicationContext()
        );


        // PASTE ACCESS TOKEN HERE

        AccessCertificate cert = Manager.getInstance().getCertificate(new DeviceSerial
                ("0123B910A8108096EE"));

        if (cert != null) {
            onCertDownloaded(cert.getGainerSerial());
        } else {
            // PASTE ACCESS TOKEN HERE
            String accessToken = "";
            // ruptela
//            accessToken = "GiGT3hcUVZZ11r_U6BeGDrZVS_QUqYSQnrE-5gflpmzBMXpFsbzsXaeEovTdUSZwxAiQkHAGBPgkraQ6HxfG5ZWUCTfaGoQh_6ChthOOceF4yMJgkAvRzWgfjssIMldI2Q";

            // mission e
            accessToken = "Rp1wTWvW79qKE6iwGpYBimM12y-Z_Y5L6oAc2ytoyBc7S6leh88a8kQbCpPDsft7bAU3DNea02FQsJQWKJGB2zU79oTedzkWTIhqu6fv9jamQta9952aWEheHbYJ-xQ8Ng";

            Manager.getInstance().downloadCertificate(accessToken, new
                    Manager.DownloadCallback() {
                        @Override
                        public void onDownloaded(DeviceSerial serial) {
                            onCertDownloaded(serial);
                        }

                        @Override
                        public void onDownloadFailed(DownloadAccessCertificateError error) {
                            progressBar.setVisibility(GONE);
                            statusTextView.setText("Could not download the certificate:\n\n" + error
                                    .getMessage());
                            Log.d(TAG, "Could not download a certificate with token: " + error
                                    .getMessage());
                        }
                    });

        }
    }

    private void onCertDownloaded(DeviceSerial serial) {
        progressBar.setVisibility(GONE);
        Log.d(TAG, "Certificate downloaded for vehicle: " + serial);
        Intent i = new Intent(MainActivity.this, ConnectedVehicleActivity
                .class);
        i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial.getByteArray());
        i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, true);
        i.putExtra(ConnectedVehicleController.EXTRA_ALIVE_PING_AMOUNT_NAME,
                500);
        i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false);
        startActivity(i);
        MainActivity.this.finish(); // this activity is irrelevant now. SDK is
        // initialized.
    }
}