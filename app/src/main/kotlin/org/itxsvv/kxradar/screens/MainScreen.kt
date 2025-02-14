package org.itxsvv.kxradar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.itxsvv.kxradar.Beep
import org.itxsvv.kxradar.RadarSettings
import org.itxsvv.kxradar.SimpleBeep
import org.itxsvv.kxradar.beep
import org.itxsvv.kxradar.saveSettings
import org.itxsvv.kxradar.streamSettings
import androidx.compose.ui.text.input.ImeAction
import org.itxsvv.kxradar.simpleBeep


@Composable
fun MainScreen() {
    val pattern = remember { Regex("^\\d*\\d*\$") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val karooSystem = remember { KarooSystemService(ctx) }
    var savedDialogVisible by remember { mutableStateOf(false) }

    var uiThreatBeep by remember { mutableStateOf(emptyList<Beep>()) }
    var uiPassedBeep by remember { mutableStateOf<SimpleBeep?>(null) }
    var uiAllClearSoundEnabled by remember { mutableStateOf(true) }
    var uiInRideOnlyEnabled by remember { mutableStateOf(true) }
    var uiBeepEnabled by remember { mutableStateOf(true) }

    var maxBeeps = 5

    fun saveUISettings() {
        scope.launch {
            val radarSettings = RadarSettings(
                threatBeep = uiThreatBeep,
                passedBeep = uiPassedBeep ?: SimpleBeep(0, 0),
                allClearSound = uiAllClearSoundEnabled,
                inRideOnly = uiInRideOnlyEnabled,
                enabled = uiBeepEnabled
            )
            saveSettings(ctx, radarSettings)
        }
    }

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->
            uiThreatBeep = settings.threatBeep
            uiPassedBeep = settings.passedBeep
            uiAllClearSoundEnabled = settings.allClearSound
            uiInRideOnlyEnabled = settings.inRideOnly
            uiBeepEnabled = settings.enabled
        }
    }

    LaunchedEffect(Unit) {
        karooSystem.connect()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .clickable { focusManager.clearFocus() },
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(1.dp))
            Text("Threat sound")
            uiThreatBeep.forEachIndexed { index, beep ->
                DrawBeepPanel(
                    beep,
                    pattern,
                    onDurationChange = { newDur ->
                        uiThreatBeep = uiThreatBeep.toMutableList().apply {
                            this[index] = Beep(
                                frequency = this[index].frequency,
                                duration = newDur,
                                delay = this[index].delay
                            )
                        }
                    },
                    onFreqChange = { newFreq ->
                        uiThreatBeep = uiThreatBeep.toMutableList().apply {
                            this[index] = Beep(
                                frequency = newFreq,
                                duration = this[index].duration,
                                delay = this[index].delay
                            )
                        }
                    },
                    maxBeeps = maxBeeps,
                    index = index,
                    showRemove = uiThreatBeep.size > 1,
                    onRemoveBeep = {
                        uiThreatBeep = uiThreatBeep.toMutableList().apply {
                            removeAt(index)
                        }
                    },
                )
            }
            if (uiThreatBeep.size < maxBeeps) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp, 0.dp)
                        .height(50.dp),
                    onClick = {
                        uiThreatBeep = uiThreatBeep.toMutableList().apply {
                            this[lastIndex] = this[lastIndex].copy(delay = 300)
                            add(Beep(200, 100, 0))
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add beep")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add beep")
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp, 0.dp)
                    .height(50.dp),
                onClick = {
                    scope.launch {
                        karooSystem.beep(uiThreatBeep)
                    }
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play")
            }
        }
        HorizontalDivider(thickness = 2.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(8.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = uiAllClearSoundEnabled,
                    onCheckedChange = {
                        uiAllClearSoundEnabled = it
                    }
                )
                Text(modifier = Modifier.weight(1f), text = "All clear sound")
            }
            if (uiPassedBeep != null) {
                DrawBeepPanel(
                    beep = uiPassedBeep!!,
                    index = 0,
                    pattern = pattern,
                    onFreqChange = { newFreq ->
                        uiPassedBeep = uiPassedBeep!!.copy(frequency = newFreq)
                    },
                    onDurationChange = { newDuration ->
                        uiPassedBeep =
                            uiPassedBeep!!.copy(duration = newDuration)
                    },
                )

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp, 0.dp)
                        .height(50.dp),
                    onClick = {
                        scope.launch {
                            karooSystem.simpleBeep(uiPassedBeep!!)
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play")
                }
            }
        }
        HorizontalDivider(thickness = 2.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(16.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = uiInRideOnlyEnabled,
                    onCheckedChange = {
                        uiInRideOnlyEnabled = it
                    }
                )
                Text(modifier = Modifier.weight(1f), text = "In-ride only")
            }
        }
        HorizontalDivider(thickness = 2.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 0.dp)
                    .height(50.dp),
                onClick = {
                    scope.launch {
                        saveUISettings()
                        savedDialogVisible = true
                    }
                }) {
                Icon(Icons.Default.Done, contentDescription = "Save")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        }
        HorizontalDivider(thickness = 2.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Switch(
                checked = uiBeepEnabled,
                onCheckedChange = {
                    uiBeepEnabled = it
                }
            )
            Text(modifier = Modifier.weight(1f), text = "Enabled")
        }
        Spacer(modifier = Modifier.size(2.dp))

        if (savedDialogVisible) {
            AlertDialog(onDismissRequest = { savedDialogVisible = false },
                confirmButton = {
                    Button(onClick = {
                        savedDialogVisible = false
                    }) { Text("OK") }
                },
                text = { Text("Settings saved successfully.") }
            )
        }
    }
}

@Composable
fun DrawBeepPanel(
    beep: SimpleBeep,
    pattern: Regex,
    onFreqChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDelayChange: (Int) -> Unit = {},
    maxBeeps: Int = 5,
    index: Int = 0,
    showRemove: Boolean = false,
    onRemoveBeep: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            OutlinedTextField(
                value = beep.frequency.toString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                onValueChange = { newFreq ->
                    if (!newFreq.isEmpty() && newFreq.matches(pattern)) {
                        onFreqChange(newFreq.replace("\n", "").toInt())
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(text = "Freq.") }
            )
            OutlinedTextField(
                value = beep.duration.toString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                onValueChange = { newDuration ->
                    if (!newDuration.isEmpty() && newDuration.matches(pattern)) {
                        onDurationChange((newDuration.replace("\n", "")).toInt())
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(text = "Dur.") }
            )
        }

        if (beep is Beep && index < maxBeeps - 1) {
            OutlinedTextField(
                value = beep.delay.toString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                onValueChange = { newDelay ->
                    if (!newDelay.isEmpty() && newDelay.matches(pattern)) {
                        onDelayChange((newDelay.replace("\n", "")).toInt())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                label = { Text(text = "Delay") },
            )
        }
        if (showRemove) {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 8.dp, 0.dp, 4.dp)
                    .height(50.dp),
                onClick = { onRemoveBeep() }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}





