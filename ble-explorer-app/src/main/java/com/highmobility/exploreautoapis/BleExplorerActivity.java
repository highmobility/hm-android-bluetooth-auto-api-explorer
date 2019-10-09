package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.HMKit;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;
import com.highmobility.sandboxui.controller.ConnectedVehicleController;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.view.View.GONE;
import static timber.log.Timber.d;

public class BleExplorerActivity extends Activity {
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.status_text_view) TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /*
         * Before using HMKit, you'll have to initialise the HMKit singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   HMKit.getInstance().initialise(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         *
         *   Access token is also required for downloading the access certificate.
         */

        // PASTE SNIPPET HERE

        // PASTE ACCESS TOKEN HERE
        String accessToken = "";

        HMKit.getInstance().downloadAccessCertificate(accessToken, new HMKit.DownloadCallback() {
            @Override
            public void onDownloaded(DeviceSerial serial) {
                onCertificateDownloaded(serial);
            }

            @Override
            public void onDownloadFailed(DownloadAccessCertificateError error) {
                BleExplorerActivity.this.onDownloadFailed(error.getMessage());
            }
        });
    }

    private void onDownloadFailed(String message) {
        progressBar.setVisibility(GONE);
        statusTextView.setText("Could not download the certificate:\n\n" + message);
        d("Could not download the certificate with token: %s", message);
    }

    private void onCertificateDownloaded(DeviceSerial serial) {
        progressBar.setVisibility(GONE);
        d("Certificate downloaded for vehicle: %s", serial);
        Intent i = new Intent(BleExplorerActivity.this, ConnectedVehicleActivity
                .class);
        i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial.getHex());
        i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, true);
        i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false);
        startActivity(i);
        BleExplorerActivity.this.finish(); // this activity is irrelevant now. SDK is initialised.
    }
}