package com.groupcalendar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.ui.theme.SlotBusyRed
import com.groupcalendar.app.ui.theme.SlotFreeGreen
import com.groupcalendar.app.ui.theme.SlotFreeGreenLight
import com.groupcalendar.app.ui.theme.TrackBackground
import com.groupcalendar.app.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CalendarViewMode { DAY, WEEK }

private enum class SortMode(val label: String) { NAME("A–Z"), MOST_FREE("Free"), BUSIEST("Busy") }

private const val START_HOUR = 8
private const val END_HOUR = 22
private val MEMBER_COL_WIDTH = 92.dp
private val FREE_COL_WIDTH = 36.dp

/**
 * A one-screen tabular roster: members as rows, time as columns (hours for Day, days for Week).
 * Members can be shown/hidden via the filter chips and the roster can be re-sorted by tapping
 * the "Member" header, so no second screen or horizontal scroll is needed.
 */
@Composable
fun GroupCalendarTable(
    roster: Map<String, MemberAvailability>,
    myProfile: MemberAvailability,
    selectedDate: Date,
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allMembers = remember(roster, myProfile) {
        val ordered = LinkedHashMap<String, MemberAvailability>()
        ordered[myProfile.userId] = myProfile
        roster.values.forEach { if (it.userId != myProfile.userId) ordered[it.userId] = it }
        ordered.values.toList()
    }
    val memberIds = allMembers.map { it.userId }

    var selectedIds by remember(memberIds) { mutableStateOf(memberIds.toSet()) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }

    val weekDates = remember(selectedDate) { DateTimeUtils.datesOfWeek(selectedDate) }
    val targetDateString = remember(selectedDate) { DateTimeUtils.formatDateOnly(selectedDate) }

    fun freeMinutes(member: MemberAvailability): Int = if (viewMode == CalendarViewMode.WEEK) {
        weekDates.sumOf {
            DateTimeUtils.freeMinutesForDate(member, DateTimeUtils.formatDateOnly(it), START_HOUR, END_HOUR)
        }
    } else {
        DateTimeUtils.freeMinutesForDate(member, targetDateString, START_HOUR, END_HOUR)
    }

    val sortedVisibleMembers = allMembers
        .filter { selectedIds.contains(it.userId) }
        .let { visible ->
            when (sortMode) {
                SortMode.NAME -> visible.sortedBy { it.userName.lowercase() }
                SortMode.MOST_FREE -> visible.sortedByDescending { freeMinutes(it) }
                SortMode.BUSIEST -> visible.sortedBy { freeMinutes(it) }
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {

        // Day/Week switch + date navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedViewSwitch(viewMode = viewMode, onChange = onViewModeChange)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onPrev, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (viewMode == CalendarViewMode.DAY)
                        DateTimeUtils.formatDisplayDate(selectedDate)
                    else
                        DateTimeUtils.formatWeekRange(selectedDate),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onToday, contentPadding = PaddingValues(0.dp)) {
                    Text("Today", fontSize = 11.sp)
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }

        // Member filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allMembers, key = { it.userId }) { member ->
                MemberChip(
                    member = member,
                    isOn = selectedIds.contains(member.userId),
                    onClick = {
                        selectedIds = if (selectedIds.contains(member.userId))
                            selectedIds - member.userId
                        else
                            selectedIds + member.userId
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                GridHeaderRow(
                    viewMode = viewMode,
                    weekDates = weekDates,
                    sortLabel = sortMode.label,
                    onSortClick = {
                        sortMode = when (sortMode) {
                            SortMode.NAME -> SortMode.MOST_FREE
                            SortMode.MOST_FREE -> SortMode.BUSIEST
                            SortMode.BUSIEST -> SortMode.NAME
                        }
                    }
                )
                HorizontalDivider()

                EveryoneFreeRow(
                    visibleMembers = allMembers.filter { selectedIds.contains(it.userId) },
                    viewMode = viewMode,
                    targetDateString = targetDateString,
                    weekDates = weekDates
                )
                HorizontalDivider()

                when {
                    allMembers.isEmpty() -> EmptyState("No group member availability loaded yet. Pull to sync.")
                    sortedVisibleMembers.isEmpty() -> EmptyState("No members selected — tap an avatar above to show their schedule.")
                    else -> sortedVisibleMembers.forEachIndexed { index, member ->
                        MemberRow(
                            member = member,
                            isMe = member.userId == myProfile.userId,
                            viewMode = viewMode,
                            targetDateString = targetDateString,
                            weekDates = weekDates
                        )
                        if (index != sortedVisibleMembers.lastIndex) HorizontalDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem(color = SlotFreeGreen, label = "Common free time")
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = SlotBusyRed, label = "Busy")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SegmentedViewSwitch(viewMode: CalendarViewMode, onChange: (CalendarViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(3.dp)
    ) {
        CalendarViewMode.entries.forEach { mode ->
            val selected = mode == viewMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (mode == CalendarViewMode.DAY) "Day" else "Week",
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MemberChip(member: MemberAvailability, isOn: Boolean, onClick: () -> Unit) {
    val color = memberColor(member)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isOn) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isOn) color else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
            .alpha(if (isOn) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isOn) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            } else {
                Text(member.userName.take(1).uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(member.userName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GridHeaderRow(
    viewMode: CalendarViewMode,
    weekDates: List<Date>,
    sortLabel: String,
    onSortClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(MEMBER_COL_WIDTH)
                .padding(start = 10.dp)
                .clickable(onClick = onSortClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Member", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$sortLabel ↕",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewMode == CalendarViewMode.DAY) {
            for (h in START_HOUR until END_HOUR) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (h % 2 == 0) {
                        Text(
                            text = formatHourShort(h),
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            weekDates.forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = WEEKDAY_FORMAT.format(d),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DAY_NUM_FORMAT.format(d),
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Box(modifier = Modifier.width(FREE_COL_WIDTH), contentAlignment = Alignment.Center) {
                Text(text = "Free", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlotFreeGreen)
            }
        }
    }
}

@Composable
private fun EveryoneFreeRow(
    visibleMembers: List<MemberAvailability>,
    viewMode: CalendarViewMode,
    targetDateString: String,
    weekDates: List<Date>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlotFreeGreenLight.copy(alpha = 0.5f))
            .height(if (viewMode == CalendarViewMode.DAY) 36.dp else 42.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(MEMBER_COL_WIDTH).padding(start = 10.dp)) {
            Text(
                text = "Everyone free",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SlotFreeGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${visibleMembers.size} shown",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (viewMode == CalendarViewMode.DAY) {
            val busyBits = DateTimeUtils.combinedBusyBitmap(visibleMembers, targetDateString, START_HOUR, END_HOUR)
            for (h in START_HOUR until END_HOUR) {
                val i0 = (h - START_HOUR) * 2
                val freeBits = booleanArrayOf(!busyBits[i0], !busyBits[i0 + 1])
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 1.dp)) {
                    SegmentedTrack(bits = freeBits, segmentColor = SlotFreeGreen, modifier = Modifier.fillMaxSize())
                }
            }
        } else {
            var totalFreeMin = 0
            weekDates.forEach { d ->
                val dateString = DateTimeUtils.formatDateOnly(d)
                val busyBits = DateTimeUtils.combinedBusyBitmap(visibleMembers, dateString, START_HOUR, END_HOUR)
                val freeBits = BooleanArray(busyBits.size) { !busyBits[it] }
                totalFreeMin += freeBits.count { it } * 30
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    SegmentedTrack(bits = freeBits, segmentColor = SlotFreeGreen, modifier = Modifier.fillMaxSize())
                }
            }
            Box(modifier = Modifier.width(FREE_COL_WIDTH), contentAlignment = Alignment.Center) {
                Text(text = "${totalFreeMin / 60}h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlotFreeGreen)
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: MemberAvailability,
    isMe: Boolean,
    viewMode: CalendarViewMode,
    targetDateString: String,
    weekDates: List<Date>
) {
    val avatarColor = memberColor(member)

    val freeLabel = if (viewMode == CalendarViewMode.DAY) {
        "${DateTimeUtils.freeMinutesForDate(member, targetDateString, START_HOUR, END_HOUR) / 60}h free today"
    } else {
        val totalFree = weekDates.sumOf {
            DateTimeUtils.freeMinutesForDate(member, DateTimeUtils.formatDateOnly(it), START_HOUR, END_HOUR)
        }
        "${totalFree / 60}h free this week"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
            .height(if (viewMode == CalendarViewMode.DAY) 40.dp else 46.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(MEMBER_COL_WIDTH).padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = if (isMe) "${member.userName} (You)" else member.userName,
                    fontSize = 11.sp,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = freeLabel,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (viewMode == CalendarViewMode.DAY) {
            val busyBits = DateTimeUtils.halfHourBitmap(member.busySlots, targetDateString, START_HOUR, END_HOUR)
            for (h in START_HOUR until END_HOUR) {
                val i0 = (h - START_HOUR) * 2
                val hourBits = booleanArrayOf(busyBits[i0], busyBits[i0 + 1])
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 1.dp)) {
                    SegmentedTrack(bits = hourBits, segmentColor = SlotBusyRed, modifier = Modifier.fillMaxSize())
                }
            }
        } else {
            weekDates.forEach { d ->
                val dateString = DateTimeUtils.formatDateOnly(d)
                val busyBits = DateTimeUtils.halfHourBitmap(member.busySlots, dateString, START_HOUR, END_HOUR)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    SegmentedTrack(bits = busyBits, segmentColor = SlotBusyRed, modifier = Modifier.fillMaxSize())
                }
            }
            val totalFree = weekDates.sumOf {
                DateTimeUtils.freeMinutesForDate(member, DateTimeUtils.formatDateOnly(it), START_HOUR, END_HOUR)
            }
            Box(modifier = Modifier.width(FREE_COL_WIDTH), contentAlignment = Alignment.Center) {
                Text(text = "${totalFree / 60}h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlotFreeGreen)
            }
        }
    }
}

/**
 * Renders contiguous true-runs of [bits] as colored segments over their fractional position,
 * so a slot that covers only part of a cell (e.g. a meeting starting at :30) shows precisely.
 */
@Composable
private fun SegmentedTrack(bits: BooleanArray, segmentColor: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(TrackBackground)
    ) {
        val totalWidth = maxWidth
        var i = 0
        while (i < bits.size) {
            if (bits[i]) {
                val start = i
                while (i < bits.size && bits[i]) i++
                val leftFrac = start.toFloat() / bits.size
                val widthFrac = (i - start).toFloat() / bits.size
                Box(
                    modifier = Modifier
                        .offset(x = totalWidth * leftFrac)
                        .width(totalWidth * widthFrac)
                        .fillMaxHeight()
                        .background(segmentColor)
                )
            } else {
                i++
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

private fun memberColor(member: MemberAvailability): Color = try {
    Color(android.graphics.Color.parseColor(member.colorHex ?: "#3B82F6"))
} catch (e: Exception) {
    Color(0xFF2563EB)
}

private fun formatHourShort(hour: Int): String {
    val period = if (hour < 12) "a" else "p"
    val hr = if (hour % 12 == 0) 12 else hour % 12
    return "$hr$period"
}

private val WEEKDAY_FORMAT = SimpleDateFormat("EEE", Locale.getDefault())
private val DAY_NUM_FORMAT = SimpleDateFormat("d", Locale.getDefault())
