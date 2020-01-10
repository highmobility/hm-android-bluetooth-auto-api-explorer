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
package com.highmobility.sandboxui.view;

import android.app.Activity;
import android.content.DialogInterface;

import com.highmobility.sandboxui.model.VehicleState;

public interface IConnectedVehicleView {
    void setViewState(ViewState viewState);

    void onCapabilitiesUpdate(VehicleState vehicle);

    void onVehicleStatusUpdate(VehicleState vehicle);

    void onError(boolean fatal, String message);

    void showAlert(String title, String message, String confirmTitle, String declineTitle,
                   DialogInterface.OnClickListener confirm, DialogInterface.OnClickListener
                           decline);

    Activity getActivity();

    enum ViewState {
        DOWNLOADING_CERT,
        FAILED_TO_DOWNLOAD_CERT,
        BROADCASTING,
        CONNECTED,
        AUTHENTICATING,
        AUTHENTICATED,
        AUTHENTICATED_LOADING
    }
}
