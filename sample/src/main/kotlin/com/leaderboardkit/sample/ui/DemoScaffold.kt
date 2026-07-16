package com.leaderboardkit.sample.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Shared chrome for every demo screen: a back-enabled top bar, a snackbar host [onShowError] can feed, and an optional "submit a random score" FAB. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onSubmitRandomScore: (() -> Unit)? = null,
    onSeedBoard: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (onSubmitRandomScore != null || onSeedBoard != null) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    onSeedBoard?.let {
                        ExtendedFloatingActionButton(
                            onClick = it,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text("New User") }
                        )
                    }
                    onSubmitRandomScore?.let {
                        ExtendedFloatingActionButton(
                            onClick = it,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text("Submit") }
                        )
                    }
                }
            }
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

/** A plausible-looking random score for the demo's "submit score" FAB. */
fun randomDemoScore(current: Long? = null): Long {
    val base = current ?: Random.nextLong(100, 1000)
    return base + Random.nextLong(10, 500)
}
