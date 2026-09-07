package com.halovoid.lncrawler.ui.feature.request.components

import androidx.compose.foundation.clickable
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
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.core.components.ConfirmCancelDialog
import com.halovoid.lncrawler.ui.core.components.ProgressIndicator
import com.halovoid.lncrawler.ui.core.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RequestCard(
    request: Request,
    onClick: () -> Unit,
    onReplay: () -> Unit,
    onCancel: () -> Unit,
    onContinue: (() -> Unit)? = null,
    onSecurityClick: (() -> Unit)? = null,
    allowAction: Boolean = true,
    isCancelling: Boolean = false,
    isActionPending: Boolean = false,
) {
    val isWorkFinished = (request.progressSuccess + request.progressFailed + request.progressCancelled) == request.progressTotal

    val isProcessing = when (request.rstatus) {
        RequestStatus.RUNNING, RequestStatus.PENDING, RequestStatus.BLOCKED, RequestStatus.CANCELLING -> true
        RequestStatus.SUCCESS -> !isWorkFinished
        else -> isActionPending
    }
    
    val canContinue = (request.rstatus == RequestStatus.CANCELLED || request.rstatus == RequestStatus.FAILED) && (request.progressSuccess < request.progressTotal)

    val displayStatus = if (isProcessing && request.rstatus != RequestStatus.CANCELLING && request.rstatus != RequestStatus.BLOCKED) {
        RequestStatus.RUNNING
    } else {
        request.rstatus
    }
    val locale = LocalConfiguration.current.locales[0]

    val formattedDate = remember(request.createdAt, locale) {
        SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(request.createdAt))
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        ConfirmCancelDialog(
            title = "Cancel Request?",
            message = "Are you sure you want to stop \"${request.name}\"? Any progress made will be preserved, but the current task will stop.",
            onConfirm = {
                showCancelDialog = false
                onCancel()
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isCancelling) { onClick() },
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
                Column(modifier = Modifier.weight(1f)) {
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
                    if (request.rstatus == RequestStatus.BLOCKED && onSecurityClick != null) {
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
                    StatusIndicator(displayStatus)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val progress = if (request.progressTotal > 0) {
                (request.progressSuccess).toFloat() / request.progressTotal
            } else 0f

            Column {
                ProgressIndicator(
                    success = request.progressSuccess,
                    failed = request.progressFailed,
                    cancelled = request.progressCancelled,
                    total = request.progressTotal,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (displayStatus == RequestStatus.SUCCESS) SuccessGreen.copy(alpha = 0.8f) else PrimaryText.copy(alpha = 0.6f),
                        fontWeight = if (displayStatus == RequestStatus.SUCCESS) FontWeight.Bold else FontWeight.Medium
                    )

                    if (allowAction) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            } else if (isProcessing) {
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
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (canContinue && onContinue != null) {
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
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    
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

            if (!request.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ErrorRed.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = request.error,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: RequestStatus) {
    when (status) {
        RequestStatus.SUCCESS -> Icon(
            imageVector = Icons.Default.CheckCircle, 
            contentDescription = "Success", 
            tint = SuccessGreen.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        RequestStatus.FAILED -> Icon(
            imageVector = Icons.Default.Error, 
            contentDescription = "Failed", 
            tint = ErrorRed.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        RequestStatus.CANCELLED -> Icon(
            imageVector = Icons.Default.Cancel, 
            contentDescription = "Cancelled", 
            tint = SecondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        RequestStatus.CANCELLING -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = ErrorRed.copy(alpha = 0.7f)
        )
        RequestStatus.RUNNING -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp), 
            strokeWidth = 2.dp, 
            color = BrandAccent
        )
        RequestStatus.BLOCKED -> Icon(
            imageVector = Icons.Default.Security, 
            contentDescription = "Blocked", 
            tint = BrandAccent,
            modifier = Modifier.size(20.dp)
        )
        else -> Icon(
            imageVector = Icons.Default.Schedule, 
            contentDescription = "Pending", 
            tint = SecondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
