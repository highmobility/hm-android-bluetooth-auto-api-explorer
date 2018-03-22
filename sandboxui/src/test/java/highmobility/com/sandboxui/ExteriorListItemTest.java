package highmobility.com.sandboxui;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.Identifier;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.CapabilityProperty;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.model.ExteriorListItem;
import com.highmobility.sandboxui.model.VehicleStatus;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * Created by ttiganik on 30/03/2017.
 */
public class ExteriorListItemTest {

    /**
     * - [ ] add all of the readable items that are not null
     * - [ ] check if action capability is supported, then enable the buttons as well
     * - [ ] only use single list item, with 2 or 3 segments
     */
    @Test
    public void testNullItemNotAdded() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        assertTrue(items.length == 1);
    }

    @Test
    public void testActionEnabledIfCapabilitySupported() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.doorsLocked = false;
        Capabilities capas = new Capabilities.Builder().addCapability(new
                CapabilityProperty(Identifier.DOOR_LOCKS, new Type[] { LockUnlockDoors.TYPE })).build();
        vehicle.update(capas, true);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        assertTrue(items[0].actionSupported == true);
    }

    @Test
    public void testActionDisabledIfCapabilitySupported() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        assertTrue(items[0].actionSupported == false);
    }

    @Test
    public void testIcon() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.frontExteriorLightState = FrontExteriorLightState.INACTIVE;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        ExteriorListItem item = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        assertTrue(item.iconResId == R.drawable.ext_front_lights_off);
        vehicle.frontExteriorLightState = FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM;

        items = ExteriorListItem.createExteriorListItems(vehicle);
        item = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);

        assertTrue(item.iconResId == R.drawable.ext_front_lights_full_beam);
    }

    @Test
    public void testSegmentCountAndText() {
        // check that segment count is correct
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.frontExteriorLightState = FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM;
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        ExteriorListItem lightsItem = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        ExteriorListItem doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.DOORS_LOCKED, items);

        assertTrue(lightsItem.actionSupported == false); // capa not supported
        assertTrue(doorsItem.actionSupported == false); // capa not supported

        Capabilities capas = new Capabilities.Builder()
                .addCapability(new CapabilityProperty(Identifier.DOOR_LOCKS, new Type[] { LockUnlockDoors.TYPE }))
                .addCapability(new CapabilityProperty(Identifier.LIGHTS, new Type[] { ControlLights.TYPE })).build();
        vehicle.update(capas, true);

        items = ExteriorListItem.createExteriorListItems(vehicle);
        lightsItem = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.DOORS_LOCKED, items);

        assertTrue(lightsItem.actionSupported == true); // capa not supported
        assertTrue(doorsItem.actionSupported == true); // capa not supported
    }

    @Test
    public void actionEnabledChangesText() {
        // check that segment count is correct
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.isWindshieldDefrostingActive = true;
        Capabilities capas = new Capabilities.Builder()
                .addCapability(new CapabilityProperty(Identifier.CLIMATE, new Type[] { StartStopDefrosting.TYPE }))
                .build();
        vehicle.update(capas, true);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        ExteriorListItem doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.IS_WINDSHIELD_DEFROSTING_ACTIVE, items);

        String textFirst = doorsItem.segmentTitles[0];

        vehicle.isWindshieldDefrostingActive = false;
        items = ExteriorListItem.createExteriorListItems(vehicle);
        doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.IS_WINDSHIELD_DEFROSTING_ACTIVE, items);

        String textAfter = doorsItem.segmentTitles[0];
        assertTrue(textFirst.equals(textAfter) == false);
    }

    @Test
    public void capabilitySetsActionSupport() {
        // check that segment count is correct
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.rooftopOpenPercentage = 1f;

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        ExteriorListItem item = ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_OPEN_PERCENTAGE, items);

        assertTrue(item.actionSupported == false);

        Capabilities capas = new Capabilities.Builder()
                .addCapability(new CapabilityProperty(Identifier.ROOFTOP, new Type[] { ControlRooftop.TYPE }))
                .build();
        vehicle.update(capas, true);
        items = ExteriorListItem.createExteriorListItems(vehicle);
        item = ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_OPEN_PERCENTAGE, items);

        assertTrue(item.actionSupported == true);
    }

    @Test
    public void segmentCount() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.rooftopOpenPercentage = 1f;
        vehicle.frontExteriorLightState = FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM;
        Capabilities capas = new Capabilities.Builder()
                .addCapability(new CapabilityProperty(Identifier.ROOFTOP, new Type[] { ControlRooftop.TYPE }))
                .addCapability(new CapabilityProperty(Identifier.LIGHTS, new Type[] { ControlLights.TYPE }))
                .build();
        vehicle.update(capas, true);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(vehicle);
        ExteriorListItem item = ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_OPEN_PERCENTAGE, items);
        ExteriorListItem item2 = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);

        assertTrue(item.segmentCount == 2);
        assertTrue(item2.segmentCount == 3);
    }
}