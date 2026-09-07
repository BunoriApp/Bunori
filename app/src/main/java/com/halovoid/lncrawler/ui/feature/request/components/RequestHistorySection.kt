package com.halovoid.lncrawler.ui.feature.request.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.core.components.SecurityCheckDialog
import com.halovoid.lncrawler.ui.core.theme.*

fun LazyListScope.requestHistorySection(
    requestHistory: List<Request>,
    onRequestClick: (String) -> Unit,
    onGroupClick: (RequestType) -> Unit,
    onReplay: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onContinue: (String) -> Unit = {},
    onSecurityClick: (Request) -> Unit = {},
    cancellingRequestIds: Set<String> = emptySet(),
    activeActionIds: Set<String> = emptySet(),
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    allowAction: Boolean = false,
    forceUngrouped: Boolean = false
) {
    if (requestHistory.isEmpty()) return

    if (forceUngrouped) {
        items(requestHistory, key = { it.id }) { request ->
            Box(modifier = Modifier.padding(horizontal = horizontalPadding).padding(bottom = 12.dp)) {
                RequestCard(
                    request = request,
                    onClick = { onRequestClick(request.id) },
                    onReplay = { onReplay(request.id) },
                    onCancel = { onCancel(request.id) },
                    onContinue = { onContinue(request.id) },
                    onSecurityClick = { onSecurityClick(request) },
                    isCancelling = cancellingRequestIds.contains(request.id),
                    isActionPending = activeActionIds.contains(request.id),
                    allowAction = allowAction
                )
            }
        }
    } else {
        val groupedRequests = requestHistory.groupBy { it.type }

        groupedRequests.forEach { (type, requests) ->
            if (requests.size > 1) {
                item(key = "group_$type") {
                    Box(modifier = Modifier.padding(horizontal = horizontalPadding).padding(bottom = 12.dp)) {
                        RequestGroupCard(
                            type = type,
                            requests = requests,
                            onClick = { onGroupClick(type) }
                        )
                    }
                }
            } else {
                items(requests, key = { it.id }) { request ->
                    Box(modifier = Modifier.padding(horizontal = horizontalPadding).padding(bottom = 12.dp)) {
                        RequestCard(
                            request = request,
                            onClick = { onRequestClick(request.id) },
                            onReplay = { onReplay(request.id) },
                            onCancel = { onCancel(request.id) },
                            onContinue = { onContinue(request.id) },
                            onSecurityClick = { onSecurityClick(request) },
                            isCancelling = cancellingRequestIds.contains(request.id),
                            isActionPending = activeActionIds.contains(request.id),
                            allowAction = allowAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestActionHandler(
    onResolveCloudflare: (String, String) -> Unit,
    content: @Composable (onSecurityClick: (Request) -> Unit) -> Unit
) {
    var securityDialogRequest by remember { mutableStateOf<Request?>(null) }

    if (securityDialogRequest != null) {
        SecurityCheckDialog(
            novelName = securityDialogRequest!!.name,
            onConfirm = {
                val req = securityDialogRequest!!
                securityDialogRequest = null
                onResolveCloudflare(req.id, req.url ?: req.novelUrl)
            },
            onDismiss = { securityDialogRequest = null }
        )
    }

    content { securityDialogRequest = it }
}

@Composable
fun RequestGroupCard(
    type: RequestType,
    requests: List<Request>,
    onClick: () -> Unit
) {
    val totalSuccess = requests.sumOf { it.progressSuccess }
    val totalFailed = requests.sumOf { it.progressFailed }
    val totalProgress = requests.sumOf { it.progressTotal }
    
    val latestUpdate = requests.maxOfOrNull { it.updatedAt } ?: 0L

    val typeName = when (type) {
        RequestType.NOVEL_METADATA -> "Metadata"
        RequestType.CHAPTER -> "Chapters"
        RequestType.ARTIFACT -> "Exports"
        RequestType.RANGE_DOWNLOAD -> "Downloads"
        RequestType.BACKUP -> "Backups"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeName,
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
                            color = BrandAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = type.name, 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                color = BrandAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "${requests.size} jobs",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )

                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.4f)
                        )

                        Text(
                            text = "$totalSuccess/$totalProgress",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )

                        if (totalFailed > 0) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "$totalFailed failed",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        if (latestUpdate > 0) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText.copy(alpha = 0.4f)
                            )
                            Text(
                                text = android.text.format.DateUtils.getRelativeTimeSpanString(latestUpdate).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SecondaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
