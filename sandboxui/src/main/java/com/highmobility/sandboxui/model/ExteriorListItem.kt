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
package com.highmobility.sandboxui.model

import android.content.res.Resources
import com.highmobility.autoapi.*
import com.highmobility.autoapi.value.LockState
import com.highmobility.sandboxui.R
import java.util.*

class ExteriorListItem {
    var segmentCount = 0
    var title: String? = null // the cell title
    var iconResId = 0 // cell icon
    var actionSupported = false
    var stateTitle: String? = null // the state title (if action not possible and segment not shown)

    lateinit var segmentTitles: Array<String>

    var selectedSegment = 0
    var type: Type? = null

    enum class Type {
        DOORS_LOCKED, TRUNK_LOCK_STATE, IS_WINDSHIELD_DEFROSTING_ACTIVE, ROOFTOP_DIMMING_PERCENTAGE, ROOFTOP_POSITION, FRONT_EXTERIOR_LIGHT_STATE, REMOTE_CONTROL
    }

    companion object {
        /**
         * Create the items that are displayed in exterior list view from VehicleStatus.
         */
        @JvmStatic
        fun createExteriorListItems(
            resources: Resources,
            vehicle: VehicleState
        ): Array<ExteriorListItem> {
            val builder = ArrayList<ExteriorListItem>()

            // create the items:
            if (vehicle.doorsLocked != null) {
                val item = ExteriorListItem()
                item.type = Type.DOORS_LOCKED
                item.actionSupported =
                    vehicle.isSupported(Doors.IDENTIFIER, Doors.PROPERTY_LOCKS_STATE)
                item.title = "DOOR LOCKS"
                item.segmentCount = 2

                if (vehicle.doorsLocked!!) {
                    item.stateTitle = "LOCKED"
                    item.iconResId = R.drawable.ext_doors_locked
                    item.segmentTitles = arrayOf("LOCKED", "UNLOCK")
                    item.selectedSegment = 0
                } else {
                    item.stateTitle = "UNLOCKED"
                    item.segmentTitles = arrayOf("LOCK", "UNLOCKED")
                    item.iconResId = R.drawable.ext_doors_unlocked
                    item.selectedSegment = 1
                }
                builder.add(item)
            }
            if (vehicle.trunkLockState != null) {
                val item = ExteriorListItem()
                item.type = Type.TRUNK_LOCK_STATE
                item.actionSupported = vehicle.isSupported(
                    Trunk.IDENTIFIER,
                    Trunk.PROPERTY_LOCK
                )
                item.title = "TRUNK LOCK"
                item.segmentCount = 2

                if (vehicle.trunkLockState == LockState.LOCKED) {
                    item.stateTitle = "LOCKED"
                    item.segmentTitles = arrayOf("LOCKED", "UNLOCK")
                    item.iconResId = R.drawable.ext_trunk_closed
                    item.selectedSegment = 0
                } else {
                    item.stateTitle = "UNLOCKED"
                    item.segmentTitles = arrayOf("LOCK", "UNLOCKED")
                    item.iconResId = R.drawable.ext_trunk_open
                    item.selectedSegment = 1
                }
                builder.add(item)
            }
            if (vehicle.isWindshieldDefrostingActive != null) {
                val item = ExteriorListItem()
                item.type = Type.IS_WINDSHIELD_DEFROSTING_ACTIVE
                item.actionSupported = vehicle.isSupported(
                    Climate.IDENTIFIER,
                    Climate.PROPERTY_DEFROSTING_STATE
                )
                item.title = "WINDSHIELD HEATING"
                item.segmentCount = 2

                if (vehicle.isWindshieldDefrostingActive!!) {
                    item.stateTitle = "ACTIVE"
                    item.segmentTitles = arrayOf("ACTIVE", "INACTIVATE")
                    item.iconResId = R.drawable.ext_windshield_heating_on
                    item.selectedSegment = 0
                } else {
                    item.segmentTitles = arrayOf("ACTIVATE", "INACTIVE")
                    item.stateTitle = "INACTIVE"
                    item.iconResId = R.drawable.ext_windshield_heating_off
                    item.selectedSegment = 1
                }
                builder.add(item)
            }
            if (vehicle.rooftopDimmingPercentage != null) {
                val item = ExteriorListItem()
                item.type = Type.ROOFTOP_DIMMING_PERCENTAGE
                item.actionSupported = vehicle.isSupported(
                    RooftopControl.IDENTIFIER,
                    RooftopControl.PROPERTY_DIMMING
                )
                item.title = "ROOFTOP DIMMING"
                item.segmentCount = 2
                item.segmentTitles = arrayOf("OPAQUE", "TRANSPARENT")
                if (vehicle.rooftopDimmingPercentage == 1.0) {
                    item.stateTitle = "OPAQUE"
                    item.iconResId = R.drawable.ext_roof_opaque
                    item.selectedSegment = 0
                } else {
                    item.stateTitle = "TRANSPARENT"
                    item.iconResId = R.drawable.ext_roof_transparent
                    item.selectedSegment = 1
                }
                builder.add(item)
            }
            if (vehicle.rooftopOpenPercentage != null) {
                val item = ExteriorListItem()
                item.type = Type.ROOFTOP_POSITION
                item.actionSupported = vehicle.isSupported(
                    RooftopControl.IDENTIFIER,
                    RooftopControl.PROPERTY_POSITION
                )
                item.title = "ROOFTOP OPENING"
                item.segmentCount = 2
                if (vehicle.rooftopOpenPercentage == 1.0) {
                    item.stateTitle = "OPEN"
                    item.iconResId = R.drawable.ext_rooftop_open
                    item.segmentTitles = arrayOf("OPEN", "CLOSE")
                    item.selectedSegment = 0
                } else {
                    item.stateTitle = "CLOSED"
                    item.iconResId = R.drawable.ext_rooftop_closed
                    item.segmentTitles = arrayOf("OPEN", "CLOSED")
                    item.selectedSegment = 1
                }
                builder.add(item)
            }
            if (vehicle.lightsState != null && vehicle.lightsState!!.frontExteriorLight.value != null) {
                val lightsState = vehicle.lightsState!!.frontExteriorLight.value
                val item = ExteriorListItem()
                item.type = Type.FRONT_EXTERIOR_LIGHT_STATE
                item.actionSupported = vehicle.isSupported(
                    Lights.IDENTIFIER,
                    Lights.PROPERTY_FRONT_EXTERIOR_LIGHT
                )
                item.title = resources.getString(R.string.frontLightsTitle)
                item.segmentCount = 3
                item.segmentTitles = arrayOf("INACTIVE", "ACTIVE", "FULL BEAM")

                when (lightsState) {
                    Lights.FrontExteriorLight.INACTIVE -> {
                        item.stateTitle = item.segmentTitles[0]
                        item.iconResId = R.drawable.ext_front_lights_off
                        item.selectedSegment = 0
                    }
                    Lights.FrontExteriorLight.ACTIVE_WITH_FULL_BEAM -> {
                        item.stateTitle = item.segmentTitles[2]
                        item.iconResId = R.drawable.ext_front_lights_full_beam
                        item.selectedSegment = 2
                    }
                    Lights.FrontExteriorLight.ACTIVE,
                    Lights.FrontExteriorLight.AUTOMATIC,
                    Lights.FrontExteriorLight.DRL-> {
                        item.stateTitle = item.segmentTitles[1]
                        item.iconResId = R.drawable.ext_front_lights_on
                        item.selectedSegment = 1
                    }
                }
                builder.add(item)
            }
            if (vehicle.isRemoteControlSupported) {
                val item = ExteriorListItem()
                item.type = Type.REMOTE_CONTROL
                item.title = "REMOTE CONTROL"
                item.iconResId = R.drawable.ext_remote
                builder.add(item)
            }
            return builder.toTypedArray()
        }

        @JvmStatic
        fun getItem(type: Type, items: Array<ExteriorListItem>): ExteriorListItem? {
            for (i in items.indices) {
                if (items[i].type == type) return items[i]
            }
            return null
        }
    }
}