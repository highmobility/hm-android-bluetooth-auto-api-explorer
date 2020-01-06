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
package com.highmobility.sandboxui.controller;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.highmobility.sandboxui.R;

public class BroadcastFragment extends Fragment {
    public interface OnFragmentInteractionListener {
        void onBroadcastSerialSwitchChanged(boolean on);
    }

    private OnFragmentInteractionListener listener;

    TextView bleStatusTextView;
    LinearLayout broadcastSerialView;
    Switch broadcastingSerialSwitch;

    public BroadcastFragment() {
        // Required empty public constructor
    }

    public void setStatusText(String text) {
        bleStatusTextView.setText(text);
    }

    public void onLinkReceived(boolean received) {
        broadcastSerialView.setVisibility(received ? View.GONE : View.VISIBLE);
    }

    public void onBroadcastingSerial(boolean broadcasting) {
        broadcastingSerialSwitch.setChecked(broadcasting);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_broadcast, container, false);
        bleStatusTextView = view.findViewById(R.id.looking_for_links);
        broadcastSerialView = view.findViewById(R.id.broadcast_serial_view);
        broadcastingSerialSwitch = view.findViewById(R.id.broadcasting_serial_switch);

        broadcastingSerialSwitch.setOnCheckedChangeListener((compoundButton, b) -> listener
                .onBroadcastSerialSwitchChanged(b));

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}