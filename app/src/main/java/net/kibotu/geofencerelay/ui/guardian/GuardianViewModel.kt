package net.kibotu.geofencerelay.ui.guardian

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.kibotu.geofencerelay.features.ai.history.CognitiveHistoryManager
import net.kibotu.geofencerelay.features.ai.history.DailyScorecardItem
import net.kibotu.geofencerelay.model.BreachAlert
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.model.RemoteCommand
import net.kibotu.geofencerelay.relay.MqttRelayClient
import net.kibotu.geofencerelay.util.BatteryUtils
import net.kibotu.geofencerelay.util.LocationUtils
import net.kibotu.geofencerelay.util.NotificationHelper
import net.kibotu.geofencerelay.util.SoundPlayer
import java.util.UUID

class GuardianViewModel(application: Application) : AndroidViewModel(application) {

    private val relay = MqttRelayClient.shared
    val isConnected = relay.isConnected
    private var guardianEmail: String = ""

    // Initial safe zone (anchors to tracker's location once first ping arrives or custom placed)
    private val _zone = MutableStateFlow(
        GeofenceZone(
            id = UUID.randomUUID().toString().take(8),
            name = "My Safe Zone",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 300.0
        )
    )
    val zone = _zone.asStateFlow()

    private val _targetPing = MutableStateFlow<LocationPing?>(null)
    val targetPing = _targetPing.asStateFlow()

    private val _latestAlert = MutableStateFlow<BreachAlert?>(null)
    val latestAlert = _latestAlert.asStateFlow()

    private val _isBreached = MutableStateFlow(false)
    val isBreached = _isBreached.asStateFlow()

    private val _broadcastSuccess = MutableStateFlow(false)
    val broadcastSuccess = _broadcastSuccess.asStateFlow()

    private val _recenterTrigger = MutableStateFlow(0)
    val recenterTrigger = _recenterTrigger.asStateFlow()

    private val _isPlayingSound = MutableStateFlow(false)
    val isPlayingSound = _isPlayingSound.asStateFlow()

    private val _latestScorecard = MutableStateFlow<DailyScorecardItem?>(null)
    val latestScorecard = _latestScorecard.asStateFlow()

    private val _scorecardsHistory = MutableStateFlow<List<DailyScorecardItem>>(emptyList())
    val scorecardsHistory = _scorecardsHistory.asStateFlow()

    private var breachAlertJob: Job? = null
    private var hasCustomZoneLocation = false

    private fun updateBreachStatus(dist: Double) {
        val z = _zone.value
        val breached = if (z.latitude != 0.0) dist > z.radiusMeters else false
        val wasBreached = _isBreached.value
        _isBreached.value = breached

        if (breached) {
            if (!wasBreached || breachAlertJob == null || !breachAlertJob!!.isActive) {
                startRepeatingBreachAlerts()
            }
        } else {
            // Target is back inside radius! Immediately clear breach, sound, and notifications
            stopRepeatingBreachAlerts()
        }
    }

    private fun startRepeatingBreachAlerts() {
        breachAlertJob?.cancel()
        breachAlertJob = viewModelScope.launch {
            val app = getApplication<Application>()
            while (_isBreached.value) {
                val ping = _targetPing.value
                val zoneName = _zone.value.name
                val dist = ping?.distanceFromCenter ?: 0.0
                val device = ping?.deviceName ?: "Tracked Device"

                NotificationHelper.showBreachNotification(app, zoneName, dist, device)
                SoundPlayer.playFindMySound(app)

                // Repeat notification popup & alarm every 15 seconds while phone is locked/off
                delay(15_000L)
            }
        }
    }

    private fun stopRepeatingBreachAlerts() {
        breachAlertJob?.cancel()
        breachAlertJob = null
        val app = getApplication<Application>()
        NotificationHelper.cancelBreachNotification(app)
        SoundPlayer.stopSound()
        _latestAlert.value = null
    }

    fun reconnect() {
        if (guardianEmail.isNotEmpty()) {
            viewModelScope.launch {
                relay.connect(guardianEmail)
            }
        }
    }

