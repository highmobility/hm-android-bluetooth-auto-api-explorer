package com.highmobility.sandboxui.model;

import android.content.res.Resources;

import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.ControlTrunk;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.sandboxui.R;

import java.util.ArrayList;

/**
 * Created by ttiganik on 15/03/2018.
 */
public class ExteriorListItem {
    public int segmentCount;
    public String title; // the cell title
    public int iconResId; // cell icon
    public boolean actionSupported;

    public String stateTitle; // the state title (if action not possible and segment not shown)
    public String[] segmentTitles; // the segment titles
    public int selectedSegment;

    public Type type;

    /**
     * Create the items that are displayed in exterior list view from VehicleStatus.
     */
    public static ExteriorListItem[] createExteriorListItems(Resources resources,
                                                             VehicleStatus vehicle) {
        ArrayList<ExteriorListItem> builder = new ArrayList<>();

        // create the items:
        if (vehicle.doorsLocked != null) {
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.DOORS_LOCKED;

            item.actionSupported = vehicle.isSupported(LockUnlockDoors.IDENTIFIER,
                    LockUnlockDoors.IDENTIFIER_INSIDE_LOCKS_STATE);
            item.title = "DOOR LOCKS";

            item.segmentCount = 2;
            item.segmentTitles = new String[2];

            if (vehicle.doorsLocked) {
                item.stateTitle = "LOCKED";
                item.iconResId = R.drawable.ext_doors_locked;
                item.segmentTitles[0] = "LOCKED";
                item.segmentTitles[1] = "UNLOCK";
                item.selectedSegment = 0;
            } else {
                item.stateTitle = "UNLOCKED";
                item.segmentTitles[0] = "LOCK";
                item.segmentTitles[1] = "UNLOCKED";
                item.iconResId = R.drawable.ext_doors_unlocked;
                item.selectedSegment = 1;
            }

            builder.add(item);
        }

        if (vehicle.trunkLockState != null) {
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.TRUNK_LOCK_STATE;
            item.actionSupported = vehicle.isSupported(ControlTrunk.IDENTIFIER,
                    ControlTrunk.IDENTIFIER_LOCK);
            item.title = "TRUNK LOCK";

            item.segmentCount = 2;
            item.segmentTitles = new String[2];

            if (vehicle.trunkLockState == LockState.LOCKED) {
                item.stateTitle = "LOCKED";
                item.segmentTitles[0] = "LOCKED";
                item.segmentTitles[1] = "UNLOCK";
                item.iconResId = R.drawable.ext_trunk_closed;
                item.selectedSegment = 0;
            } else {
                item.stateTitle = "UNLOCKED";
                item.segmentTitles[0] = "LOCK";
                item.segmentTitles[1] = "UNLOCKED";
                item.iconResId = R.drawable.ext_trunk_open;
                item.selectedSegment = 1;
            }
            builder.add(item);
        }

        if (vehicle.isWindshieldDefrostingActive != null) {
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.IS_WINDSHIELD_DEFROSTING_ACTIVE;
            item.actionSupported = vehicle.isSupported(StartStopDefrosting.IDENTIFIER,
                    StartStopDefrosting.IDENTIFIER_DEFROSTING_STATE);
            item.title = "WINDSHIELD HEATING";

            item.segmentCount = 2;
            item.segmentTitles = new String[2];

            if (vehicle.isWindshieldDefrostingActive) {
                item.stateTitle = "ACTIVE";
                item.segmentTitles[0] = "ACTIVE";
                item.segmentTitles[1] = "INACTIVATE";
                item.iconResId = R.drawable.ext_windshield_heating_on;
                item.selectedSegment = 0;
            } else {
                item.segmentTitles[0] = "ACTIVATE";
                item.segmentTitles[1] = "INACTIVE";
                item.stateTitle = "INACTIVE";
                item.iconResId = R.drawable.ext_windshield_heating_off;
                item.selectedSegment = 1;
            }

            builder.add(item);
        }

        if (vehicle.rooftopDimmingPercentage != null) {
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.ROOFTOP_DIMMING_PERCENTAGE;
            item.actionSupported = vehicle.isSupported(ControlRooftop.IDENTIFIER,
                    ControlRooftop.IDENTIFIER_DIMMING);
            item.title = "ROOFTOP DIMMING";

            item.segmentCount = 2;
            item.segmentTitles = new String[2];
            item.segmentTitles[0] = "OPAQUE";
            item.segmentTitles[1] = "TRANSPARENT";

            if (vehicle.rooftopDimmingPercentage == 1f) {
                item.stateTitle = "OPAQUE";
                item.iconResId = R.drawable.ext_roof_opaque;
                item.selectedSegment = 0;
            } else {
                item.stateTitle = "TRANSPARENT";
                item.iconResId = R.drawable.ext_roof_transparent;
                item.selectedSegment = 1;
            }

            builder.add(item);
        }

        if (vehicle.rooftopOpenPercentage != null) {
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.ROOFTOP_POSITION;
            item.actionSupported = vehicle.isSupported(ControlRooftop.IDENTIFIER,
                    ControlRooftop.IDENTIFIER_POSITION);
            item.title = "ROOFTOP OPENING";

            item.segmentCount = 2;
            item.segmentTitles = new String[2];

            if (vehicle.rooftopOpenPercentage == 1f) {
                item.stateTitle = "OPEN";
                item.iconResId = R.drawable.ext_rooftop_open;
                item.segmentTitles[0] = "OPEN";
                item.segmentTitles[1] = "CLOSE";
                item.selectedSegment = 0;
            } else {
                item.stateTitle = "CLOSED";
                item.iconResId = R.drawable.ext_rooftop_closed;
                item.segmentTitles[0] = "OPEN";
                item.segmentTitles[1] = "CLOSED";
                item.selectedSegment = 1;
            }

            builder.add(item);
        }

        if (vehicle.lightsState != null && vehicle.lightsState.getFrontExteriorLight().getValue() != null) {
            LightsState.FrontExteriorLight lightsState =
                    vehicle.lightsState.getFrontExteriorLight().getValue();
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.FRONT_EXTERIOR_LIGHT_STATE;
            item.actionSupported = vehicle.isSupported(ControlLights.IDENTIFIER,
                    ControlLights.IDENTIFIER_FRONT_EXTERIOR_LIGHT);
            item.title = resources.getString(R.string.frontLightsTitle);

            item.segmentCount = 3;
            item.segmentTitles = new String[3];

            item.segmentTitles[0] = "INACTIVE";
            item.segmentTitles[1] = "ACTIVE";
            item.segmentTitles[2] = "FULL BEAM";

            if (lightsState == LightsState.FrontExteriorLight.INACTIVE) {
                item.stateTitle = item.segmentTitles[0];
                item.iconResId = R.drawable.ext_front_lights_off;
                item.selectedSegment = 0;
            } else if (lightsState == LightsState.FrontExteriorLight.ACTIVE) {
                item.stateTitle = item.segmentTitles[1];
                item.iconResId = R.drawable.ext_front_lights_on;
                item.selectedSegment = 1;
            } else if (lightsState == LightsState.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM) {
                item.stateTitle = item.segmentTitles[2];
                item.iconResId = R.drawable.ext_front_lights_full_beam;
                item.selectedSegment = 2;
            }

            builder.add(item);
        }

        if (vehicle.isRemoteControlSupported()) {
            ExteriorListItem item = new ExteriorListItem();
            item.type = Type.REMOTE_CONTROL;
            item.title = "REMOTE CONTROL";
            item.iconResId = R.drawable.ext_remote;
            builder.add(item);
        }

        return builder.toArray(new ExteriorListItem[builder.size()]);
    }

    public static ExteriorListItem getItem(Type type, ExteriorListItem[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].type == type) return items[i];
        }
        return null;
    }

    public enum Type {
        DOORS_LOCKED,
        TRUNK_LOCK_STATE,
        IS_WINDSHIELD_DEFROSTING_ACTIVE,
        ROOFTOP_DIMMING_PERCENTAGE,
        ROOFTOP_POSITION,
        FRONT_EXTERIOR_LIGHT_STATE,
        REMOTE_CONTROL
    }
}