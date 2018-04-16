package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

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
         */

        try {
            // PASTE THE SNIPPET HERE

            //Manager.getInstance().initialize(
            //        "...",
            //        "...",
            //        "...",
            //        getApplicationContext()
            //);

            // PASTE ACCESS TOKEN HERE
            String accessToken = "";

            Manager.getInstance().downloadCertificate(accessToken, new Manager.DownloadCallback() {
                @Override
                public void onDownloaded(byte[] serial) {
                    progressBar.setVisibility(GONE);

                    Log.d(TAG, "Certificate downloaded for vehicle: " + Bytes.hexFromBytes
                            (serial));
                    Intent i = new Intent(MainActivity.this, ConnectedVehicleActivity.class);
                    i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial);
                    i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, true);
                    i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false);
                    startActivity(i);
                    MainActivity.this.finish(); // this activity is irrelevant now. SDK is initialized.
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
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
            progressBar.setVisibility(GONE);
            statusTextView.setText(e.getMessage());
        }
    }
}