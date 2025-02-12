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

val settingsKey = stringPreferencesKey("settings")

@Serializable
open class SimpleBeepPattern(
    open val freq: Int,
    open val duration: Int,
){
    fun copy(freq: Int = this.freq, duration: Int = this.duration): SimpleBeepPattern {
        return SimpleBeepPattern(freq, duration)
    }
}

@Serializable
data class BeepPattern(
    @SerialName("beep_freq") override val freq: Int,
    @SerialName("beep_duration") override val duration: Int,
    val delay: Int,
) : SimpleBeepPattern(freq, duration)

@Serializable
data class RadarSettings(
    val threatLevelPattern: List<BeepPattern>,
    val threatPassedLevelPattern: SimpleBeepPattern,
    val inRideOnly: Boolean = false,
    val enabled: Boolean = true,
) {
    companion object {
        val defaultSettings = Json.encodeToString(RadarSettings())
    }

    constructor() : this(
        listOf(BeepPattern(200, 100, 300)),
        SimpleBeepPattern(0, 0),
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

fun KarooSystemService.beep(pattern: List<BeepPattern>) {
    val playBeepPatternTones = pattern.flatMap { beepPattern ->
        listOf(
            PlayBeepPattern.Tone(beepPattern.freq, beepPattern.duration),
            PlayBeepPattern.Tone(null, beepPattern.delay)
        )
    }
    
    dispatch(PlayBeepPattern(playBeepPatternTones))
}

fun KarooSystemService.simpleBeep(beep: SimpleBeepPattern) {
    dispatch(
        PlayBeepPattern(
            listOf(PlayBeepPattern.Tone(beep.freq, beep.duration))
        )
    )
}
