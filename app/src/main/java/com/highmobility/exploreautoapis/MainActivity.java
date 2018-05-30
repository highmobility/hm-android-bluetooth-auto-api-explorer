package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.highmobility.crypto.AccessCertificate;
import com.highmobility.hmkit.Error.DownloadAccessCertificateError;
import com.highmobility.hmkit.Manager;
import com.highmobility.sandboxui.controller.ConnectedVehicleController;
import com.highmobility.sandboxui.view.ConnectedVehicleActivity;
import com.highmobility.utils.Bytes;

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
        Manager.environment = Manager.Environment.STAGING;
        Manager.getInstance().initialize(
                "dGVzdBIbNgrN/I39pquS1JyKhrOYHvPMrzcEz0j9grNYsmYzofuh1ZHAmTYdRMs6HHWChkoYDuwSL20PqQ4MlESOlTORL0CDDZ+ZD3YlY3JOwrqHVF75es7/h/OtPGMVB8jRi1pkeIYKsV/TZGGy7BuwLEgKWUs95Cixuo+/mzvVTvlhSBVA7BuPnGoF5Z3KBlYiWsnK4OOs",
                "2+Z3ggUcFK+MqO3V/gAVhlSQO36fJnjvtQn5AqKylY4=",
                "P1myzyqRK3oERcM2QLlhoK+B3BIfN+l61zo9wGQj/T8ARhS9ue3q+5HAiYNZySBN85P3J" +
                        "+hlJEm2T1fNdJfBwQ==",
                getApplicationContext()
        );

        AccessCertificate cert = Manager.getInstance().getCertificate(Bytes.bytesFromHex("0123B910A8108096EE"));

        // PASTE ACCESS TOKEN HERE
        if (cert != null) {
            onCertDownloaded(cert.getGainerSerial());
        } else {
            String accessToken = "aa5fac7f-bbef-41ab-905f-706b4b038e7a";
            Manager.getInstance().downloadCertificate(accessToken, new
                    Manager.DownloadCallback() {
                        @Override
                        public void onDownloaded(byte[] serial) {
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

    private void onCertDownloaded(byte[] serial) {
        progressBar.setVisibility(GONE);
        Log.d(TAG, "Certificate downloaded for vehicle: " + serial);
        Intent i = new Intent(MainActivity.this, ConnectedVehicleActivity
                .class);
        i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial);
        i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, true);
        i.putExtra(ConnectedVehicleController.EXTRA_ALIVE_PING_AMOUNT_NAME,
                500);
        i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false);
        startActivity(i);
        MainActivity.this.finish(); // this activity is irrelevant now. SDK is
        // initialized.
    }
}