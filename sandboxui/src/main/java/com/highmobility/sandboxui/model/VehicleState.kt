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

import com.highmobility.autoapi.*
import com.highmobility.autoapi.property.Property
import com.highmobility.autoapi.value.ActiveState
import com.highmobility.autoapi.value.LockState
import com.highmobility.autoapi.value.Position
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.hmkit.Link
import timber.log.Timber
import java.util.*

/**
 * This class will keep the state of the vehicle according to commands received.
 */
class VehicleState {
    var name: String? = null
    var insideTemperature: Double? = null
    var batteryPercentage: Double? = null
    var doorsLocked: Boolean? = null
    var trunkLockState: LockState? = null
    var trunkLockPosition: Position? = null
    var isWindshieldDefrostingActive: Boolean? = null
    var rooftopDimmingPercentage: Double? = null
    var rooftopOpenPercentage: Double? = null

    // retained for set commands
    var lightsState: Lights.State? = null
    var lightsAmbientColor: IntArray = intArrayOf()
    var capabilitiesState: Capabilities.State? = null
        private set

    fun update(command: Command?) {
        if (command is VehicleStatus.State) {
            val states = command.states
            if (states == null) {
                Timber.e("update: null featureStates")
                return
            }
            for (i in states.indices) {
                if (states[i].value != null) update(states[i].value)
            }
        } else if (command is VehicleInformation.State) {
            name = command.name.value
        } else if (command is Climate.State) {
            insideTemperature = command.insideTemperature.value?.value
            isWindshieldDefrostingActive = command.defrostingState.value == ActiveState.ACTIVE
        } else if (command is Doors.State) {
            doorsLocked = command.locksState.value == LockState.LOCKED
        } else if (command is Trunk.State) {
            trunkLockState = command.lock.value
            trunkLockPosition = command.position.value
        } else if (command is RooftopControl.State) {
            rooftopDimmingPercentage = command.dimming.value
            rooftopOpenPercentage = command.position.value
        } else if (command is Charging.State) {
            batteryPercentage = command.batteryLevel.value
        } else if (command is Lights.State) {
            lightsState = command
        } else if (command is Capabilities.State) {
            capabilitiesState = command
        }
    }

    fun onLinkAuthenticated(link: Link) {
        // when link first connects, it has no serial(auth gives serial). We use the serial from
        // backend.
        vehicleConnectedWithBle = link.serial
    }

    fun onLinkConnected() {
        // we do not have a serial yet.

        // Could be that previously link connected but did not authenticate and its serial
        // is null(vehicleConnectedWithBle not set), this means we will start broadcasting
        // when a link is connected. Currently the SDK does not have a fix to it.
        vehicleConnectedWithBle = DeviceSerial("000000000000000000")
    }

    fun isSupported(identifier: Int?, property: Byte): Boolean {
        return if (capabilitiesState == null) false else capabilitiesState!!.getSupported(
            identifier,
            property
        )
    }

    val isRemoteControlSupported: Boolean
        get() = isSupported(RemoteControl.IDENTIFIER, RemoteControl.PROPERTY_SPEED) &&
                isSupported(RemoteControl.IDENTIFIER, RemoteControl.PROPERTY_ANGLE)

    companion object {
        // means SDK cannot be terminated
        @JvmField
        var vehicleConnectedWithBle: DeviceSerial? = null
        fun isVehicleConnected(serial: DeviceSerial): Boolean {
            return vehicleConnectedWithBle != null && serial == vehicleConnectedWithBle == true
        }

        fun isVehicleConnectedButNotTo(serial: DeviceSerial): Boolean {
            return vehicleConnectedWithBle != null && serial == vehicleConnectedWithBle == false
        }

        /**
         * Convert property array to value array.
         *
         * @return The new values array.
         */
        @JvmStatic
        fun <V> propertiesToValues(properties: List<Property<V>>): List<V?> {
            val values = ArrayList<V?>()
            for (i in properties.indices) {
                values.add(properties[i].value)
            }
            return values
        }
    }
}