package com.highmobility.exploreautoapis;

import android.app.Activity;

import com.highmobility.exploreautoapis.storage.VehicleStatus;

/**
 * Created by root on 24/05/2017.
 */

public interface IVehicleView {
    void showLoadingView(boolean loading);
    void onCapabilitiesUpdate(VehicleStatus vehicle);
    void onVehicleStatusUpdate(VehicleStatus vehicle);
    void onError(boolean fatal, String message);
    void showBleInfoView(boolean show, String status);
    Activity getActivity();
}
