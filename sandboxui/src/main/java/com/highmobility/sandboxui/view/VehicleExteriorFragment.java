package com.highmobility.sandboxui.view;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.model.ExteriorListItem;
import info.hoang8f.android.segmented.SegmentedGroup;

public class VehicleExteriorFragment extends Fragment {
    ListView listView;
    Adapter listViewAdapter;
    ConnectedVehicleActivity parent;

    public static VehicleExteriorFragment newInstance(ConnectedVehicleActivity
                                                              connectedVehicleActivity) {
        VehicleExteriorFragment fragment = new VehicleExteriorFragment();
        fragment.parent = connectedVehicleActivity;
        return fragment;
    }

    public VehicleExteriorFragment() {
    }

    public void onVehicleStatusUpdate(ExteriorListItem[] items) {
        if (listViewAdapter == null) {
            createAdapter(items);
        } else {
            listViewAdapter.items = items;
            listViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_vehicle_exterior, container, false);
        listView = v.findViewById(R.id.vehicle_exterior_list_view);
        return v;
    }

    private void createAdapter(ExteriorListItem[] items) {
        listViewAdapter = new Adapter(getContext(), items);

        listView.setAdapter(listViewAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (((ExteriorListItem) listViewAdapter.getItem(position)).type ==
                    ExteriorListItem.Type.REMOTE_CONTROL) {
                VehicleExteriorFragment.this.parent.onRemoteControlClicked();
            }
        });
    }

    class Adapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        public ExteriorListItem[] items;

        public Adapter(Context context, ExteriorListItem[] items) {
            this.context = context;
            this.items = items;
            inflater = (LayoutInflater) this.context.getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            ExteriorListItem item = items[position];

            if (item.type == ExteriorListItem.Type.REMOTE_CONTROL) {
                convertView = inflater.inflate(R.layout.list_item_exterior_remote_control, null,
                        false);
            } else if (item.type == ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE) {
                convertView = inflater.inflate(R.layout.list_item_exterior_item_three_segments,
                        null, false);
                ImageView image = convertView.findViewById(R.id.icon);
                TextView title = convertView.findViewById(R.id.title);
                TextView stateTitle = convertView.findViewById(R.id.state_title);
                SegmentedGroup segmentedGroup = convertView.findViewById(R.id
                        .segment_group);

                image.setImageResource(item.iconResId);
                title.setText(item.title);

                if (item.actionSupported) {
                    RadioButton firstButton = convertView.findViewById(R.id
                            .first_button);
                    RadioButton secondButton = convertView.findViewById(R.id
                            .second_button);
                    RadioButton thirdButton = convertView.findViewById(R.id
                            .third_button);

                    firstButton.setText(item.segmentTitles[0]);
                    secondButton.setText(item.segmentTitles[1]);
                    thirdButton.setText(item.segmentTitles[2]);

                    if (item.selectedSegment == 0) {
                        firstButton.toggle();
                        secondButton.setOnClickListener(view -> {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(1);
                        });
                        thirdButton.setOnClickListener(view -> {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(2);
                        });
                    } else if (item.selectedSegment == 1) {
                        secondButton.toggle();
                        firstButton.setOnClickListener(view -> {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(0);
                        });
                        thirdButton.setOnClickListener(view -> {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(2);
                        });
                    } else {
                        thirdButton.toggle();
                        firstButton.setOnClickListener(view -> {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(0);
                        });
                        secondButton.setOnClickListener(view -> {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(1);
                        });
                    }
                } else {
                    segmentedGroup.setVisibility(View.GONE);
                    stateTitle.setVisibility(View.VISIBLE);
                    stateTitle.setText(item.stateTitle);
                }
            } else {
                convertView = inflater.inflate(R.layout.list_item_exterior_item, null, false);
                ImageView image = convertView.findViewById(R.id.icon);
                TextView title = convertView.findViewById(R.id.title);
                TextView stateTitle = convertView.findViewById(R.id.state_title);
                SegmentedGroup segmentedGroup = convertView.findViewById(R.id
                        .segment_group);

                image.setImageResource(item.iconResId);
                title.setText(item.title);

                if (item.actionSupported) {
                    RadioButton firstButton = convertView.findViewById(R.id
                            .first_button);
                    RadioButton secondButton = convertView.findViewById(R.id
                            .second_button);
                    firstButton.setText(item.segmentTitles[0]);
                    secondButton.setText(item.segmentTitles[1]);
                    if (item.selectedSegment == 0) {
                        firstButton.toggle();
                        secondButton.setOnClickListener(view -> onTwoButtonSegmentClick(item));
                    } else {
                        secondButton.toggle();
                        firstButton.setOnClickListener(view -> onTwoButtonSegmentClick(item));
                    }
                } else {
                    segmentedGroup.setVisibility(View.GONE);
                    stateTitle.setVisibility(View.VISIBLE);
                    stateTitle.setText(item.stateTitle);
                }
            }

            return convertView;
        }

        private void onTwoButtonSegmentClick(ExteriorListItem item) {
            switch (item.type) {
                case DOORS_LOCKED:
                    VehicleExteriorFragment.this.parent.controller
                            .onLockDoorsClicked();
                    break;
                case IS_WINDSHIELD_DEFROSTING_ACTIVE:
                    VehicleExteriorFragment.this.parent.controller
                            .onWindshieldDefrostingClicked();
                    break;
                case ROOFTOP_DIMMING_PERCENTAGE:
                    VehicleExteriorFragment.this.parent.controller
                            .onSunroofVisibilityClicked();
                    break;
                case ROOFTOP_POSITION:
                    VehicleExteriorFragment.this.parent.controller
                            .onSunroofOpenClicked();
                    break;
                case TRUNK_LOCK_STATE:
                    VehicleExteriorFragment.this.parent.controller
                            .onLockTrunkClicked();
                    break;
            }
        }
    }
}
