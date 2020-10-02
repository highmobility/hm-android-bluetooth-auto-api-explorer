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
package com.highmobility.exploreautoapis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.hmkit.HMKit
import com.highmobility.hmkit.HMKit.DownloadCallback
import com.highmobility.hmkit.error.DownloadAccessCertificateError
import com.highmobility.sandboxui.controller.ConnectedVehicleController
import com.highmobility.sandboxui.view.ConnectedVehicleActivity
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

open class BaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        val accessToken = ""

        if (accessToken != "") {
            downloadAccessCertificate(accessToken)
        } else if (initialiseFromResources() == false) {
            onDownloadFailed("Initialisation values not set in BaseActivity.java")
        }
    }

    private fun downloadAccessCertificate(accessToken: String) {
        if (useCertificateFromStorage() == false) {
            HMKit.getInstance().downloadAccessCertificate(accessToken, object : DownloadCallback {
                override fun onDownloaded(serial: DeviceSerial) {
                    onCertificateDownloaded(serial, false)
                }

                override fun onDownloadFailed(error: DownloadAccessCertificateError) {
                    this@BaseActivity.onDownloadFailed(error.message)
                }
            })
        }
    }

    protected fun onDownloadFailed(message: String) {
        progressBar.visibility = View.GONE
        statusTextView.text = "Could not download the certificate:\n\n$message"
        Timber.d("Could not download the certificate with token: %s", message)
    }

    protected open fun onCertificateDownloaded(
        serial: DeviceSerial,
        useBle: Boolean
    ) {
        progressBar.visibility = View.GONE
        Timber.d("Certificate downloaded for vehicle: %s", serial)
        val i = Intent(this@BaseActivity, ConnectedVehicleActivity::class.java)
        i.putExtra(ConnectedVehicleController.EXTRA_SERIAL, serial.hex)
        i.putExtra(ConnectedVehicleController.EXTRA_USE_BLE, useBle)
        i.putExtra(ConnectedVehicleActivity.EXTRA_FINISH_ON_BACK_PRESS, false)
        startActivity(i)
        finish() // this activity is irrelevant now. SDK is initialised.
    }

    private val credentials: Credentials by lazy { Credentials(applicationContext) }

    private fun useCertificateFromStorage(): Boolean {
        val serialRes = credentials.getEnvironmentResource("vehicleSerial")
        if (serialRes != null) {
            val serial = DeviceSerial(serialRes)
            val cert = HMKit.getInstance().getCertificate(serial)
            if (cert != null) {
                onCertificateDownloaded(serial, false)
                return true
            }
        }

        return false
    }

    private fun initialiseFromResources(): Boolean {
        // initialise from string resources if keys exist

        if (HMKit.webUrl != null) {
            val accessToken = credentials.getEnvironmentResource("accessToken")
            downloadAccessCertificate(accessToken!!)
            return true
        }

        HMKit.webUrl = credentials.getEnvironmentResource("webUrl")
        if (HMKit.webUrl != null) {
            HMKit.getInstance().initialise(
                credentials.getEnvironmentResource("deviceCert"),
                credentials.getEnvironmentResource("privateKey"),
                credentials.getEnvironmentResource("issuerPublicKey"),
                applicationContext
            )
            val accessToken = credentials.getEnvironmentResource("accessToken")
            downloadAccessCertificate(accessToken!!)
            return true
        }

        return false
    }
}