    fun init(email: String) {
        guardianEmail = email.trim().lowercase()
        viewModelScope.launch {
            // Continuous watchdog: ensures relay reconnects automatically if network drops
            launch {
                while (isActive) {
                    if (guardianEmail.isNotEmpty() && (!relay.isConnected.value || !relay.isClientConnected)) {
                        relay.connect(guardianEmail)
                    }
                    delay(3500L)
                }
            }

            // Listen for active zone from broker
            launch {
                relay.activeZone.collect { existingZone ->
                    if (existingZone != null && existingZone.latitude != 0.0) {
                        hasCustomZoneLocation = true
                        _zone.value = existingZone
                    }
                }
            }

            // Listen for live location pings from remote target device
            launch {
                relay.latestPing.collect { ping ->
                    var z = _zone.value
                    // If safe zone center not set yet, anchor it to the tracker's initial position
                    if (!hasCustomZoneLocation && (z.latitude == 0.0 || z.longitude == 0.0) && ping.latitude != 0.0) {
                        z = z.copy(latitude = ping.latitude, longitude = ping.longitude)
                        _zone.value = z
                    }

                    val dist = if (z.latitude != 0.0 && ping.latitude != 0.0) {
                        LocationUtils.distanceMeters(ping.latitude, ping.longitude, z.latitude, z.longitude)
                    } else 0.0

                    updateBreachStatus(dist)
                    _targetPing.value = ping.copy(
                        distanceFromCenter = dist,
                        isBreach = _isBreached.value
                    )
                }
            }

            // Listen for breach alerts
            launch {
                relay.breachAlert.collect { alert ->
                    _latestAlert.value = alert
                    if (alert.status == "RESOLVED_INSIDE") {
                        updateBreachStatus(0.0)
                    } else {
                        val ping = _targetPing.value
                        val dist = ping?.distanceFromCenter ?: (alert.distanceMeters)
                        updateBreachStatus(dist)
                    }
                }
            }

            // Load existing scorecards from local history
            val app = getApplication<Application>()
            _latestScorecard.value = CognitiveHistoryManager.getLatestScorecard(app)
            _scorecardsHistory.value = CognitiveHistoryManager.getAllScorecards(app)

            // Listen for incoming cognitive scorecards from tracker
            launch {
                relay.latestScorecard.collect { scorecard ->
                    _latestScorecard.value = scorecard
                    CognitiveHistoryManager.recordScorecard(app, scorecard, shouldBroadcast = false)
                    _scorecardsHistory.value = CognitiveHistoryManager.getAllScorecards(app)
                }
            }
        }
    }

    fun updateCenter(lat: Double, lon: Double) {
        hasCustomZoneLocation = true
        _zone.value = _zone.value.copy(
            latitude = lat,
            longitude = lon,
            updatedAt = System.currentTimeMillis()
        )
        _targetPing.value?.let { ping ->
            val dist = LocationUtils.distanceMeters(ping.latitude, ping.longitude, lat, lon)
            updateBreachStatus(dist)
            _targetPing.value = ping.copy(
                distanceFromCenter = dist,
                isBreach = _isBreached.value
            )
        }
        broadcastZone()
    }

    fun updateRadius(radius: Double) {
        _zone.value = _zone.value.copy(
            radiusMeters = radius,
            updatedAt = System.currentTimeMillis()
        )
        _targetPing.value?.let { ping ->
            val dist = LocationUtils.distanceMeters(ping.latitude, ping.longitude, _zone.value.latitude, _zone.value.longitude)
            updateBreachStatus(dist)
            _targetPing.value = ping.copy(
                distanceFromCenter = dist,
                isBreach = _isBreached.value
            )
        }
        broadcastZone()
    }

    fun updateName(name: String) {
        _zone.value = _zone.value.copy(
            name = name,
            updatedAt = System.currentTimeMillis()
        )
        broadcastZone()
    }

    fun broadcastZone() {
        if (guardianEmail.isBlank()) return
        viewModelScope.launch {
            val success = relay.publishZone(guardianEmail, _zone.value)
            _broadcastSuccess.value = success
        }
    }

    fun triggerRecenter() {
        _recenterTrigger.value += 1
    }

    fun playSound() {
        _isPlayingSound.value = true
        SoundPlayer.playFindMySound(getApplication())
        val deviceId = _targetPing.value?.deviceId
        if (!deviceId.isNullOrEmpty() && guardianEmail.isNotEmpty()) {
            viewModelScope.launch {
                relay.sendCommand(
                    targetEmail = guardianEmail,
                    deviceId = deviceId,
                    command = RemoteCommand("PLAY_SOUND", guardianEmail)
                )
            }
        }
    }

    fun stopSound() {
        _isPlayingSound.value = false
        SoundPlayer.stopSound()
        val deviceId = _targetPing.value?.deviceId
        if (!deviceId.isNullOrEmpty() && guardianEmail.isNotEmpty()) {
            viewModelScope.launch {
                relay.sendCommand(
                    targetEmail = guardianEmail,
                    deviceId = deviceId,
                    command = RemoteCommand("STOP_SOUND", guardianEmail)
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRepeatingBreachAlerts()
    }
}
