package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.PhoneMissed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.RecentsFilter
import com.example.ui.components.InitialsAvatar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EndCallRed
import com.example.ui.theme.IncomingCallTeal
import com.example.ui.theme.MissedCallRed
import com.example.ui.theme.OutgoingCallBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentsTab(
    recents: List<CallRecord>,
    selectedFilter: RecentsFilter,
    onFilterChange: (RecentsFilter) -> Unit,
    onCallClick: (String) -> Unit,
    onDeleteCall: (CallRecord) -> Unit,
    onClearAll: () -> Unit,
    onItemClick: (CallRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("recents_tab")
    ) {
        // Filter bar (All vs Missed) and Clear action
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFilter == RecentsFilter.ALL,
                    onClick = { onFilterChange(RecentsFilter.ALL) },
                    label = { Text("All (${recents.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("filter_all_recents")
                )

                FilterChip(
                    selected = selectedFilter == RecentsFilter.MISSED,
                    onClick = { onFilterChange(RecentsFilter.MISSED) },
                    label = { Text("Missed") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MissedCallRed.copy(alpha = 0.2f),
                        selectedLabelColor = MissedCallRed
                    ),
                    modifier = Modifier.testTag("filter_missed_recents")
                )
            }

            if (recents.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.testTag("clear_recents_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear call history",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (recents.isEmpty()) {
            RecentsEmptyState(isMissedOnly = selectedFilter == RecentsFilter.MISSED)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recents_list")
            ) {
                items(recents, key = { it.id }) { record ->
                    RecentCallItem(
                        record = record,
                        onClick = { onItemClick(record) },
                        onCallClick = { onCallClick(record.phoneNumber) },
                        onDeleteClick = { onDeleteCall(record) }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Call History") },
            text = { Text("Are you sure you want to delete all call logs?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    }
                ) {
                    Text("Clear All", color = EndCallRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecentCallItem(
    record: CallRecord,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isMissed = record.type == CallType.MISSED || record.type == CallType.REJECTED

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("recent_item_${record.id}")
    ) {
        // Initials avatar
        InitialsAvatar(
            name = record.name ?: record.phoneNumber,
            size = 46.dp,
            colorIndex = (record.phoneNumber.hashCode() and 0x7FFFFFFF) % 8
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isMissed) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isMissed) MissedCallRed else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Call type icon
                CallTypeBadge(type = record.type)

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${formatCallTime(record.timestamp)} • ${record.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Call back button
        IconButton(
            onClick = onCallClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("recent_call_button_${record.id}")
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CallGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${record.displayName}",
                    tint = CallGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CallTypeBadge(type: CallType) {
    when (type) {
        CallType.INCOMING -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                contentDescription = "Incoming",
                tint = IncomingCallTeal,
                modifier = Modifier.size(15.dp)
            )
        }
        CallType.OUTGOING -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMade,
                contentDescription = "Outgoing",
                tint = OutgoingCallBlue,
                modifier = Modifier.size(15.dp)
            )
        }
        CallType.MISSED -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMissed,
                contentDescription = "Missed",
                tint = MissedCallRed,
                modifier = Modifier.size(15.dp)
            )
        }
        CallType.REJECTED -> {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Declined",
                tint = EndCallRed,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun formatCallTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val oneDay = 24 * 3600 * 1000L

    return when {
        diff < 60 * 1000L -> "Just now"
        diff < 3600 * 1000L -> "${diff / (60 * 1000L)}m ago"
        diff < oneDay -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            format.format(Date(timestamp))
        }
        diff < 2 * oneDay -> "Yesterday"
        else -> {
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            format.format(Date(timestamp))
        }
    }
}

@Composable
private fun RecentsEmptyState(isMissedOnly: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("recents_empty_state")
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMissedOnly) Icons.Outlined.PhoneMissed else Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isMissedOnly) "No Missed Calls" else "No Recent Calls",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isMissedOnly) "You have answered all incoming calls." else "Your incoming, outgoing, and missed calls will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
