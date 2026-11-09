package com.example.konteksto.data.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.example.konteksto.domain.model.AudioInputDevice
import com.example.konteksto.domain.model.AudioInputType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface AudioInputRepository {
    /** Emits the input device now, then again whenever a microphone is connected, removed or rerouted. */
    val currentInputDevice: Flow<AudioInputDevice>

    /**
     * While enabled, a connected Bluetooth headset's mic is used instead of the phone's, including
     * one that connects later. Disabling it gives the audio route back to the system.
     */
    fun setBluetoothMicEnabled(enabled: Boolean)
}

/** Every method must be called on the main thread, where the device callbacks also run. */
class AndroidAudioInputRepository(context: Context) : AudioInputRepository {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isBluetoothMicEnabled = false
    private var isHoldingBluetoothRoute = false

    // Keeps the route on a Bluetooth headset that connects or reconnects mid-broadcast.
    private val routeCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = applyBluetoothRoute()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = applyBluetoothRoute()
    }

    override val currentInputDevice: Flow<AudioInputDevice> = callbackFlow {
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                trySend(resolveInputDevice())
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                trySend(resolveInputDevice())
            }
        }
        val communicationListener = AudioManager.OnCommunicationDeviceChangedListener {
            trySend(resolveInputDevice())
        }
        // Registering calls onAudioDevicesAdded with the devices already connected.
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        audioManager.addOnCommunicationDeviceChangedListener({ mainHandler.post(it) }, communicationListener)
        awaitClose {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            audioManager.removeOnCommunicationDeviceChangedListener(communicationListener)
        }
    }.distinctUntilChanged()

    override fun setBluetoothMicEnabled(enabled: Boolean) {
        if (isBluetoothMicEnabled == enabled) return
        isBluetoothMicEnabled = enabled
        if (enabled) {
            audioManager.registerAudioDeviceCallback(routeCallback, mainHandler)
            applyBluetoothRoute()
        } else {
            audioManager.unregisterAudioDeviceCallback(routeCallback)
            releaseBluetoothRoute()
        }
    }

    /**
     * Speech recognition only records from a Bluetooth headset while it is the communication
     * device, which also needs the phone in communication mode, so playback on the headset drops
     * to call quality until the route is released.
     */
    private fun applyBluetoothRoute() {
        if (!isBluetoothMicEnabled) return
        val headset = audioManager.availableCommunicationDevices.firstOrNull { it.isBluetoothHeadset() }
        if (headset == null) {
            releaseBluetoothRoute()
            return
        }
        if (isHoldingBluetoothRoute && audioManager.communicationDevice?.id == headset.id) return

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        isHoldingBluetoothRoute = audioManager.setCommunicationDevice(headset)
        if (!isHoldingBluetoothRoute) audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun releaseBluetoothRoute() {
        if (!isHoldingBluetoothRoute) return
        isHoldingBluetoothRoute = false
        audioManager.clearCommunicationDevice()
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun resolveInputDevice(): AudioInputDevice {
        val communicationDevice = audioManager.communicationDevice
        if (isHoldingBluetoothRoute && communicationDevice != null && communicationDevice.isBluetoothHeadset()) {
            return AudioInputDevice(AudioInputType.Bluetooth, communicationDevice.productName?.toString().orEmpty().trim())
        }
        return pickDefaultInputDevice(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList())
    }
}

private fun AudioDeviceInfo.isBluetoothHeadset(): Boolean =
    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLE_HEADSET

/**
 * Without a Bluetooth route, speech recognition records from the default source, which Android
 * routes to a USB or wired headset mic when one is plugged in and to the phone's own mic otherwise.
 */
private fun pickDefaultInputDevice(devices: List<AudioDeviceInfo>): AudioInputDevice {
    val usb = devices.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE
    }
    if (usb != null) return AudioInputDevice(AudioInputType.Usb, usb.productName?.toString().orEmpty().trim())

    val wired = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
    if (wired != null) return AudioInputDevice(AudioInputType.WiredHeadset)

    return AudioInputDevice(AudioInputType.BuiltInMic)
}
