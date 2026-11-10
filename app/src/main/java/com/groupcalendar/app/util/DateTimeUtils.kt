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
     * Half-hour occupancy bitmap for one member's busy slots on a given date, clipped to
     * [startHour, endHour). Index i represents the half-hour starting at startHour*60 + i*30.
     */
    fun halfHourBitmap(
        slots: List<TimeSlot>,
        targetDateString: String,
        startHour: Int,
        endHour: Int
    ): BooleanArray {
        val numBlocks = (endHour - startHour) * 2
        val bits = BooleanArray(numBlocks)

        val startOfDayMillis = try {
            isoFormat.parse("${targetDateString}T${"%02d".format(startHour)}:00:00")?.time ?: return bits
        } catch (e: Exception) {
            return bits
        }
        val halfHourMillis = 30 * 60 * 1000L

        for (slot in slots) {
            val start = parseIso(slot.start)?.time ?: continue
            val end = parseIso(slot.end)?.time ?: continue

            for (i in 0 until numBlocks) {
                val blockStart = startOfDayMillis + i * halfHourMillis
                val blockEnd = blockStart + halfHourMillis
                if (start < blockEnd && end > blockStart) {
                    bits[i] = true
                }
            }
        }
        return bits
    }

    /**
     * Half-hour bitmap that is true wherever ANY of the given members is busy.
     */
    fun combinedBusyBitmap(
        members: Collection<MemberAvailability>,
        targetDateString: String,
        startHour: Int,
        endHour: Int
    ): BooleanArray {
        val numBlocks = (endHour - startHour) * 2
        val combined = BooleanArray(numBlocks)
        for (member in members) {
            val bits = halfHourBitmap(member.busySlots, targetDateString, startHour, endHour)
            for (i in 0 until numBlocks) {
                if (bits[i]) combined[i] = true
            }
        }
        return combined
    }

    /**
     * Minutes a member is free within [startHour, endHour) on the given date.
     */
    fun freeMinutesForDate(
        member: MemberAvailability,
        targetDateString: String,
        startHour: Int,
        endHour: Int
    ): Int {
        val bits = halfHourBitmap(member.busySlots, targetDateString, startHour, endHour)
        val busyBlocks = bits.count { it }
        return (bits.size - busyBlocks) * 30
    }

    /**
     * The seven dates (Monday through Sunday) of the week containing [date].
     */
    fun datesOfWeek(date: Date): List<Date> {
        val cal = Calendar.getInstance().apply { time = date }
        val dow = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 .. SATURDAY=7
        val diffToMonday = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
        cal.add(Calendar.DAY_OF_YEAR, diffToMonday)
        return (0 until 7).map { offset ->
            val d = cal.clone() as Calendar
            d.add(Calendar.DAY_OF_YEAR, offset)
            d.time
        }
    }

    private val weekRangeFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    fun formatWeekRange(date: Date): String {
        val week = datesOfWeek(date)
        return "${weekRangeFormat.format(week.first())} – ${weekRangeFormat.format(week.last())}"
    }
}
