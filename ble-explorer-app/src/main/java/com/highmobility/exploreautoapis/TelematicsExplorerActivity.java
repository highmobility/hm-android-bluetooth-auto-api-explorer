/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

import static android.view.View.GONE;
import static timber.log.Timber.d;

public class TelematicsExplorerActivity extends Activity {
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
        // xytt, serial EE4A67ED4B6A4BDA28
        HMKit.webUrl = "https://sandbox.api.develop.high-mobility.net";
        HMKit.getInstance().initialise(
                "dGVzdKO0RDv7v7OkEoEdVMKtpAFXTBXLIVpwh+uteHP8UhWfRenOY1Qaphqh5t263riEdLxJ0gdlb3LoVY1Kg3+UkNh2OqQfXR9uu459DLS1RB/6jj3qsp9Uqs2ZFqYYlVwtOslErZNhZsKuPz2S+kWyuA3qImuQ0PBUZgfGV0nrG4pYaqyxoOM6QtaLhl8Bpo2ASGIXEaoG",
                "4cgd48pTp7Qm4IyyWNPiwpOuSeJC+Bw7cWiVjEXk83s=",
                "xmwpNk2LirIEQ2Qq3BtH0W+FfSmjEWZJ6Wd8wBvyHQH06t+EiqAjGLDaqCZCVzcMZrl4AxPBQVZYC9pruLqa5Q==",
                getApplicationContext()
        );

        String accessToken ="d26a5335-d8ae-42ef-b26c-f90edc24fd16";

        HMKit.getInstance().downloadAccessCertificate(accessToken, new HMKit.DownloadCallback() {
            @Override
            public void onDownloaded(DeviceSerial serial) {
                onCertificateDownloaded(serial);
            }

            @Override
            public void onDownloadFailed(DownloadAccessCertificateError error) {
                TelematicsExplorerActivity.this.onDownloadFailed(error.getMessage());
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
        Intent i = new Intent(TelematicsExplorerActivity.this, ConnectedVehicleActivity
                .class);
        i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial.getHex());
        i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, false);
        i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false);
        startActivity(i);
        TelematicsExplorerActivity.this.finish(); // this activity is irrelevant now. SDK is initialised.
    }
}