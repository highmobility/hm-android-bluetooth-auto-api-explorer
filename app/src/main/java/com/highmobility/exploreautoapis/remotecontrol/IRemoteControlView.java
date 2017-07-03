package com.highmobility.exploreautoapis.remotecontrol;

import android.app.Activity;

/**
 * Created by ttiganik on 04/10/2016.
 */

public interface IRemoteControlView {
    void showLoadingView(boolean show);
    void showStopButton(boolean show);
    Activity getActivity();
}
