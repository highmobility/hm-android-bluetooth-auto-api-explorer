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
package highmobility.com.sandboxui

import android.content.res.Resources
import com.highmobility.autoapi.*
import com.highmobility.autoapi.property.Property
import com.highmobility.autoapi.value.SupportedCapability
import com.highmobility.sandboxui.R
import com.highmobility.sandboxui.model.ExteriorListItem
import com.highmobility.sandboxui.model.ExteriorListItem.Companion.createExteriorListItems
import com.highmobility.sandboxui.model.ExteriorListItem.Companion.getItem
import com.highmobility.sandboxui.model.VehicleState
import com.highmobility.value.Bytes
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ExteriorListItemTest {
    val resources = Mockito.mock(Resources::class.java)

    @Before
    fun setUp() {
    }

    /**
     * add all of the readable items that are not null. check if action capability is supported,
     * then enable the buttons as well. only use single list item, with 2 or 3 segments
     */
    @Test
    fun testNullItemNotAdded() {
        val vehicle = VehicleState()
        vehicle.doorsLocked = false
        val items = createExteriorListItems(resources, vehicle)
        TestCase.assertTrue(items.size == 1)
    }

    @Test
    fun testActionEnabledIfCapabilitySupported() {
        val vehicle = VehicleState()
        vehicle.doorsLocked = false
        val capas = Capabilities.State.Builder().addCapability(
            Property(
                SupportedCapability(
                    Doors.IDENTIFIER,
                    Bytes(byteArrayOf(Doors.PROPERTY_LOCKS_STATE))
                )
            )
        ).build()
        vehicle.update(capas)
        val items = createExteriorListItems(resources, vehicle)
        TestCase.assertTrue(items[0].actionSupported)
    }

    @Test
    fun testActionDisabledIfCapabilitySupported() {
        val vehicle = VehicleState()
        vehicle.doorsLocked = false
        val items = createExteriorListItems(resources, vehicle)
        TestCase.assertTrue(!items[0].actionSupported)
    }

    @Test
    fun testIcon() {
        val vehicle = VehicleState()
        vehicle.lightsState = Lights.State.Builder()
            .setFrontExteriorLight(Property(Lights.FrontExteriorLight.INACTIVE)).build()
        var items: Array<ExteriorListItem> = createExteriorListItems(resources, vehicle)
        var item = getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items)
        TestCase.assertTrue(item!!.iconResId == R.drawable.ext_front_lights_off)
        vehicle.lightsState = Lights.State.Builder()
            .setFrontExteriorLight(Property(Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM))
            .build()
        items = createExteriorListItems(resources, vehicle)
        item = getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items)
        TestCase.assertTrue(item!!.iconResId == R.drawable.ext_front_lights_full_beam)
    }

    @Test
    fun testSegmentCountAndText() {
        // check that segment count is correct
        val vehicle = VehicleState()
        vehicle.lightsState = Lights.State.Builder()
            .setFrontExteriorLight(Property(Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM))
            .build()
        vehicle.doorsLocked = false
        var items: Array<ExteriorListItem> = createExteriorListItems(resources, vehicle)
        var lightsItem = getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items)
        var doorsItem = getItem(
            ExteriorListItem.Type.DOORS_LOCKED,
            items
        )
        TestCase.assertTrue(!lightsItem!!.actionSupported) // capa not supported
        TestCase.assertTrue(!doorsItem!!.actionSupported) // capa not supported
        val lightsCapability = SupportedCapability(
            Identifier.LIGHTS,
            Bytes(byteArrayOf(Lights.PROPERTY_FRONT_EXTERIOR_LIGHT))
        )
        val lockCapability = SupportedCapability(
            Doors.IDENTIFIER,
            Bytes(byteArrayOf(Doors.PROPERTY_LOCKS_STATE))
        )
        val capas = Capabilities.State.Builder()
            .addCapability(Property(lockCapability))
            .addCapability(Property(lightsCapability)).build()
        vehicle.update(capas)
        items = createExteriorListItems(resources, vehicle)
        lightsItem = getItem(
            ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE,
            items
        )
        doorsItem = getItem(ExteriorListItem.Type.DOORS_LOCKED, items)
        TestCase.assertTrue(lightsItem!!.actionSupported) // capa not supported
        TestCase.assertTrue(doorsItem!!.actionSupported) // capa not supported
    }

    @Test
    fun actionEnabledChangesText() {
        // check that segment count is correct
        val vehicle = VehicleState()
        vehicle.isWindshieldDefrostingActive = true
        val capas = Capabilities.State.Builder()
            .addCapability(
                Property(
                    SupportedCapability(
                        Climate.IDENTIFIER,
                        Bytes(byteArrayOf(Climate.PROPERTY_DEFROSTING_STATE))
                    )
                )
            )
            .build()
        vehicle.update(capas)
        var items: Array<ExteriorListItem> = createExteriorListItems(resources, vehicle)
        var doorsItem = getItem(
            ExteriorListItem.Type.IS_WINDSHIELD_DEFROSTING_ACTIVE,
            items
        )
        val textFirst = doorsItem!!.segmentTitles[0]
        vehicle.isWindshieldDefrostingActive = false
        items = createExteriorListItems(resources, vehicle)
        doorsItem = getItem(
            ExteriorListItem.Type.IS_WINDSHIELD_DEFROSTING_ACTIVE,
            items
        )
        val textAfter = doorsItem!!.segmentTitles[0]
        TestCase.assertTrue(textFirst != textAfter)
    }

    @Test
    fun capabilitySetsActionSupport() {
        // check that segment count is correct
        val vehicle = VehicleState()
        vehicle.rooftopOpenPercentage = 1.0
        var items: Array<ExteriorListItem> = createExteriorListItems(resources, vehicle)
        var item = getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items)
        TestCase.assertTrue(!item!!.actionSupported)
        val capa = SupportedCapability(
            RooftopControl.IDENTIFIER,
            Bytes(byteArrayOf(RooftopControl.PROPERTY_POSITION))
        )
        val capas = Capabilities.State.Builder()
            .addCapability(Property(capa)).build()
        vehicle.update(capas)
        items = createExteriorListItems(resources, vehicle)
        item = getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items)
        TestCase.assertTrue(item!!.actionSupported)
    }

    @Test
    fun segmentCount() {
        val vehicle = VehicleState()
        vehicle.rooftopOpenPercentage = 1.0
        vehicle.lightsState = Lights.State.Builder()
            .setFrontExteriorLight(Property(Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM))
            .build()
        val capas = Capabilities.State.Builder()
            .addCapability(
                Property(
                    SupportedCapability(
                        RooftopControl.IDENTIFIER,
                        Bytes(byteArrayOf(RooftopControl.PROPERTY_POSITION))
                    )
                )
            )
            .addCapability(
                Property(
                    SupportedCapability(
                        Lights.IDENTIFIER,
                        Bytes(byteArrayOf(Lights.PROPERTY_FRONT_EXTERIOR_LIGHT))
                    )
                )
            ).build()
        vehicle.update(capas)
        val items = createExteriorListItems(resources, vehicle)
        val item = getItem(ExteriorListItem.Type.ROOFTOP_POSITION, items)
        val item2 = getItem(ExteriorListItem.Type.FRONT_EXTERIOR_LIGHT_STATE, items)
        TestCase.assertTrue(item!!.segmentCount == 2)
        TestCase.assertTrue(item2!!.segmentCount == 3)
    }
}