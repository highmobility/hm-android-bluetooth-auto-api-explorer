package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Manager;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;
import com.highmobility.sandboxui.controller.ConnectedVehicleController;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;

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

        /*
         * Before using HMKit, you'll have to initialise the Manager singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   Manager.getInstance().initialize(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         *
         *   Access token is also required for downloading the access certificate.
         */

        // PASTE ACCESS TOKEN HERE
        String accessToken = "";

        try {
            Manager.getInstance().initialize(
                    "dGVzdLnVeFXsIJTMMDWwwF7qX" +
                            "/RAYcXdpTzQFbxNYs0vJcq8RpyN1HbA5PCTqJ8CI2urU8PO1YvV1mP4nJuEqHQpq9Dzl0UJiGglp3a3uBqXVTGy0+LwQ0MROMNAYh+Tdp2yIqvU6Uy5yboLcrHLLUHDZEguiGEnVP0pNH+uCaHca4/CiNnmKEm67pZqXtnDDH0NHqP2LEsi",

                    "CDh9oEK5koiw/4VhUT16FEeB6Z+6TRw9mup2aGoYlCM=",
                    "K5mVFoq2rqKwAttWdIyPhwgVL80FNxkkNpgr/ca+ueq3JFn5iMLAMTJOKzG26qwtqrLO" +
                            "+z2sxxdwWNaItdBUWg==",
                    getApplicationContext()
            );

            // PASTE ACCESS TOKEN HERE
            accessToken =
                    "Rp1wTWvW79qKE6iwGpYBimM12y" +
                            "-Z_Y5L6oAc2ytoyBc7S6leh88a8kQbCpPDsft7bAU3DNea02FQsJQWKJGB2zU79oTedzkWTIhqu6fv9jamQta9952aWEheHbYJ-xQ8Ng";
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }

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

    private void onCertDownloaded(DeviceSerial serial) {
        progressBar.setVisibility(GONE);
        Log.d(TAG, "Certificate downloaded for vehicle: " + serial);
        Intent i = new Intent(MainActivity.this, ConnectedVehicleActivity
                .class);
        i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial.getByteArray());
        i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, true);
        i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false);
        startActivity(i);
        MainActivity.this.finish(); // this activity is irrelevant now. SDK is
        // initialized.
    }
}