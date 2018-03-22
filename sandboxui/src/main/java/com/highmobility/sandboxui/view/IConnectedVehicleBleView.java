package com.highmobility.sandboxui.view;

/**
 * Created by root on 24/05/2017.
 */

public interface IConnectedVehicleBleView extends IConnectedVehicleView {
    void showBleInfoView(boolean show, String status);
    void onLinkReceived(boolean received);
}
