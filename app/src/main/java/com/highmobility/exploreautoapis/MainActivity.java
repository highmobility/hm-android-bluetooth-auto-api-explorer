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
                    "dGVzdOlUDp/iZdmzY3AthPYPev5SFk72I+/pKDMlvcYS9ksR6nS8xf3WoGmARSzAZsGsIkdvs56zzvGbwsmg+IV6Qkgh6U2WGXe4mGpoib8WcW/de2lPZ94EMazB0wppKQCNu7Q1yHPuTlPx6EwaT6ntlCz2oPmtspy9mO+U6hzg3eSzjptslG+MzfMTUcbImFsokoZpl39s",
                    "HRgynolx5zIfK+r9Xd/Js6DNdnvHtE/kc6j6T9V+zbQ=",
                    "HJS8Wh+Gjh2JRB8pMOmQdTMfVR7JoPLVF1U85xjSg7puYoTwLf+DO9Zs67jw+6pXmtkYxynMQm0rfcBU0XFF5A==",
                    getApplicationContext()
            );

            // PASTE ACCESS TOKEN HERE
            accessToken =
                    "O6kzNEYre8PNzjY8WaPRN9dTVXpOQr5Wbzy09UuimtS1-lEKpSKY5jSMkoPnZ-gauSNZp2IJp8mlL_5wLLnP-mvQ2wzR74Z-9rxQ_W7J6mCs_TCXmsbXN6NaSM_z4FiZfw";
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