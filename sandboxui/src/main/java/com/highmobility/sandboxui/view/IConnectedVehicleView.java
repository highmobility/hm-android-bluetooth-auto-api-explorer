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
