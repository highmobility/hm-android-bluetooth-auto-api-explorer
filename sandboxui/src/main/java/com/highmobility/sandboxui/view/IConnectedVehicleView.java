package com.highmobility.sandboxui.view;

import android.app.Activity;

import com.highmobility.sandboxui.model.VehicleStatus;

public interface IConnectedVehicleView {
    void showLoadingView(boolean loading);
    void onCapabilitiesUpdate(VehicleStatus vehicle);
    void onVehicleStatusUpdate(VehicleStatus vehicle);
    void onError(boolean fatal, String message);
    Activity getActivity();
}
