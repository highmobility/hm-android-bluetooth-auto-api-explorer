package com.highmobility.exploreautoapis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.highmobility.exploreautoapis.storage.VehicleStatus;
import com.rd.PageIndicatorView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class VehicleActivity extends FragmentActivity implements IVehicleView {
    public static final String TAG = "HMKit Reference App";
    public static final String EXTRA_VEHICLE_ID = "EXTRA_VEHICLE_ID";
    static final int REQUEST_CODE_REMOTE_CONTROL = 12;

    @BindView(R.id.page_indicator) PageIndicatorView pageIndicatorView;
    @BindView(R.id.view_pager) ViewPager viewPager;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.looking_for_links) TextView bleStatusTextView;

    VehicleOverviewFragment overviewFragment;
    VehicleExteriorFragment exteriorFragment;
    VehicleController controller;

    public void onRemoteControlClicked() {
        controller.startRemoteControl();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle);
        ButterKnife.bind(this);

        controller = new VehicleController(this);

        viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager(), controller.vehicle));
        title.setText("Explore AutoAPIs");
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                pageIndicatorView.setSelection(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_REMOTE_CONTROL) {
            controller.onReturnFromRemoteControl();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.onDestroy();
    }

    @Override
    public void showBleInfoView(boolean show, String status) {
        showNormalView(!show);
        bleStatusTextView.setVisibility(show ? VISIBLE : GONE);
        bleStatusTextView.setText(status);
    }

    void showNormalView(boolean show) {
        if (show) {
            pageIndicatorView.setCount(viewPager.getAdapter().getCount());
            viewPager.animate().alpha(1f).setDuration(200).setListener(null);
        }
        else {
            pageIndicatorView.setCount(0);
            viewPager.animate().alpha(0f).setDuration(200).setListener(null);
        }
    }

    @Override
    public void showLoadingView(boolean loading) {
        showNormalView(!loading);
        progressBar.setVisibility(loading ? VISIBLE : GONE);
    }

    @Override
    public void onCapabilitiesUpdate(VehicleStatus vehicle) {

    }

    @Override
    public void onVehicleStatusUpdate(VehicleStatus vehicle) {
        overviewFragment.onVehicleStatusUpdate();
        exteriorFragment.onVehicleStatusUpdate();
    }

    @Override
    public void onError(boolean fatal, String message) {
        Log.e("", "onError: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        if (fatal) {

        }
        else {
            overviewFragment.onVehicleStatusUpdate();
            exteriorFragment.onVehicleStatusUpdate();
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        VehicleStatus vehicle;
        public PagerAdapter(FragmentManager fragmentManager, VehicleStatus vehicle) {
            super(fragmentManager);
            this.vehicle = vehicle;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    overviewFragment = VehicleOverviewFragment.newInstance(vehicle, VehicleActivity.this);
                    return overviewFragment;
                case 1:
                    exteriorFragment = VehicleExteriorFragment.newInstance(vehicle, VehicleActivity.this);
                    return exteriorFragment;
                case 2:
                    return VehicleInteriorFragment.newInstance(vehicle, VehicleActivity.this);
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "OVERVIEW";
                case 1:
                    return "EXTERIOR";
                case 2:
                    return "INTERIOR";
                default:
                    return null;
            }
        }
    }
}
