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
package highmobility.com.sandboxui;

import android.content.res.Resources;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Climate;
import com.highmobility.autoapi.Doors;
import com.highmobility.autoapi.Identifier;
import com.highmobility.autoapi.Lights;
import com.highmobility.autoapi.RooftopControl;
import com.highmobility.autoapi.property.Property;
import com.highmobility.autoapi.value.SupportedCapability;
import com.highmobility.sandboxui.R;
import com.highmobility.sandboxui.model.ExteriorListItem;
import com.highmobility.sandboxui.model.VehicleState;
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
        VehicleState vehicle = new VehicleState();
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        assertTrue(items.length == 1);
    }

    @Test
    public void testActionEnabledIfCapabilitySupported() {
        VehicleState vehicle = new VehicleState();
        vehicle.doorsLocked = false;
        Capabilities.State capas = new Capabilities.State.Builder().addCapability(new
                Property(new SupportedCapability(Doors.IDENTIFIER,
                new Bytes(new byte[]{Doors.PROPERTY_LOCKS_STATE})))).build();

        vehicle.update(capas);

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        assertTrue(items[0].actionSupported == true);
    }

    @Test
    public void testActionDisabledIfCapabilitySupported() {
        VehicleState vehicle = new VehicleState();
        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        assertTrue(items[0].actionSupported == false);
    }

    @Test
    public void testIcon() {
        VehicleState vehicle = new VehicleState();
        vehicle.lightsState =
                new Lights.State.Builder().setFrontExteriorLight(new Property(Lights.FrontExteriorLight.INACTIVE)).build();
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem item =
                ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        assertTrue(item.iconResId == R.drawable.ext_front_lights_off);
        vehicle.lightsState =
                new Lights.State.Builder().setFrontExteriorLight(new Property(Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM)).build();

        items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        item = ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);

        assertTrue(item.iconResId == R.drawable.ext_front_lights_full_beam);
    }

    @Test
    public void testSegmentCountAndText() {
        // check that segment count is correct
        VehicleState vehicle = new VehicleState();
        vehicle.lightsState =
                new Lights.State.Builder().setFrontExteriorLight(new Property(Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM)).build();

        vehicle.doorsLocked = false;
        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem lightsItem =
                ExteriorListItem.getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items);
        ExteriorListItem doorsItem = ExteriorListItem.getItem(ExteriorListItem.Type.DOORS_LOCKED,
                items);

        assertTrue(lightsItem.actionSupported == false); // capa not supported
        assertTrue(doorsItem.actionSupported == false); // capa not supported

        SupportedCapability lightsCapability = new SupportedCapability(Identifier.LIGHTS,
                new Bytes(new byte[]{Lights.PROPERTY_FRONT_EXTERIOR_LIGHT}));

        SupportedCapability lockCapability = new SupportedCapability(
                Doors.IDENTIFIER,
                new Bytes(new byte[]{Doors.PROPERTY_LOCKS_STATE}));

        Capabilities.State capas = new Capabilities.State.Builder()
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
        VehicleState vehicle = new VehicleState();
        vehicle.isWindshieldDefrostingActive = true;
        Capabilities.State capas = new Capabilities.State.Builder()
                .addCapability(new Property(new SupportedCapability(Climate.IDENTIFIER,
                        new Bytes(new byte[]{Climate.PROPERTY_DEFROSTING_STATE}))))
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
        VehicleState vehicle = new VehicleState();
        vehicle.rooftopOpenPercentage = 1d;

        ExteriorListItem[] items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        ExteriorListItem item =
                ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items);

        assertTrue(item.actionSupported == false);

        SupportedCapability capa = new SupportedCapability(RooftopControl.IDENTIFIER,
                new Bytes(new byte[]{RooftopControl.PROPERTY_POSITION}));

        Capabilities.State capas = new Capabilities.State.Builder()
                .addCapability(new Property(capa)).build();

        vehicle.update(capas);
        items = ExteriorListItem.createExteriorListItems(resources, vehicle);
        item = ExteriorListItem.getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items);

        assertTrue(item.actionSupported == true);
    }

    @Test
    public void segmentCount() {
        VehicleState vehicle = new VehicleState();
        vehicle.rooftopOpenPercentage = 1d;

        vehicle.lightsState =
                new Lights.State.Builder().setFrontExteriorLight(new Property(Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM)).build();
        Capabilities.State capas = new Capabilities.State.Builder()
                .addCapability(new Property(new SupportedCapability(RooftopControl.IDENTIFIER,
                        new Bytes(new byte[]{RooftopControl.PROPERTY_POSITION}))))
                .addCapability(new Property(new SupportedCapability(Lights.IDENTIFIER,
                        new Bytes(new byte[]{Lights.PROPERTY_FRONT_EXTERIOR_LIGHT})))).build();
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