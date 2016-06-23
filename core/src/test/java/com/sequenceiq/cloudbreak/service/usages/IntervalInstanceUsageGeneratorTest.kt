package com.sequenceiq.cloudbreak.service.usages

import java.util.Calendar.DATE
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MILLISECOND
import java.util.Calendar.MINUTE
import java.util.Calendar.SECOND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import java.text.ParseException
import java.util.Calendar
import java.util.Date

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.domain.InstanceMetaData

class IntervalInstanceUsageGeneratorTest {

    private var underTest: IntervalInstanceUsageGenerator? = null

    @Before
    fun setUp() {
        underTest = IntervalInstanceUsageGenerator()
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldReturnWithEmptyMapWhenInstanceTerminatedBeforeTheIntervalStart() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 0)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldReturnWithEmptyMapWhenInstanceStartedAfterTheEndOfTheInterval() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        instance.terminationDate = cal.timeInMillis

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 0)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceRunInTheInterval() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 0, 0, 0)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceStartsInTheIntervalAndDoNotTerminate() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 0, 0, 0)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceStartsBeforeTheIntervalAndDoNotTerminate() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 0, 0, 0)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceStartsBeforeAndTerminatesAfterTheInterval() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 0, 0, 0)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.terminationDate = cal.timeInMillis

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceStartsInTheIntervalAndTerminatesAfterTheInterval() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 0, 0, 0)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.terminationDate = cal.timeInMillis

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceStartsBeforeAndTerminatesInTheInterval() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 29)
        setCalendarTo(cal, 12, 0, 0, 0)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 2)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 3)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenInstanceStartAndTerminateDatesAreEqualToTheInterval() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 0, 0, 0)
        instance.startDate = cal.timeInMillis
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 2)
        instance.terminationDate = cal.timeInMillis
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 3)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcInstanceHourWhenInstanceIsRunningForExactlyOneHour() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 26)
        setCalendarTo(cal, 12, 1, 11, 1)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(HOUR_OF_DAY, cal.get(HOUR_OF_DAY) + 1)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 1)
        assertEquals(java.lang.Long.valueOf(1), result.values.toArray()[0])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcInstanceHourWhenInstanceIsRunningForMoreThanOneHour() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 1, 11, 1)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(HOUR_OF_DAY, cal.get(HOUR_OF_DAY) + 2)
        cal.set(MINUTE, cal.get(MINUTE) + 2)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 1)
        assertEquals(java.lang.Long.valueOf(3), result.values.toArray()[0])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcExactInstanceHourWhenInstanceIsRunningForTwoDays() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 2, 12)
        setCalendarTo(cal, 12, 1, 11, 111)
        val intervalStart = cal.time
        cal.set(DATE, cal.get(DATE) + 1)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
        assertEquals(java.lang.Long.valueOf(12), result.values.toArray()[0])
        assertEquals(java.lang.Long.valueOf(12), result.values.toArray()[1])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcExactInstanceHourWhenInstanceIsRunningMoreThenTwoDay() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 1, 11, 1)
        val intervalStart = cal.time
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 3)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 4)
        assertEquals(java.lang.Long.valueOf(12), result["2015-02-27"])
        assertEquals(java.lang.Long.valueOf(24), result["2015-02-28"])
        assertEquals(java.lang.Long.valueOf(24), result["2015-03-01"])
        assertEquals(java.lang.Long.valueOf(12), result["2015-03-02"])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcExactInstanceHourWhenInstanceIsRunningForTwoDaysAndHourNeedToBeRounded() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 1, 11, 111)
        val intervalStart = cal.time
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        setCalendarTo(cal, 12, 1, 11, 112)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
        assertEquals(java.lang.Long.valueOf(12), result["2015-02-27"])
        assertEquals(java.lang.Long.valueOf(13), result["2015-02-28"])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldReturnWithEmptyListWhenInstanceStartIsNull() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        setCalendarTo(cal, 12, 1, 11, 111)
        val intervalStart = cal.time
        instance.startDate = null
        cal.set(DATE, cal.get(DATE) + 1)
        setCalendarTo(cal, 12, 1, 11, 112)
        instance.terminationDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertTrue(result.isEmpty())
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursForADayWhenTheInstanceRunsLessThanTheOverFlowedMinutesExistOnTheNextDay() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 27)
        //from 12:50
        setCalendarTo(cal, 12, 50, 0, 0)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalStart = cal.time
        //to next day 00:40
        cal.set(DATE, cal.get(DATE) + 1)
        setCalendarTo(cal, 0, 40, 0, 0)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 1)
        assertEquals(12L, result.values.toArray()[0])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetInstanceHoursShouldCalcHoursWhenTheInstanceRunsMoreThanTheOverFlowedMinutesExistOnTheNextDay() {
        val instance = InstanceMetaData()
        val cal = Calendar.getInstance()
        cal.set(2015, 1, 26)
        //from 12:50
        setCalendarTo(cal, 12, 50, 0, 0)
        instance.startDate = cal.timeInMillis
        cal.set(DATE, cal.get(DATE) + 1)
        val intervalStart = cal.time
        //to next day 00:40
        cal.set(DATE, cal.get(DATE) + 1)
        setCalendarTo(cal, 0, 51, 0, 0)
        val intervalEnd = cal.time

        val result = underTest!!.getInstanceHours(instance, intervalStart, intervalEnd)

        assertEquals(result.size.toLong(), 2)
        Assert.assertEquals(java.lang.Long.valueOf(12), result["2015-02-27"])
        assertEquals(java.lang.Long.valueOf(1), result["2015-02-28"])
    }

    private fun setCalendarTo(calendar: Calendar, hour: Int, min: Int, sec: Int, ms: Int) {
        calendar.set(HOUR_OF_DAY, hour)
        calendar.set(MINUTE, min)
        calendar.set(SECOND, sec)
        calendar.set(MILLISECOND, ms)
    }
}