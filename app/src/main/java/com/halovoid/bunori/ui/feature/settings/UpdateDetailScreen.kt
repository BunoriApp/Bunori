package com.halovoid.bunori.ui.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.ui.core.components.FlowingSineWave
import com.halovoid.bunori.ui.core.components.MarkdownContent
import com.halovoid.bunori.ui.core.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDetailScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Update Details", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = PrimaryText,
                    navigationIconContentColor = PrimaryText
                )
            )
        }
    ) { innerPadding ->
        val state = updateState
        if (state is AppUpdateState.UpdateAvailable || state is AppUpdateState.Downloading || state is AppUpdateState.ReadyToInstall) {
            val availableState = when (state) {
                is AppUpdateState.UpdateAvailable -> state
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    val tagName = when (state) {
                        is AppUpdateState.UpdateAvailable -> state.tagName
                        else -> "Latest Version"
                    }

                    val publishedDate = remember(availableState?.publishedAt) {
                        availableState?.publishedAt?.let {
                            try {
                                val zdt = ZonedDateTime.parse(it)
                                zdt.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH))
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        Text(
                            text = tagName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        if (publishedDate != null) {
                            Text(
                                text = publishedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    val body = availableState?.releaseNotes
                    if (!body.isNullOrBlank()) {
                        MarkdownContent(body)
                    } else {
                        Text(
                            text = "No detailed release notes available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText.copy(alpha = 0.5f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(120.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = DarkBackground,
                    tonalElevation = 4.dp
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        when (state) {
                            is AppUpdateState.UpdateAvailable -> {
                                Button(
                                    onClick = { 
                                        if (state.apkDownloadUrl != null) {
                                            viewModel.startUpdateDownload(state.apkDownloadUrl)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandAccent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Download Update", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            is AppUpdateState.Downloading, is AppUpdateState.ReadyToInstall, is AppUpdateState.Installing -> {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val isReadyOrInstalling = state is AppUpdateState.ReadyToInstall || state is AppUpdateState.Installing
                                
                                val waveColor1 by animateColorAsState(
                                    targetValue = if (isReadyOrInstalling) SuccessGreen.copy(alpha = 0.2f) else BrandAccent.copy(alpha = 0.2f),
                                    animationSpec = tween(1000),
                                    label = "waveColor1"
                                )
                                val waveColor2 by animateColorAsState(
                                    targetValue = if (isReadyOrInstalling) SuccessGreen.copy(alpha = 0.4f) else BrandAccent.copy(alpha = 0.4f),
                                    animationSpec = tween(1000),
                                    label = "waveColor2"
                                )
                                
                                val fillProgress by animateFloatAsState(
                                    targetValue = if (isReadyOrInstalling) 0.8f else 0.5f,
                                    animationSpec = tween(1200, easing = FastOutSlowInEasing),
                                    label = "fillProgress"
                                )
                                
                                val text = when (state) {
                                    is AppUpdateState.Downloading -> "Downloading Update..."
                                    is AppUpdateState.ReadyToInstall -> "Install Now"
                                    is AppUpdateState.Installing -> "Installing..."
                                    else -> ""
                                }
                                
                                val clickableModifier = if (state is AppUpdateState.ReadyToInstall) {
                                    Modifier.clickable { viewModel.installUpdate(context, state.uri) }
                                } else {
                                    Modifier
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant)
                                        .then(clickableModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlowingSineWave(
                                        modifier = Modifier.fillMaxSize(),
                                        color = waveColor1,
                                        amplitude = 12f,
                                        wavelength = 240f,
                                        durationMillis = 2000,
                                        reverse = false,
                                        fillProgress = fillProgress
                                    )
                                    FlowingSineWave(
                                        modifier = Modifier.fillMaxSize(),
                                        color = waveColor2,
                                        amplitude = 8f,
                                        wavelength = 180f,
                                        durationMillis = 1500,
                                        reverse = true,
                                        fillProgress = fillProgress
                                    )
                                    Text(
                                        text = text,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandAccent)
            }
        }
    }
}
