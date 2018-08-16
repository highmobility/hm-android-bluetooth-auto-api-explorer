package com.highmobility.sandboxui.controller;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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