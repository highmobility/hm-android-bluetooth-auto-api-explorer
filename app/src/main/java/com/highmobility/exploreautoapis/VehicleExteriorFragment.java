/*
 * HMKit Auto API - Auto API Parser for Java
 * Copyright (C) 2018 High-Mobility <licensing@high-mobility.com>
 *
 * This file is part of HMKit Auto API.
 *
 * HMKit Auto API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HMKit Auto API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HMKit Auto API.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.highmobility.exploreautoapis;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.Identifier;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.OpenCloseTrunk;
import com.highmobility.autoapi.property.CapabilityProperty;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.exploreautoapis.storage.VehicleStatus;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.hoang8f.android.segmented.SegmentedGroup;

public class VehicleExteriorFragment extends Fragment {
    @BindView(R.id.list_view) ListView listView;
    Adapter listViewAdapter;
    VehicleActivity parent;
    VehicleStatus vehicle;

    public static VehicleExteriorFragment newInstance(VehicleStatus vehicle, VehicleActivity
            vehicleActivity) {
        VehicleExteriorFragment fragment = new VehicleExteriorFragment();
        fragment.setVehicle(vehicle);
        fragment.parent = vehicleActivity;
        return fragment;
    }

    public VehicleExteriorFragment() {
    }

    public void setVehicle(VehicleStatus vehicle) {
        this.vehicle = vehicle;
    }

    public void onVehicleStatusUpdate() {
        if (listViewAdapter == null) {
            createAdapter();
        } else {
            listViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_vehicle_exterior, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    private void createAdapter() {
        listViewAdapter = new Adapter(getContext(), vehicle);

        listView.setAdapter(listViewAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((CapabilityProperty) listViewAdapter.getItem(position)).getIdentifier() ==
                        Identifier.REMOTE_CONTROL) {
                    VehicleExteriorFragment.this.parent.onRemoteControlClicked();
                }
            }
        });
    }

    class Adapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        public VehicleStatus vehicle;

        public Adapter(Context context, VehicleStatus vehicle) {
            this.context = context;
            this.vehicle = vehicle;
            inflater = (LayoutInflater) this.context.getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return vehicle.exteriorCapabilities.length;
        }

        @Override
        public Object getItem(int position) {
            return vehicle.exteriorCapabilities[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            CapabilityProperty capability = vehicle.exteriorCapabilities[position];

            if (capability.getIdentifier() == Identifier.REMOTE_CONTROL) {
                convertView = inflater.inflate(R.layout.list_item_exterior_remote_control, null,
                        false);
            } else if (capability.getIdentifier() == Identifier.ROOFTOP) {
                convertView = inflater.inflate(R.layout.list_item_two_exterior_items, null, false);

                ImageView image = (ImageView) convertView.findViewById(R.id.icon);
                ImageView imageTwo = (ImageView) convertView.findViewById(R.id.icon2);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                TextView titleTwo = (TextView) convertView.findViewById(R.id.title2);

                title.setText("ROOFTOP DIMMING");
                titleTwo.setText("ROOFTOP OPENING");

                // control
                if (capability.isSupported(ControlRooftop.TYPE)) {
                    // show segment
                    RadioButton firstButton = (RadioButton) convertView.findViewById(R.id
                            .first_button);
                    RadioButton secondButton = (RadioButton) convertView.findViewById(R.id
                            .second_button);

                    firstButton.setText("TRANSPARENT");
                    secondButton.setText("OPAQUE");

                    if (vehicle.rooftopDimmingPercentage == 1f) {
                        image.setImageResource(R.drawable.ext_roof_opaque);

                        secondButton.toggle();

                        firstButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller
                                        .onSunroofVisibilityClicked();
                            }
                        });
                    } else {
                        image.setImageResource(R.drawable.ext_roof_transparent);
                        firstButton.toggle();

                        secondButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller
                                        .onSunroofVisibilityClicked();
                            }
                        });
                    }

                    RadioButton firstButton1 = (RadioButton) convertView.findViewById(R.id
                            .first_button2);
                    RadioButton secondButton1 = (RadioButton) convertView.findViewById(R.id
                            .second_button2);

                    if (vehicle.rooftopOpenPercentage == 0f) {
                        imageTwo.setImageResource(R.drawable.ext_rooftop_closed);
                        firstButton1.setText("OPEN");
                        secondButton1.setText("CLOSED");

                        secondButton1.toggle();
                        firstButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller
                                        .onSunroofOpenClicked();
                            }
                        });
                    } else {
                        imageTwo.setImageResource(R.drawable.ext_rooftop_open);
                        firstButton1.setText("OPEN");
                        secondButton1.setText("CLOSE");
                        firstButton.toggle();
                        secondButton1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller
                                        .onSunroofOpenClicked();
                            }
                        });
                    }
                } else {
                    TextView stateTitle = (TextView) convertView.findViewById(R.id.state_title);
                    SegmentedGroup segmentedGroup = (SegmentedGroup) convertView.findViewById(R
                            .id.segment_group);
                    segmentedGroup.setVisibility(View.GONE);
                    stateTitle.setVisibility(View.VISIBLE);
                    if (vehicle.rooftopDimmingPercentage == 1f) {
                        stateTitle.setText("OPAQUE");
                        image.setImageResource(R.drawable.ext_roof_opaque);
                    } else {
                        stateTitle.setText("TRANSPARENT");
                        image.setImageResource(R.drawable.ext_roof_transparent);
                    }

                    TextView stateTitle1 = (TextView) convertView.findViewById(R.id.state_title2);
                    SegmentedGroup segmentedGroup1 = (SegmentedGroup) convertView.findViewById(R
                            .id.segment_group2);
                    segmentedGroup1.setVisibility(View.GONE);
                    stateTitle1.setVisibility(View.VISIBLE);
                    if (vehicle.rooftopOpenPercentage == 0f) {
                        stateTitle1.setText("CLOSED");
                        imageTwo.setImageResource(R.drawable.ext_rooftop_closed);
                    } else {
                        stateTitle.setText("OPEN");
                        imageTwo.setImageResource(R.drawable.ext_rooftop_open);
                    }
                }
            } else if (capability.getIdentifier() == Identifier.LIGHTS) {
                convertView = inflater.inflate(R.layout.list_item_exterior_item_three_segments,
                        null, false);
                ImageView image = (ImageView) convertView.findViewById(R.id.icon);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText("FRONT LIGHTS");

                if (capability.isSupported(ControlLights.TYPE)) {
                    RadioButton firstButton = (RadioButton) convertView.findViewById(R.id
                            .first_button);
                    RadioButton secondButton = (RadioButton) convertView.findViewById(R.id
                            .second_button);
                    RadioButton thirdButton = (RadioButton) convertView.findViewById(R.id
                            .third_button);

                    firstButton.setText("INACTIVE");
                    secondButton.setText("ACTIVE");
                    thirdButton.setText("FULL BEAM");

                    if (vehicle.frontExteriorLightState == FrontExteriorLightState.INACTIVE) {
                        firstButton.toggle();
                        image.setImageResource(R.drawable.ext_front_lights_off);
                    } else if (vehicle.frontExteriorLightState == FrontExteriorLightState.ACTIVE) {
                        secondButton.toggle();
                        image.setImageResource(R.drawable.ext_front_lights_on);
                    } else if (vehicle.frontExteriorLightState == FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM) {
                        thirdButton.toggle();
                        image.setImageResource(R.drawable.ext_front_lights_full_beam);
                    }

                    firstButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(FrontExteriorLightState.INACTIVE);
                        }
                    });

                    secondButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(FrontExteriorLightState.ACTIVE);
                        }
                    });

                    thirdButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleExteriorFragment.this.parent.controller
                                    .onFrontExteriorLightClicked(FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM);
                        }
                    });
                } else {
                    SegmentedGroup segmentedGroup = (SegmentedGroup) convertView.findViewById(R
                            .id.segment_group);
                    segmentedGroup.setVisibility(View.GONE);
                    TextView stateTitle = (TextView) convertView.findViewById(R.id.state_title);
                    stateTitle.setVisibility(View.VISIBLE);

                    if (vehicle.frontExteriorLightState == FrontExteriorLightState.INACTIVE) {
                        image.setImageResource(R.drawable.ext_front_lights_off);
                        stateTitle.setText("INACTIVE");
                    } else if (vehicle.frontExteriorLightState == FrontExteriorLightState.ACTIVE) {
                        image.setImageResource(R.drawable.ext_front_lights_on);
                        stateTitle.setText("ACTIVE");
                    } else if (vehicle.frontExteriorLightState == FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM) {
                        image.setImageResource(R.drawable.ext_front_lights_full_beam);
                        stateTitle.setText("FULL BEAM");
                    }
                }
            } else {
                convertView = inflater.inflate(R.layout.list_item_exterior_item, null, false);
                ImageView image = (ImageView) convertView.findViewById(R.id.icon);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                RadioButton firstButton = (RadioButton) convertView.findViewById(R.id.first_button);
                RadioButton secondButton = (RadioButton) convertView.findViewById(R.id
                        .second_button);
                TextView stateTitle = (TextView) convertView.findViewById(R.id.state_title);
                SegmentedGroup segmentedGroup = (SegmentedGroup) convertView.findViewById(R.id
                        .segment_group);

                if (capability.getIdentifier() == Identifier.CLIMATE) {
                    title.setText("WINDSHIELD HEATING");

                    if (capability.isSupported(ControlLights.TYPE)) {
                        if (vehicle.isWindshieldDefrostingActive) {
                            image.setImageResource(R.drawable.ext_windshield_heating_on);
                            secondButton.toggle();
                            secondButton.setText("ACTIVE");
                            firstButton.setText("INACTIVATE");
                            firstButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller
                                            .onWindshieldDefrostingClicked();
                                }
                            });
                        } else {
                            image.setImageResource(R.drawable.ext_windshield_heating_off);
                            firstButton.toggle();
                            secondButton.setText("ACTIVATE");
                            firstButton.setText("INACTIVE");
                            secondButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller
                                            .onWindshieldDefrostingClicked();
                                }
                            });
                        }
                    } else {
                        segmentedGroup.setVisibility(View.GONE);
                        stateTitle.setVisibility(View.VISIBLE);

                        if (vehicle.isWindshieldDefrostingActive) {
                            image.setImageResource(R.drawable.ext_windshield_heating_on);
                            stateTitle.setText("ACTIVE");
                        } else {
                            image.setImageResource(R.drawable.ext_windshield_heating_off);
                            stateTitle.setText("INACTIVE");
                        }

                    }
                } else if (capability.getIdentifier() == Identifier.DOOR_LOCKS) {
                    title.setText("DOOR LOCKS");

                    if (capability.isSupported(LockUnlockDoors.TYPE)) {
                        if (vehicle.doorsLocked == true) {
                            image.setImageResource(R.drawable.ext_doors_locked);
                            firstButton.setText("UNLOCK");
                            secondButton.setText("LOCKED");
                            secondButton.toggle();
                            firstButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller
                                            .onLockDoorsClicked();
                                }
                            });
                        } else {
                            image.setImageResource(R.drawable.ext_doors_unlocked);
                            firstButton.setText("UNLOCKED");
                            secondButton.setText("LOCK");
                            firstButton.toggle();
                            secondButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller
                                            .onLockDoorsClicked();
                                }
                            });
                        }
                    } else {
                        segmentedGroup.setVisibility(View.GONE);
                        stateTitle.setVisibility(View.VISIBLE);
                        if (vehicle.doorsLocked == true) {
                            stateTitle.setText("LOCKED");
                            image.setImageResource(R.drawable.ext_doors_locked);
                        } else {
                            stateTitle.setText("UNLOCKED");
                            image.setImageResource(R.drawable.ext_doors_unlocked);
                        }
                    }
                } else if (capability.getIdentifier() == Identifier.TRUNK_ACCESS) {
                    title.setText("TRUNK LOCK");

                    if (capability.isSupported(OpenCloseTrunk.TYPE)) {
                        if (vehicle.trunkLockState == TrunkLockState.LOCKED) {
                            image.setImageResource(R.drawable.ext_trunk_closed);
                            firstButton.setText("UNLOCK");
                            secondButton.setText("LOCKED");
                            secondButton.toggle();

                            firstButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller
                                            .onLockTrunkClicked();
                                }
                            });
                        } else {
                            image.setImageResource(R.drawable.ext_trunk_open);
                            firstButton.setText("UNLOCKED");
                            secondButton.setText("LOCK");
                            firstButton.toggle();
                            secondButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller
                                            .onLockTrunkClicked();
                                }
                            });
                        }
                    } else {
                        segmentedGroup.setVisibility(View.GONE);
                        stateTitle.setVisibility(View.VISIBLE);

                        if (vehicle.trunkLockState == TrunkLockState.LOCKED) {
                            stateTitle.setText("LOCKED");
                            image.setImageResource(R.drawable.ext_trunk_closed);
                        } else {
                            stateTitle.setText("UNLOCKED");
                            image.setImageResource(R.drawable.ext_trunk_open);
                        }
                    }
                }
            }

            return convertView;
        }
    }
}
