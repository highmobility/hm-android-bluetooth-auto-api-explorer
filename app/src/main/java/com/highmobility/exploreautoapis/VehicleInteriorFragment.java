package com.highmobility.exploreautoapis;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.highmobility.exploreautoapis.storage.VehicleStatus;

public class VehicleInteriorFragment extends Fragment {
    VehicleStatus vehicle;
    VehicleActivity parent;

    public VehicleInteriorFragment() {
        // Required empty public constructor
    }

    public static VehicleInteriorFragment newInstance(VehicleStatus vehicle, VehicleActivity vehicleActivity) {
        VehicleInteriorFragment fragment = new VehicleInteriorFragment();
        fragment.vehicle = vehicle;
        fragment.parent = vehicleActivity;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vehicle_interior, container, false);
    }
}
