package com.halovoid.bunori.ui.feature.request.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.domain.models.Request
import com.halovoid.bunori.ui.core.components.ConfirmCancelDialog
import com.halovoid.bunori.ui.core.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactRequestItem(
    request: Request,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val formattedDate = remember(request.createdAt, locale) {
        SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(request.createdAt))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (request.progressTotal > 0) {
                        Text(
                            text = if (request.progressSuccess > 0) {
                                "${request.progressSuccess}/${request.progressTotal} tasks"
                            } else {
                                "${request.progressTotal} tasks"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.4f)
                        )
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            StatusIndicator(request.status)
        }

        HorizontalDivider(
            color = BorderColor.copy(alpha = 0.25f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun StatusIndicator(
    status: RequestStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (status) {
            RequestStatus.RUNNING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.8.dp,
                    color = PrimaryText
                )
                Text("Running", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.PAUSED -> {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Paused",
                    tint = PrimaryText,
                    modifier = Modifier.size(12.dp)
                )
                Text("Paused", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = PrimaryText,
                    modifier = Modifier.size(12.dp)
                )
                Text("Completed", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Failed",
                    tint = PrimaryText,
                    modifier = Modifier.size(12.dp)
                )
                Text("Failed", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.CANCELLED -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelled",
                    tint = PrimaryText,
                    modifier = Modifier.size(12.dp)
                )
                Text("Cancelled", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.CANCELLING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.8.dp,
                    color = PrimaryText
                )
                Text("Cancelling", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.BLOCKED -> {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Blocked",
                    tint = PrimaryText,
                    modifier = Modifier.size(12.dp)
                )
                Text("Blocked", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
            RequestStatus.PENDING -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Queued",
                    tint = PrimaryText,
                    modifier = Modifier.size(12.dp)
                )
                Text("Queued", style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RequestCard(
    request: Request,
    onClick: (() -> Unit)? = null,
    onReplay: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onContinue: (() -> Unit)? = null,
    onSecurityClick: (() -> Unit)? = null,
    allowAction: Boolean = false,
    isCancelling: Boolean = false,
    isActionPending: Boolean = false,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val formattedDate = remember(request.createdAt, locale) {
        SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(request.createdAt))
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog && onCancel != null) {
        ConfirmCancelDialog(
            title = "Cancel Batch?",
            message = "Are you sure you want to stop \"${request.name}\"? Any completed progress will be preserved.",
            onConfirm = {
                showCancelDialog = false
                onCancel()
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    val progress = if (request.progressTotal > 0) {
        request.progressSuccess.toFloat() / request.progressTotal
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null && !isCancelling) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = request.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = request.type.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = SecondaryText,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (request.priority > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = SecondaryText.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "High Priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText.copy(alpha = 0.7f),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.4f)
                        )

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (request.status == RequestStatus.BLOCKED && onSecurityClick != null) {
                        IconButton(
                            onClick = onSecurityClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Security Check Needed",
                                tint = BrandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    StatusIndicator(request.status)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BrandAccent,
                    trackColor = DarkSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (request.progressTotal > 0) {
                            "${request.progressSuccess}/${request.progressTotal} tasks"
                        } else {
                            "0 tasks"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!request.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ErrorRed.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = request.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (allowAction) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Cancelling",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed,
                            fontSize = 10.sp
                        )
                    } else if (request.status == RequestStatus.RUNNING || request.status == RequestStatus.PENDING || request.status == RequestStatus.BLOCKED) {
                        if (onCancel != null) {
                            TextButton(
                                onClick = { showCancelDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (request.status == RequestStatus.PAUSED) {
                        if (onContinue != null) {
                            TextButton(
                                onClick = onContinue,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = BrandAccent)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onCancel != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { showCancelDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (request.status == RequestStatus.SUCCESS || request.status == RequestStatus.FAILED || request.status == RequestStatus.CANCELLED) {
                        if (onReplay != null) {
                            TextButton(
                                onClick = onReplay,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = SecondaryText)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Replay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
