package com.highmobility.sandboxui.view;

import android.app.Activity;
import android.content.DialogInterface;

import com.highmobility.sandboxui.model.VehicleStatus;

public interface IConnectedVehicleView {
    void showLoadingView(boolean loading);

    void onCapabilitiesUpdate(VehicleStatus vehicle);

    void onVehicleStatusUpdate(VehicleStatus vehicle);

    void onError(boolean fatal, String message);

    void showAlert(String title, String message, String confirmTitle, String declineTitle,
                   DialogInterface.OnClickListener confirm, DialogInterface.OnClickListener
                           decline);

    Activity getActivity();

}
