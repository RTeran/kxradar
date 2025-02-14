package org.itxsvv.kxradar

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }

val settingsKey = stringPreferencesKey("settings_v2")

@Serializable
open class SimpleBeep(
    open val frequency: Int,
    open val duration: Int,
){
    fun copy(frequency: Int = this.frequency, duration: Int = this.duration): SimpleBeep {
        return SimpleBeep(frequency, duration)
    }
}

@Serializable
data class Beep(
    @SerialName("beep_frequency") override val frequency: Int,
    @SerialName("beep_duration") override val duration: Int,
    val delay: Int,
) : SimpleBeep(frequency, duration)

@Serializable
data class RadarSettings(
    val threatBeep: List<Beep>,
    val passedBeep: SimpleBeep,
    val allClearSound: Boolean = false,
    val inRideOnly: Boolean = false,
    val enabled: Boolean = true,
) {
    companion object {
        val defaultSettings = Json.encodeToString(RadarSettings())
    }

    constructor() : this(
        listOf(Beep(200, 100, 300)),
        SimpleBeep(0, 100),
        false, true
    )
}

suspend fun saveSettings(context: Context, settings: RadarSettings) {
    context.dataStore.edit { t ->
        t[settingsKey] = Json.encodeToString(settings)
    }
}

fun Context.streamSettings(): Flow<RadarSettings> {
    return dataStore.data.map { settingsJson ->
        try {
            jsonWithUnknownKeys.decodeFromString<RadarSettings>(
                settingsJson[settingsKey] ?: RadarSettings.defaultSettings
            )
        } catch (e: Throwable) {
            Log.e(KarooRadarExtension.TAG, "Failed to read preferences", e)
            RadarSettings()
        }
    }.distinctUntilChanged()
}

fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> {
    return callbackFlow {
        val listenerId = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
            trySendBlocking(event.state)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamRideState(): Flow<RideState> {
    return callbackFlow {
        val listenerId = addConsumer { rideState: RideState ->
            trySendBlocking(rideState)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.beep(pattern: List<Beep>) {
    val playBeepPatternTones = pattern.flatMap { beepPattern ->
        listOf(
            PlayBeepPattern.Tone(beepPattern.frequency, beepPattern.duration),
            PlayBeepPattern.Tone(null, beepPattern.delay)
        )
    }

    dispatch(PlayBeepPattern(playBeepPatternTones))
}

fun KarooSystemService.simpleBeep(beep: SimpleBeep) {
    dispatch(
        PlayBeepPattern(
            listOf(PlayBeepPattern.Tone(beep.frequency, beep.duration))
        )
    )
}
