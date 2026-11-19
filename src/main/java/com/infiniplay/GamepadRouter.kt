package com.infiniplay

import android.content.Context
import android.hardware.input.InputManager
import android.view.*
import com.infiniplay.core.PadLayout

class GamepadRouter(
    context: Context,
    private val keys: (String, Int, Set<Int>) -> Unit,
    private val analog: (String, Int, Int, Float, Float) -> Unit,
    private val removed: (String) -> Unit,
    private val changed: () -> Unit,
) : InputManager.InputDeviceListener {
    private val manager = context.getSystemService(InputManager::class.java)
    private val store = PlayStore(context)
    private val ports = mutableMapOf<Int, Int>()

    fun devices() =
        InputDevice.getDeviceIds().toList().mapNotNull(InputDevice::getDevice).filter {
            it.supportsSource(InputDevice.SOURCE_GAMEPAD) ||
                it.supportsSource(InputDevice.SOURCE_JOYSTICK)
        }

    fun port(device: InputDevice): Int =
        ports.getOrPut(device.id) {
            store
                .controllerPort(
                    device.descriptor,
                    ((0..3).firstOrNull { it !in ports.values } ?: 0),
                )
                .coerceIn(0, 3)
        }

    fun assign(device: InputDevice, port: Int) {
        removed("hardware:${device.id}:")
        ports[device.id] = port.coerceIn(0, 3)
        store.controllerPort(device.descriptor, port.coerceIn(0, 3), true)
        changed()
    }

    fun start() {
        ports.clear()
        manager.registerInputDeviceListener(this, null)
        devices().forEach { port(it) }
        changed()
    }

    fun stop() {
        manager.unregisterInputDeviceListener(this)
        ports.keys.forEach { removed("hardware:$it:") }
    }

    override fun onInputDeviceAdded(id: Int) {
        InputDevice.getDevice(id)
            ?.takeIf {
                it.supportsSource(InputDevice.SOURCE_GAMEPAD) ||
                    it.supportsSource(InputDevice.SOURCE_JOYSTICK)
            }
            ?.let { port(it) }
        changed()
    }

    override fun onInputDeviceChanged(id: Int) {
        removed("hardware:$id:")
        changed()
    }

    override fun onInputDeviceRemoved(id: Int) {
        removed("hardware:$id:")
        ports.remove(id)
        changed()
    }

    fun key(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (
            !device.supportsSource(InputDevice.SOURCE_GAMEPAD) &&
                !device.supportsSource(InputDevice.SOURCE_JOYSTICK)
        )
            return false
        if (
            event.keyCode !in
                setOf(
                    19,
                    20,
                    21,
                    22,
                    96,
                    97,
                    98,
                    99,
                    100,
                    101,
                    102,
                    103,
                    104,
                    105,
                    106,
                    107,
                    108,
                    109,
                )
        )
            return false
        val mapped = PadLayout.physicalKey(event.keyCode)
        keys(
            "hardware:${device.id}:key:${event.keyCode}",
            port(device),
            if (event.action == KeyEvent.ACTION_DOWN) setOf(mapped) else emptySet(),
        )
        return true
    }

    fun motion(event: MotionEvent): Boolean {
        if (
            !event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
                event.actionMasked != MotionEvent.ACTION_MOVE
        )
            return false
        val device = event.device ?: return false
        val port = port(device)
        val source = "hardware:${device.id}:"
        fun axis(id: Int): Float {
            val flat = device.getMotionRange(id, event.source)?.flat ?: 0f
            return PadLayout.deadZone(event.getAxisValue(id).toDouble(), flat.toDouble()).toFloat()
        }
        val hat = mutableSetOf<Int>()
        val hx = axis(MotionEvent.AXIS_HAT_X)
        val hy = axis(MotionEvent.AXIS_HAT_Y)
        if (hx < -.5) hat += 21
        if (hx > .5) hat += 22
        if (hy < -.5) hat += 19
        if (hy > .5) hat += 20
        keys(source + "hat", port, hat)
        analog(source + "left", port, 1, axis(MotionEvent.AXIS_X), axis(MotionEvent.AXIS_Y))
        analog(
            source + "right",
            port,
            2,
            axis(
                if (device.getMotionRange(MotionEvent.AXIS_Z) != null) MotionEvent.AXIS_Z
                else MotionEvent.AXIS_RX
            ),
            axis(
                if (device.getMotionRange(MotionEvent.AXIS_RZ) != null) MotionEvent.AXIS_RZ
                else MotionEvent.AXIS_RY
            ),
        )
        fun trigger(primary: Int, alternate: Int, key: Int) {
            val v = maxOf(event.getAxisValue(primary), event.getAxisValue(alternate))
            keys(source + "trigger:$key", port, if (v > .5) setOf(key) else emptySet())
        }
        trigger(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE, 104)
        trigger(MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS, 105)
        return true
    }
}
