package com.halovoid.lncrawler.ui.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.api.loader.SourceLoader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.font.FontWeight
import com.halovoid.lncrawler.ui.core.theme.*
import kotlinx.coroutines.launch

@Composable
fun SourceSyncScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sourceLoader = remember { SourceLoader(context) }
    
    val logs = remember { mutableStateListOf<LogEntry>() }
    var isSyncing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()

    var showDetails by remember { mutableStateOf(false) }

    fun addLog(message: String) {
        logs.add(LogEntry(message = message))
    }

    fun performSync() {
        isSyncing = true
        error = null
        scope.launch {
            try {
                sourceLoader.loadSources(onProgress = { log ->
                    addLog(log)
                })
                isSyncing = false
            } catch (e: SourceLoader.IncompatibleAppException) {
                error = "App Update Required: ${e.message}"
                addLog("Error: $error")
                isSyncing = false
            } catch (e: Exception) {
                error = e.message ?: "An unknown error occurred"
                addLog("Error: $error")
                isSyncing = false
            }
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        performSync()
    }

    val completedSources = logs.count { it.message.contains("Complete") || it.message.contains("Success") }
    val statusText = if (isSyncing) "Preparing your novel sources..." else if (error != null) "Couldn't prepare sources" else "Sources ready"

    OnboardingStep(
        title = "Preparing Sources",
        subtitle = "LNCrawler is downloading the latest source definitions so you can start crawling novels.",
        buttonText = if (error != null) "Retry" else "Get Started",
        onNext = {
            if (error != null) {
                performSync()
            } else {
                onComplete()
            }
        },
        isNextEnabled = !isSyncing
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = BrandAccent,
                        strokeWidth = 4.dp,
                        trackColor = BrandAccent.copy(alpha = 0.1f)
                    )
                } else if (error != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) MaterialTheme.colorScheme.error else PrimaryText,
                fontWeight = FontWeight.Bold
            )
            
            if (completedSources > 0 && error == null) {
                Text(
                    text = "$completedSources sources updated",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            TextButton(
                onClick = { showDetails = !showDetails },
                colors = ButtonDefaults.textButtonColors(contentColor = SecondaryText)
            ) {
                Text(
                    text = if (showDetails) "Hide Details" else "Show Details",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (showDetails) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs, key = { it.id }) { logEntry ->
                            Text(
                                text = logEntry.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = if (logEntry.message.startsWith("Error")) MaterialTheme.colorScheme.error else SecondaryText
                            )
                        }
                    }
                }
            }
            
            if (error != null) {
                TextButton(onClick = onComplete, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Skip for now",
                        color = SecondaryText.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Immutable
data class LogEntry(
    val id: Long = System.nanoTime(),
    val message: String
)
