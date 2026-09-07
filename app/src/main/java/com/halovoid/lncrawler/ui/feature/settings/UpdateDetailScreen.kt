package com.halovoid.lncrawler.ui.feature.settings

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.core.theme.*
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

@Composable
fun MarkdownContent(markdown: String) {
    val lines = markdown.lines()
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    Text(
                        text = trimmed.removePrefix("###").trim(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                trimmed.startsWith("##") -> {
                    if (!trimmed.contains("v", ignoreCase = true)) {
                        Text(
                            text = trimmed.removePrefix("##").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                        )
                    }
                }
                trimmed.startsWith("*") || trimmed.startsWith("-") -> {
                    Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
                        Text("•", color = SecondaryText, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            text = parseBasicMarkdown(trimmed.substring(1).trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            lineHeight = 22.sp
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseBasicMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

fun parseBasicMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val regex = "\\*\\*(.*?)\\*\\*".toRegex()
        val matches = regex.findAll(text)
        
        matches.forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last
            
            append(text.substring(currentIndex, start))
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryText)) {
                append(matchResult.groupValues[1])
            }
            
            currentIndex = end + 1
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

@Composable
fun FlowingSineWave(
    modifier: Modifier = Modifier,
    color: Color,
    amplitude: Float,
    wavelength: Float,
    durationMillis: Int,
    reverse: Boolean,
    fillProgress: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = if (reverse) (2 * Math.PI).toFloat() else 0f,
        targetValue = if (reverse) 0f else (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        path.moveTo(0f, height)
        for (x in 0..width.toInt() step 4) {
            val relativeX = x.toFloat()
            val y = (height * (1f - fillProgress)) + Math.sin((relativeX / wavelength * 2 * Math.PI) - phase).toFloat() * amplitude
            path.lineTo(relativeX, y)
        }
        path.lineTo(width, height)
        path.close()

        drawPath(
            path = path,
            color = color
        )
    }
}
