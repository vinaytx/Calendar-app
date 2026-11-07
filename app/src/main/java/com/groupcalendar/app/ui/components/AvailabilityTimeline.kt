package com.groupcalendar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.data.model.TimeSlot
import com.groupcalendar.app.ui.theme.SlotBusyRed
import com.groupcalendar.app.ui.theme.SlotFreeGreen
import com.groupcalendar.app.ui.theme.TrackBackground
import com.groupcalendar.app.util.DateTimeUtils

@Composable
fun AvailabilityTimeline(
    roster: Map<String, MemberAvailability>,
    targetDateString: String,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val startHour = 8   // 8:00 AM
    val endHour = 22    // 10:00 PM
    val totalHours = endHour - startHour
    val hourWidth = 56.dp
    val totalTimelineWidth = hourWidth * totalHours

    var selectedSlotInfo by remember { mutableStateOf<Pair<String, TimeSlot>?>(null) }

    val commonFreeSlots = remember(roster, targetDateString) {
        DateTimeUtils.findCommonFreeSlots(roster.values, targetDateString)
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // Timeline Header Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🌟 Group Availability Highlights",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (commonFreeSlots.isEmpty()) {
                    Text(
                        text = "No common free hours found between 08:00 and 22:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val summary = commonFreeSlots.joinToString(", ") { slot ->
                        "${DateTimeUtils.formatShortTime(slot.start)} - ${DateTimeUtils.formatShortTime(slot.end)}"
                    }
                    Text(
                        text = "All members free at: $summary",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SlotFreeGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Scrollable Gantt/Timeline Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // 1. Time Ruler Header
                Row(modifier = Modifier.padding(start = 120.dp)) {
                    for (h in startHour until endHour) {
                        Box(
                            modifier = Modifier
                                .width(hourWidth)
                                .height(28.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = String.format("%02d:00", h),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.width(totalTimelineWidth + 120.dp))

                // 2. Mutual Free Times Track
                Row(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .height(34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 Everyone Free",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(120.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .width(totalTimelineWidth)
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TrackBackground)
                    ) {
                        for (slot in commonFreeSlots) {
                            val (offsetDp, widthDp) = calculateSlotPosition(
                                slot = slot,
                                startHour = startHour,
                                totalHours = totalHours,
                                totalWidth = totalTimelineWidth,
                                targetDateString = targetDateString
                            )
                            if (widthDp > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = offsetDp)
                                        .width(widthDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SlotFreeGreen)
                                        .clickable {
                                            selectedSlotInfo = "Common Free Window" to slot
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "FREE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.width(totalTimelineWidth + 120.dp))

                // 3. Member Availability Tracks
                if (roster.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No group member availability loaded. Pull to sync or join group.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    for ((_, member) in roster) {
                        MemberTimelineTrack(
                            member = member,
                            isMe = member.userId == currentUserId,
                            targetDateString = targetDateString,
                            startHour = startHour,
                            totalHours = totalHours,
                            totalWidth = totalTimelineWidth,
                            onSlotClick = { slot ->
                                selectedSlotInfo = member.userName to slot
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog for Tapped Slot
    selectedSlotInfo?.let { (owner, slot) ->
        AlertDialog(
            onDismissRequest = { selectedSlotInfo = null },
            title = {
                Text(text = if (slot.title.isNullOrBlank()) "Time Slot Details" else slot.title)
            },
            text = {
                Column {
                    Text(text = "Member: $owner", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Start: ${DateTimeUtils.formatDisplayTime(slot.start)}")
                    Text(text = "End:   ${DateTimeUtils.formatDisplayTime(slot.end)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSlotInfo = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun MemberTimelineTrack(
    member: MemberAvailability,
    isMe: Boolean,
    targetDateString: String,
    startHour: Int,
    totalHours: Int,
    totalWidth: androidx.compose.ui.unit.Dp,
    onSlotClick: (TimeSlot) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Member Badge & Name Label
        Row(
            modifier = Modifier.width(120.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(member.colorHex ?: "#3B82F6"))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = if (isMe) "${member.userName} (You)" else member.userName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.userId.take(8),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Timeline Bar for this member
        Box(
            modifier = Modifier
                .width(totalWidth)
                .height(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TrackBackground)
                .border(
                    width = if (isMe) 1.dp else 0.dp,
                    color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            // Filter busy slots belonging to target day
            val daySlots = member.busySlots.filter {
                DateTimeUtils.isSlotOnDate(it, targetDateString)
            }

            for (slot in daySlots) {
                val (offsetDp, widthDp) = calculateSlotPosition(
                    slot = slot,
                    startHour = startHour,
                    totalHours = totalHours,
                    totalWidth = totalWidth,
                    targetDateString = targetDateString
                )

                if (widthDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .padding(start = offsetDp)
                            .width(widthDp)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SlotBusyRed)
                            .clickable { onSlotClick(slot) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = slot.title ?: "Busy",
                            color = Color.White,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateSlotPosition(
    slot: TimeSlot,
    startHour: Int,
    totalHours: Int,
    totalWidth: androidx.compose.ui.unit.Dp,
    targetDateString: String
): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    val startDate = DateTimeUtils.parseIso(slot.start) ?: return 0.dp to 0.dp
    val endDate = DateTimeUtils.parseIso(slot.end) ?: return 0.dp to 0.dp

    val baseStart = DateTimeUtils.parseIso("${targetDateString}T${String.format("%02d:00:00", startHour)}") ?: return 0.dp to 0.dp
    val baseEnd = DateTimeUtils.parseIso("${targetDateString}T${String.format("%02d:00:00", startHour + totalHours)}") ?: return 0.dp to 0.dp

    val totalMillis = baseEnd.time - baseStart.time
    val clampedStart = startDate.time.coerceIn(baseStart.time, baseEnd.time)
    val clampedEnd = endDate.time.coerceIn(baseStart.time, baseEnd.time)

    if (clampedEnd <= clampedStart) return 0.dp to 0.dp

    val startRatio = (clampedStart - baseStart.time).toFloat() / totalMillis.toFloat()
    val endRatio = (clampedEnd - baseStart.time).toFloat() / totalMillis.toFloat()

    val offset = totalWidth * startRatio
    val width = totalWidth * (endRatio - startRatio)

    return offset to width
}
