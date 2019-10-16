package highmobility.com.sandboxui;

import android.content.res.Resources;

import com.highmobility.autoapi.CapabilitiesState;
import com.highmobility.autoapi.ControlLights;
import com.highmobility.autoapi.ControlRooftop;
import com.highmobility.autoapi.Identifier;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.StartStopDefrosting;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.SupportedCapability;
import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.model.ExteriorListItem;
import com.highmobility.sandboxui.model.VehicleStatus;
import com.highmobility.value.Bytes;

import org.junit.Before;
import org.junit.Test;

import androidx.annotation.NonNull;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by ttiganik on 30/03/2017.
 */
public class ExteriorListItemTest {
    Resources resources = new TestResources();

    @Before
    public void setUp() {

    }

    /**
     * add all of the readable items that are not null. check if action capability is supported,
     * then enable the buttons as well. only use single list item, with 2 or 3 segments
     */
    @Test
    public void testNullItemNotAdded() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        assertTrue(items.length == 1);
    }

    @Test
    public void testActionEnabledIfCapabilitySupported() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.doorsLocked = false;
        CapabilitiesState capas = new CapabilitiesState.Builder().addCapability(new
                Property(new SupportedCapability(LockUnlockDoors.IDENTIFIER.asInt(),
                new Bytes(new byte[]{LockUnlockDoors.IDENTIFIER_INSIDE_LOCKS_STATE})))).build();

        vehicle.update(capas);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        assertTrue(items[0].actionSupported == true);
    }

    @Test
    public void testActionDisabledIfCapabilitySupported() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        assertTrue(items[0].actionSupported == false);
    }

    @Test
    public void testIcon() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.lightsState =
                new LightsState.Builder().setFrontExteriorLight(new Property(LightsState.FrontExteriorLight.INACTIVE)).build();
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem item =
                ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        assertTrue(item.iconResId == R.drawable.ext_front_lights_off);
        vehicle.lightsState =
                new LightsState.Builder().setFrontExteriorLight(new Property(LightsState.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM)).build();

        items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        item = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);

        assertTrue(item.iconResId == R.drawable.ext_front_lights_full_beam);
    }

    @Test
    public void testSegmentCountAndText() {
        // check that segment count is correct
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.lightsState =
                new LightsState.Builder().setFrontExteriorLight(new Property(LightsState.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM)).build();

        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem lightsItem =
                ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        ExteriorListItem doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.DOORS_LOCKED,
                items);

        assertTrue(lightsItem.actionSupported == false); // capa not supported
        assertTrue(doorsItem.actionSupported == false); // capa not supported

        SupportedCapability lightsCapability = new SupportedCapability(Identifier.LIGHTS.asInt(),
                new Bytes(new byte[]{ControlLights.IDENTIFIER_FRONT_EXTERIOR_LIGHT}));

        SupportedCapability lockCapability = new SupportedCapability(
                LockUnlockDoors.IDENTIFIER.asInt(),
                new Bytes(new byte[]{LockUnlockDoors.IDENTIFIER_INSIDE_LOCKS_STATE}));

        CapabilitiesState capas = new CapabilitiesState.Builder()
                .addCapability(new Property(lockCapability))
                .addCapability(new Property(lightsCapability)).build();
        vehicle.update(capas);

        items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        lightsItem = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE,
                items);
        doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.DOORS_LOCKED, items);

        assertTrue(lightsItem.actionSupported == true); // capa not supported
        assertTrue(doorsItem.actionSupported == true); // capa not supported
    }

    @Test
    public void actionEnabledChangesText() {
        // check that segment count is correct
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.isWindshieldDefrostingActive = true;
        CapabilitiesState capas = new CapabilitiesState.Builder()
                .addCapability(new Property(new SupportedCapability(StartStopDefrosting.IDENTIFIER.asInt(),
                        new Bytes(new byte[]{StartStopDefrosting.IDENTIFIER_DEFROSTING_STATE}))))
                .build();
        vehicle.update(capas);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem doorsItem =
                ExteriorListItem.getItem(ExteriorListItem.Type.IS_WINDSHIELD_DEFROSTING_ACTIVE,
                        items);

        String textFirst = doorsItem.segmentTitles[0];

        vehicle.isWindshieldDefrostingActive = false;
        items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        doorsItem =
                ExteriorListItem.getItem(ExteriorListItem.Type.IS_WINDSHIELD_DEFROSTING_ACTIVE,
                        items);

        String textAfter = doorsItem.segmentTitles[0];
        assertTrue(textFirst.equals(textAfter) == false);
    }

    @Test
    public void capabilitySetsActionSupport() {
        // check that segment count is correct
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.rooftopOpenPercentage = 1d;

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem item =
                ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items);

        assertTrue(item.actionSupported == false);

        SupportedCapability capa = new SupportedCapability(ControlRooftop.IDENTIFIER.asInt(),
                new Bytes(new byte[]{ControlRooftop.IDENTIFIER_POSITION}));

        CapabilitiesState capas = new CapabilitiesState.Builder()
                .addCapability(new Property(capa)).build();

        vehicle.update(capas);
        items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        item = ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items);

        assertTrue(item.actionSupported == true);
    }

    @Test
    public void segmentCount() {
        VehicleStatus vehicle = new VehicleStatus();
        vehicle.rooftopOpenPercentage = 1d;

        vehicle.lightsState =
                new LightsState.Builder().setFrontExteriorLight(new Property(LightsState.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM)).build();
        CapabilitiesState capas = new CapabilitiesState.Builder()
                .addCapability(new Property(new SupportedCapability(ControlRooftop.IDENTIFIER.asInt(),
                        new Bytes(new byte[]{ControlRooftop.IDENTIFIER_POSITION}))))
                .addCapability(new Property(new SupportedCapability(ControlLights.IDENTIFIER.asInt(),
                        new Bytes(new byte[]{ControlLights.IDENTIFIER_FRONT_EXTERIOR_LIGHT})))).build();
        vehicle.update(capas);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem item =
                ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items);
        ExteriorListItem item2 =
                ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);

        assertTrue(item.segmentCount == 2);
        assertTrue(item2.segmentCount == 3);
    }

    private class TestResources extends Resources {
        public TestResources() {
            super(null, null, null);
        }

        @NonNull @Override public String getString(int id) throws NotFoundException {
            return "test env";
        }
    }
}