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

//    byte[] vehicleSerial;
//    ConnectedLink link;

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

        // PASTE THE SNIPPET HERE
        try {
            Manager.getInstance().initialize(
                    "dGVzdLnVeFXsIJTMMDWwwF7qX/RAYcXdpTzQFbxNYs0vJcq8RpyN1HbA5PCTqJ8CI2urU8PO1YvV1mP4nJuEqHQpq9Dzl0UJiGglp3a3uBqXVTGy0+LwQ0MROMNAYh+Tdp2yIqvU6Uy5yboLcrHLLUHDZEguiGEnVP0pNH+uCaHca4/CiNnmKEm67pZqXtnDDH0NHqP2LEsi",

                    "CDh9oEK5koiw/4VhUT16FEeB6Z+6TRw9mup2aGoYlCM=",
                    "K5mVFoq2rqKwAttWdIyPhwgVL80FNxkkNpgr/ca+ueq3JFn5iMLAMTJOKzG26qwtqrLO" +
                            "+z2sxxdwWNaItdBUWg==",
                    getApplicationContext()
            );

            // PASTE ACCESS TOKEN HERE
            String accessToken =
                    "Rp1wTWvW79qKE6iwGpYBimM12y" +
                            "-Z_Y5L6oAc2ytoyBc7S6leh88a8kQbCpPDsft7bAU3DNea02FQsJQWKJGB2zU79oTedzkWTIhqu6fv9jamQta9952aWEheHbYJ-xQ8Ng";

            Manager.getInstance().downloadCertificate(accessToken, new Manager.DownloadCallback() {
                @Override
                public void onDownloaded(byte[] serial) {
                    progressBar.setVisibility(GONE);
//                    MainActivity.this.vehicleSerial = serial;
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
            progressBar.setVisibility(GONE);
            statusTextView.setText(e.getMessage());
        }

    }

//    @Override protected void onResume() {
//        super.onResume();
//
//        List<ConnectedLink> links = Manager.getInstance().getBroadcaster().getLinks();
//
//        for (ConnectedLink link : links) {
//            if (Arrays.equals(link.getSerial(), vehicleSerial)) {
//                this.link = link;
//                this.link.setListener(this);
//                break;
//            }
//        }
//    }
//
//    @Override public void onStateChanged(Link link, Link.State state) {
//        if (link.getState() == Link.State.DISCONNECTED) {
//            this.link = null;
//            this.link.setListener(null);
//        }
//    }
//
//    @Override public void onCommandReceived(Link link, byte[] bytes) {
//
//    }
}