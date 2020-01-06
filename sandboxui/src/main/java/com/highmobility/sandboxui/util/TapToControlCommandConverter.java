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
package com.highmobility.sandboxui.util;

import android.os.CountDownTimer;

import java.util.Calendar;

public class TapToControlCommandConverter {
    int latestIndex = -1;

    Long lastCommandTime = Long.MAX_VALUE;
    int tapsInSameIndex = 0;
    int speed;
    int angle;

    CountDownTimer timeoutTimer;
    ITapToControlCommandConverter listener;

    public TapToControlCommandConverter(ITapToControlCommandConverter listener) {
        this.listener = listener;
    }

    public int getSpeed() {
        return speed;
    }

    public int getAngle() {
        return angle;
    }

    public void onMoveButtonClicked(int index) {
        if (Calendar.getInstance().getTimeInMillis() - lastCommandTime > 300) tapsInSameIndex = 0;

        lastCommandTime = Calendar.getInstance().getTimeInMillis();

        if (latestIndex == index) {
            tapsInSameIndex++;
        }
        else {
            tapsInSameIndex = 0;
        }

        if ((latestIndex < 3 && index > 2) || (latestIndex > 2 && index < 3)) {
            // opposite direction tap
            speed = 0;
            listener.onSpeedChanged(0);
        }

        // observe the angle change
        switch (index) {
            case 0:
            case 3: {
                if (angle != 100) {
                    angle = 100;
                    listener.onAngleChanged(angle);
                }
                break;
            }
            case 1:
            case 4: {
                if (angle != 0) {
                    angle = 0;
                    listener.onAngleChanged(angle);
                }
                break;
            }
            case 2:
            case 5: {
                if (angle != -100) {
                    angle = -100;
                    listener.onAngleChanged(angle);
                }
                break;
            }
        }

        latestIndex = index;

        // if 3 taps have been in the same direction after correct interval, change the speed
        if (tapsInSameIndex > 2) {
            switch (latestIndex) {
                case 0:
                case 1:
                case 2:
                    if (speed != 1) listener.onSpeedChanged(1);
                    speed = 1;
                    break;
                case 3:
                case 4:
                case 5:
                    if (speed != -1) listener.onSpeedChanged(-1);
                    speed = -1;
                    break;
            }
        }

        if (timeoutTimer != null) timeoutTimer.cancel();

        // reset the speed and tap count if no tap after 310ms
        timeoutTimer = new CountDownTimer(310, 300) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (Calendar.getInstance().getTimeInMillis() - lastCommandTime > 300) {
                    tapsInSameIndex = 0;
                    if (speed != 0) {
                        speed = 0;
                        listener.onSpeedChanged(0);
                    }
                }
            }
        }.start();
    }

    public void onStopClicked() {
        tapsInSameIndex = 0;
        lastCommandTime = Long.MAX_VALUE;
        speed = 0;
        if (timeoutTimer != null) timeoutTimer.cancel();
        listener.onSpeedChanged(0);
    }
}
