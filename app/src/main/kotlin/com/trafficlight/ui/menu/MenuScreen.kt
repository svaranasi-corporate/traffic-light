package com.trafficlight.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trafficlight.R

private val BUTTON_WIDTH = 200.dp
private val BUTTON_SPACING = 16.dp
private val TITLE_BUTTON_SPACING = 48.dp

/**
 * Entry-point screen of the app. Displays the app title and two navigation buttons
 * to start the traffic light or open settings (FR-1.1, FR-1.2, FR-1.3, FR-1.4).
 */
@Composable
fun MenuScreen(
    onStartClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // App title / branding (FR-1.2)
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(TITLE_BUTTON_SPACING))

        // Start button (FR-1.3)
        Button(
            onClick = onStartClick,
            modifier =
                Modifier
                    .width(BUTTON_WIDTH)
                    .semantics { contentDescription = "Start traffic light" },
        ) {
            Text(text = stringResource(R.string.menu_start))
        }

        Spacer(modifier = Modifier.height(BUTTON_SPACING))

        // Options button (FR-1.4)
        Button(
            onClick = onOptionsClick,
            modifier =
                Modifier
                    .width(BUTTON_WIDTH)
                    .semantics { contentDescription = "Open settings" },
        ) {
            Text(text = stringResource(R.string.menu_options))
        }
    }
}
