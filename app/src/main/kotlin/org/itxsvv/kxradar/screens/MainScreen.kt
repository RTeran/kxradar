package org.itxsvv.kxradar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.launch
import org.itxsvv.kxradar.*

@Composable
fun MainScreen() {
    val pattern = remember { Regex("^\\d*\\d*\$") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val karooSystem = remember { KarooSystemService(ctx) }
    var savedDialogVisible by remember { mutableStateOf(false) }

    var uiThreatLevelPattern by remember { mutableStateOf(emptyList<BeepPattern>()) }
    var uiThreatPassedLevelPattern by remember { mutableStateOf<SimpleBeepPattern?>(null) }
    var uiAllClearSoundEnabled by remember { mutableStateOf(true) }
    var uiInRideOnlyEnabled by remember { mutableStateOf(true) }
    var uiBeepEnabled by remember { mutableStateOf(true) }

    val maxBeeps = 5

    fun saveUISettings() {
        scope.launch {
            val radarSettings = RadarSettings(
                threatLevelPattern = uiThreatLevelPattern,
                threatPassedLevelPattern = uiThreatPassedLevelPattern ?: SimpleBeepPattern(0, 0),
                allClearSound = uiAllClearSoundEnabled,
                inRideOnly = uiInRideOnlyEnabled,
                enabled = uiBeepEnabled
            )

            saveSettings(ctx, radarSettings)
        }
    }

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->
            uiThreatLevelPattern = settings.threatLevelPattern
            uiThreatPassedLevelPattern = settings.threatPassedLevelPattern
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            uiThreatLevelPattern.forEachIndexed { index, beepPattern ->
                BeepPatternFields(
                    beepPattern = beepPattern,
                    index = index,
                    pattern = pattern,
                    onFreqChange = { newFreq ->
                        uiThreatLevelPattern = uiThreatLevelPattern.toMutableList().apply {
                            this[index] = BeepPattern(
                                freq = newFreq,
                                duration = this[index].duration,
                                delay = this[index].delay
                            )
                        }
                    },
                    onDurationChange = { newDuration ->
                        uiThreatLevelPattern = uiThreatLevelPattern.toMutableList().apply {
                            this[index] = BeepPattern(
                                freq = this[index].freq,
                                duration = newDuration,
                                delay = this[index].delay
                            )
                        }

                    },
                    onDelayChange = { newDelay ->
                        uiThreatLevelPattern = uiThreatLevelPattern.toMutableList().apply {
                            this[index] = this[index].copy(delay = newDelay)
                        }
                    },
                    showRemove = uiThreatLevelPattern.size > 1,
                    onRemoveBeep = {
                        uiThreatLevelPattern = uiThreatLevelPattern.toMutableList().apply {
                            removeAt(index)
                        }
                    },
                    maxBeeps = maxBeeps
                )
            }
            if (uiThreatLevelPattern.size < maxBeeps) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp, 0.dp)
                        .height(50.dp),
                    onClick = {
                        uiThreatLevelPattern = uiThreatLevelPattern.toMutableList().apply {
                            this[lastIndex] = this[lastIndex].copy(delay = 300)
                            add(BeepPattern(200, 100, 0))
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
                        karooSystem.beep(uiThreatLevelPattern)
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
            if (uiThreatPassedLevelPattern != null) {
                BeepPatternFields(
                    beepPattern = uiThreatPassedLevelPattern!!,
                    index = 0,
                    pattern = pattern,
                    onFreqChange = { newFreq ->
                        uiThreatPassedLevelPattern = uiThreatPassedLevelPattern!!.copy(freq = newFreq)
                    },
                    onDurationChange = { newDuration ->
                        uiThreatPassedLevelPattern =
                            uiThreatPassedLevelPattern!!.copy(duration = newDuration)
                    },
                )

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp, 0.dp)
                        .height(50.dp),
                    onClick = {
                        scope.launch {
                            karooSystem.simpleBeep(uiThreatPassedLevelPattern!!)
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
                    scope.launch {
                        saveUISettings()
                    }
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
fun BeepPatternFields(
    beepPattern: SimpleBeepPattern,
    index: Int,
    pattern: Regex,
    onFreqChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDelayChange: (Int) -> Unit = {},
    showRemove: Boolean = false,
    onRemoveBeep: () -> Unit = {},
    maxBeeps: Int = 5,
) {
    Card(
        modifier = Modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    .padding(0.dp, 0.dp, 0.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var freqText by remember { mutableStateOf(beepPattern.freq.toString()) }
                OutlinedTextField(
                    value = freqText,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    onValueChange = { newText ->
                        if (newText.matches(pattern)) {
                            onFreqChange(if (newText.isEmpty()) 0 else newText.toInt())
                            freqText = newText
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && beepPattern.freq == 0) {
                                freqText = "0"
                            }
                        },
                    singleLine = true,
                    label = { Text(text = "Freq.") },
                )
                var durationText by remember { mutableStateOf(beepPattern.duration.toString()) }
                OutlinedTextField(
                    value = durationText,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    onValueChange = { newText ->
                        if (newText.matches(pattern)) {
                            onDurationChange(if (newText.isEmpty()) 0 else newText.toInt())
                            durationText = newText
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && beepPattern.duration == 0) {
                                durationText = "0"
                            }
                        },
                    singleLine = true,
                    label = { Text(text = "Dur.") },
                )
            }
            if (beepPattern is BeepPattern && index < maxBeeps - 1) {
                var delayText by remember { mutableStateOf(beepPattern.delay.toString()) }
                OutlinedTextField(
                    value = delayText,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    onValueChange = { newText ->
                        if (newText.matches(pattern)) {
                            onDelayChange(if (newText.isEmpty()) 0 else newText.toInt())
                            delayText = newText
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && beepPattern.delay == 0) {
                                delayText = "0"
                            }
                        },
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
}
