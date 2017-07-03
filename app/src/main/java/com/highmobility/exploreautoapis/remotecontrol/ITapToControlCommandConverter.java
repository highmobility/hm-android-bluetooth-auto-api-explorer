package com.highmobility.exploreautoapis.remotecontrol;

/**
 * Created by ttiganik on 12/10/2016.
 */

public interface ITapToControlCommandConverter {
    void onSpeedChanged(int speed);
    void onAngleChanged(int angle);
}
