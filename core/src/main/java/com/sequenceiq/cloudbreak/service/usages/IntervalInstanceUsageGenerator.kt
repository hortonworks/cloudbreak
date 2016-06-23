package com.sequenceiq.cloudbreak.service.usages

import java.util.Calendar.DATE
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MILLISECOND
import java.util.Calendar.MINUTE
import java.util.Calendar.MONTH
import java.util.Calendar.SECOND
import java.util.Calendar.YEAR

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.InstanceMetaData

@Component
class IntervalInstanceUsageGenerator {

    @Throws(ParseException::class)
    fun getInstanceHours(instance: InstanceMetaData, startTime: Date, stopTime: Date): Map<String, Long> {
        val dailyInstanceUsages = HashMap<String, Long>()
        val start = getUsageStart(startTime, stopTime, instance)
        val stop = getUsageStop(startTime, stopTime, instance)

        if (start != null && stop != null) {
            doGenerateUsage(instance, dailyInstanceUsages, start, stop)
        }
        return dailyInstanceUsages
    }

    private fun getUsageStart(startTime: Date, stopTime: Date, instance: InstanceMetaData): Calendar? {
        var start: Calendar? = null
        val instanceStart = instance.startDate
        val intervalStart = startTime.time
        val intervalStop = stopTime.time
        if (instanceStart != null && intervalStop > instanceStart) {
            if (instanceStart < intervalStart) {
                start = getCalendarInstanceForDate(intervalStart)
            } else {
                start = getCalendarInstanceForDate(instanceStart)
            }
        }
        return start
    }

    private fun getUsageStop(startTime: Date, stopTime: Date, instance: InstanceMetaData): Calendar? {
        var stop: Calendar? = null
        val instanceStop = instance.terminationDate
        val intervalStart = startTime.time
        val intervalStop = stopTime.time
        if (instanceStop == null || intervalStart < instanceStop) {
            if (instanceStop != null && instanceStop < intervalStop) {
                stop = getCalendarInstanceForDate(instanceStop)
            } else {
                stop = getCalendarInstanceForDate(intervalStop)
            }
        }
        return stop
    }

    private fun getCalendarInstanceForDate(timeInMs: Long): Calendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMs
        return cal
    }

    @Throws(ParseException::class)
    private fun doGenerateUsage(instance: InstanceMetaData, dailyInstanceUsages: MutableMap<String, Long>, start: Calendar, stop: Calendar) {
        var runningHours: Long = 0
        val startAsDate = start.time
        if (isCalendarsOnTheSameDay(start, stop)) {
            runningHours = millisToCeiledHours(stop.timeInMillis - start.timeInMillis)
            LOGGER.debug("Instance '{}' ran less than a day, usage: {}", instance.id, runningHours)
            dailyInstanceUsages.put(DATE_FORMAT.format(startAsDate), runningHours)
        } else {
            // get start day running hours
            runningHours = runningHoursForDay(startAsDate, true)
            dailyInstanceUsages.put(DATE_FORMAT.format(startAsDate), runningHours)
            LOGGER.debug("Instance '{}' ran on the start day, usage: {}", instance.id, runningHours)
            // get stop day running hours
            subtractStartOverFlowedTimeFromStop(start, stop)
            if (!isCalendarsOnTheSameDay(start, stop)) {
                val stopAsDate = stop.time
                runningHours = runningHoursForDay(stopAsDate, false)
                dailyInstanceUsages.put(DATE_FORMAT.format(stopAsDate), runningHours)
                LOGGER.debug("Instance '{}' ran on the day of the termination, usage: {}", instance.id, runningHours)
            }

            generateAllDayStackUsages(start.timeInMillis, stop.timeInMillis, dailyInstanceUsages)
        }
    }

    private fun isCalendarsOnTheSameDay(start: Calendar, stop: Calendar): Boolean {
        return start.get(YEAR) == stop.get(YEAR)
                && start.get(MONTH) == stop.get(MONTH)
                && start.get(DATE) == stop.get(DATE)
    }

    private fun subtractStartOverFlowedTimeFromStop(start: Calendar, stop: Calendar) {
        stop.add(MINUTE, -start.get(MINUTE))
        stop.add(SECOND, -start.get(SECOND))
        stop.add(MILLISECOND, -start.get(MILLISECOND))
    }

    @Throws(ParseException::class)
    private fun runningHoursForDay(date: Date, startDay: Boolean): Long {
        val dayAsStr = DATE_FORMAT.format(date)
        var dayStartOrEndInMillis = DATE_FORMAT.parse(dayAsStr).time
        if (startDay) {
            dayStartOrEndInMillis += TimeUnit.HOURS.toMillis(HOURS_PER_DAY)
        }
        val hourInMillis = date.time - dayStartOrEndInMillis
        return millisToCeiledHours(hourInMillis)
    }

    private fun millisToCeiledHours(hourInMillis: Long): Long {
        val absHourInMillis = Math.abs(hourInMillis)
        val ceiledHours = Math.ceil(absHourInMillis / MS_PER_HOUR)
        return ceiledHours.toLong()
    }

    private fun generateAllDayStackUsages(startInMs: Long, stopInMs: Long, dailyInstanceUsages: MutableMap<String, Long>) {
        val start = getCalendarInstanceForDate(startInMs)
        start.add(DATE, 1)
        setDayToBeginning(start)
        val end = getCalendarInstanceForDate(stopInMs)
        setDayToBeginning(end)

        var date = start.time
        while (!start.after(end) && start != end) {
            val day = DATE_FORMAT.format(date)
            dailyInstanceUsages.put(day, HOURS_PER_DAY)
            LOGGER.debug("Instance ran on all day, usage: '{}'", day)
            start.add(DATE, 1)
            date = start.time
        }
    }

    private fun setDayToBeginning(calendar: Calendar) {
        calendar.set(HOUR_OF_DAY, 0)
        calendar.set(MINUTE, 0)
        calendar.set(SECOND, 0)
        calendar.set(MILLISECOND, 0)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(IntervalInstanceUsageGenerator::class.java)
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        private val HOURS_PER_DAY: Long = 24
        private val MS_PER_HOUR = 3600000.0
    }
}
