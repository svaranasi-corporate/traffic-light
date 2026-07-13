package com.trafficlight.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trafficlight.R
import com.trafficlight.data.PreferencesRepository
import com.trafficlight.data.TimingPreferences
import kotlin.math.roundToInt

private val HORIZONTAL_PADDING = 16.dp
private val SECTION_SPACING = 24.dp
private val LABEL_WIDTH = 48.dp

/**
 * Settings screen allowing the user to adjust per-phase traffic light durations.
 *
 * Satisfies FR-6.1 through FR-6.7:
 * - Material 3 sliders for RED (3–60 s), GREEN (3–60 s), YELLOW (1–10 s)
 * - Numeric value label beside each slider
 * - Each slider change immediately persisted via [PreferencesRepository]
 * - "Reset to Defaults" button restores factory defaults
 * - TopAppBar with back arrow returning to MenuScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }

    // Load saved preferences once; sliders are driven by local state
    val saved = remember { repository.getTimingPreferences() }

    var redSeconds by remember { mutableIntStateOf(saved.redDurationSeconds) }
    var greenSeconds by remember { mutableIntStateOf(saved.greenDurationSeconds) }
    var yellowSeconds by remember { mutableIntStateOf(saved.yellowDurationSeconds) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Navigate back to menu"
                            },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = HORIZONTAL_PADDING),
        ) {
            Spacer(modifier = Modifier.height(SECTION_SPACING))

            // RED slider — range 3–60 s (FR-6.1)
            DurationSlider(
                label = stringResource(R.string.settings_red_label),
                value = redSeconds,
                valueRange = TimingPreferences.RED_GREEN_MIN..TimingPreferences.RED_GREEN_MAX,
                onValueChange = { newValue ->
                    redSeconds = newValue
                    repository.saveTimingPreferences(
                        TimingPreferences(
                            redDurationSeconds = newValue,
                            greenDurationSeconds = greenSeconds,
                            yellowDurationSeconds = yellowSeconds,
                        ),
                    )
                },
            )

            Spacer(modifier = Modifier.height(SECTION_SPACING))

            // GREEN slider — range 3–60 s (FR-6.2)
            DurationSlider(
                label = stringResource(R.string.settings_green_label),
                value = greenSeconds,
                valueRange = TimingPreferences.RED_GREEN_MIN..TimingPreferences.RED_GREEN_MAX,
                onValueChange = { newValue ->
                    greenSeconds = newValue
                    repository.saveTimingPreferences(
                        TimingPreferences(
                            redDurationSeconds = redSeconds,
                            greenDurationSeconds = newValue,
                            yellowDurationSeconds = yellowSeconds,
                        ),
                    )
                },
            )

            Spacer(modifier = Modifier.height(SECTION_SPACING))

            // YELLOW slider — range 1–10 s (FR-6.3)
            DurationSlider(
                label = stringResource(R.string.settings_yellow_label),
                value = yellowSeconds,
                valueRange = TimingPreferences.YELLOW_MIN..TimingPreferences.YELLOW_MAX,
                onValueChange = { newValue ->
                    yellowSeconds = newValue
                    repository.saveTimingPreferences(
                        TimingPreferences(
                            redDurationSeconds = redSeconds,
                            greenDurationSeconds = greenSeconds,
                            yellowDurationSeconds = newValue,
                        ),
                    )
                },
            )

            Spacer(modifier = Modifier.height(SECTION_SPACING * 2))

            // Reset to Defaults button (FR-6.6)
            Button(
                onClick = {
                    repository.resetToDefaults()
                    val defaults = repository.getTimingPreferences()
                    redSeconds = defaults.redDurationSeconds
                    greenSeconds = defaults.greenDurationSeconds
                    yellowSeconds = defaults.yellowDurationSeconds
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Reset all durations to default values" },
            ) {
                Text(text = stringResource(R.string.settings_reset_defaults))
            }
        }
    }
}

/**
 * A labeled row containing a [Slider] and a fixed-width numeric value label.
 *
 * @param label         Human-readable name shown above the slider row.
 * @param value         Current integer duration in seconds.
 * @param valueRange    Valid integer range for this slider.
 * @param onValueChange Callback fired when the user finishes moving the slider.
 */
@Composable
private fun DurationSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val floatRange = valueRange.first.toFloat()..valueRange.last.toFloat()
    val steps = (valueRange.last - valueRange.first) - 1

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = floatRange,
                steps = steps,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$label slider" },
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Numeric value label beside the slider (FR-6.4)
            Text(
                text = "$value${stringResource(R.string.settings_seconds_suffix)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .width(LABEL_WIDTH)
                        .wrapContentWidth(Alignment.End),
            )
        }
    }
}
