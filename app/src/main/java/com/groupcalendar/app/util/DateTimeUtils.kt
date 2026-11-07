package com.groupcalendar.app.util

import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.data.model.TimeSlot
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatDisplayDate(date: Date): String = displayDateFormat.format(date)
    fun formatDateOnly(date: Date): String = dateOnlyFormat.format(date)

    fun parseIso(isoString: String): Date? {
        return try {
            // Strip trailing Z or timezone offset if needed
            val clean = isoString.take(19)
            isoFormat.parse(clean)
        } catch (e: Exception) {
            null
        }
    }

    fun formatIso(date: Date): String = isoFormat.format(date)

    fun formatDisplayTime(isoString: String): String {
        val d = parseIso(isoString) ?: return isoString
        return displayTimeFormat.format(d)
    }

    fun formatShortTime(isoString: String): String {
        val d = parseIso(isoString) ?: return isoString
        return shortTimeFormat.format(d)
    }

    /**
     * Checks if a busy slot intersects with a specific date
     */
    fun isSlotOnDate(slot: TimeSlot, targetDateString: String): Boolean {
        val startPrefix = slot.start.take(10)
        val endPrefix = slot.end.take(10)
        return targetDateString == startPrefix || targetDateString == endPrefix
    }

    /**
     * Given a roster of members and a target date, finds mutual free time intervals
     * between 08:00 and 22:00.
     */
    fun findCommonFreeSlots(
        roster: Collection<MemberAvailability>,
        targetDateString: String
    ): List<TimeSlot> {
        if (roster.isEmpty()) return emptyList()

        // Daily working window: 08:00 to 22:00 (14 hours * 2 = 28 half-hour blocks)
        val numBlocks = 28 // 30-minute intervals
        val busyBlock = BooleanArray(numBlocks) { false }

        val startOfDayMillis = try {
            isoFormat.parse("${targetDateString}T08:00:00")?.time ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        val thirtyMinutesMillis = 30 * 60 * 1000L

        for (member in roster) {
            for (slot in member.busySlots) {
                val start = parseIso(slot.start)?.time ?: continue
                val end = parseIso(slot.end)?.time ?: continue

                for (i in 0 until numBlocks) {
                    val blockStart = startOfDayMillis + (i * thirtyMinutesMillis)
                    val blockEnd = blockStart + thirtyMinutesMillis

                    // If slot overlaps this block
                    if (start < blockEnd && end > blockStart) {
                        busyBlock[i] = true
                    }
                }
            }
        }

        // Build continuous free intervals
        val freeSlots = mutableListOf<TimeSlot>()
        var freeStartBlock: Int? = null

        for (i in 0 until numBlocks) {
            if (!busyBlock[i]) {
                if (freeStartBlock == null) freeStartBlock = i
            } else {
                if (freeStartBlock != null) {
                    val sTime = Date(startOfDayMillis + freeStartBlock * thirtyMinutesMillis)
                    val eTime = Date(startOfDayMillis + i * thirtyMinutesMillis)
                    freeSlots.add(TimeSlot(formatIso(sTime), formatIso(eTime), "All Free"))
                    freeStartBlock = null
                }
            }
        }

        if (freeStartBlock != null) {
            val sTime = Date(startOfDayMillis + freeStartBlock * thirtyMinutesMillis)
            val eTime = Date(startOfDayMillis + numBlocks * thirtyMinutesMillis)
            freeSlots.add(TimeSlot(formatIso(sTime), formatIso(eTime), "All Free"))
        }

        return freeSlots
    }
}
