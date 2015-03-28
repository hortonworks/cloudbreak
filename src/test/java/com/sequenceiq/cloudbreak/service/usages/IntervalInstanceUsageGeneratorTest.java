package com.sequenceiq.cloudbreak.service.usages;

import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

public class IntervalInstanceUsageGeneratorTest {

    private IntervalInstanceUsageGenerator underTest;

    @Before
    public void setUp() {
        underTest = new IntervalInstanceUsageGenerator();
    }

    @Test
    public void testGetInstanceHoursShouldReturnWithEmptyMapWhenInstanceTerminatedBeforeTheIntervalStart() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 0);
    }

    @Test
    public void testGetInstanceHoursShouldReturnWithEmptyMapWhenInstanceStartedAfterTheEndOfTheInterval() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 0);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceRunInTheInterval() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 0, 0, 0);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceStartsInTheIntervalAndDoNotTerminate() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 0, 0, 0);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceStartsBeforeTheIntervalAndDoNotTerminate() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 0, 0, 0);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceStartsBeforeAndTerminatesAfterTheInterval() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 0, 0, 0);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceStartsInTheIntervalAndTerminatesAfterTheInterval() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 0, 0, 0);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceStartsBeforeAndTerminatesInTheInterval() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 29);
        setCalendarTo(cal, 12, 0, 0, 0);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 2);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 3);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenInstanceStartAndTerminateDatesAreEqualToTheInterval() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 0, 0, 0);
        instance.setStartDate(cal.getTimeInMillis());
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 2);
        instance.setTerminationDate(cal.getTimeInMillis());
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 3);
    }

    @Test
    public void testGetInstanceHoursShouldCalcInstanceHourWhenInstanceIsRunningForExactlyOneHour() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 26);
        setCalendarTo(cal, 12, 1, 11, 1);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(HOUR_OF_DAY, cal.get(HOUR_OF_DAY) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 1);
        assertEquals(Long.valueOf(1), result.values().toArray()[0]);
    }

    @Test
    public void testGetInstanceHoursShouldCalcInstanceHourWhenInstanceIsRunningForMoreThanOneHour() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 1, 11, 1);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(HOUR_OF_DAY, cal.get(HOUR_OF_DAY) + 2);
        cal.set(MINUTE, cal.get(MINUTE) + 2);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 1);
        assertEquals(Long.valueOf(3), result.values().toArray()[0]);
    }

    @Test
    public void testGetInstanceHoursShouldCalcExactInstanceHourWhenInstanceIsRunningForTwoDays() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 2, 12);
        setCalendarTo(cal, 12, 1, 11, 111);
        Date intervalStart = cal.getTime();
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
        assertEquals(Long.valueOf(12), result.values().toArray()[0]);
        assertEquals(Long.valueOf(12), result.values().toArray()[1]);
    }

    @Test
    public void testGetInstanceHoursShouldCalcExactInstanceHourWhenInstanceIsRunningMoreThenTwoDay() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 1, 11, 1);
        Date intervalStart = cal.getTime();
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 3);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 4);
        assertEquals(Long.valueOf(12), result.get("2015-02-27"));
        assertEquals(Long.valueOf(24), result.get("2015-02-28"));
        assertEquals(Long.valueOf(24), result.get("2015-03-01"));
        assertEquals(Long.valueOf(12), result.get("2015-03-02"));
    }

    @Test
    public void testGetInstanceHoursShouldCalcExactInstanceHourWhenInstanceIsRunningForTwoDaysAndHourNeedToBeRounded() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 1, 11, 111);
        Date intervalStart = cal.getTime();
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        setCalendarTo(cal, 12, 1, 11, 112);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
        assertEquals(Long.valueOf(12), result.get("2015-02-27"));
        assertEquals(Long.valueOf(13), result.get("2015-02-28"));
    }

    @Test
    public void testGetInstanceHoursShouldReturnWithEmptyListWhenInstanceStartIsNull() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        setCalendarTo(cal, 12, 1, 11, 111);
        Date intervalStart = cal.getTime();
        instance.setStartDate(null);
        cal.set(DATE, cal.get(DATE) + 1);
        setCalendarTo(cal, 12, 1, 11, 112);
        instance.setTerminationDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursForADayWhenTheInstanceRunsLessThanTheOverFlowedMinutesExistOnTheNextDay() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 27);
        //from 12:50
        setCalendarTo(cal, 12, 50, 0, 0);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalStart = cal.getTime();
        //to next day 00:40
        cal.set(DATE, cal.get(DATE) + 1);
        setCalendarTo(cal, 0, 40, 0, 0);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 1);
        assertEquals(12L, result.values().toArray()[0]);
    }

    @Test
    public void testGetInstanceHoursShouldCalcHoursWhenTheInstanceRunsMoreThanTheOverFlowedMinutesExistOnTheNextDay() throws ParseException {
        InstanceMetaData instance = new InstanceMetaData();
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 1, 26);
        //from 12:50
        setCalendarTo(cal, 12, 50, 0, 0);
        instance.setStartDate(cal.getTimeInMillis());
        cal.set(DATE, cal.get(DATE) + 1);
        Date intervalStart = cal.getTime();
        //to next day 00:40
        cal.set(DATE, cal.get(DATE) + 1);
        setCalendarTo(cal, 0, 51, 0, 0);
        Date intervalEnd = cal.getTime();

        Map<String, Long> result = underTest.getInstanceHours(instance, intervalStart, intervalEnd);

        assertEquals(result.size(), 2);
        Assert.assertEquals(Long.valueOf(12), result.get("2015-02-27"));
        assertEquals(Long.valueOf(1), result.get("2015-02-28"));
    }

    private void setCalendarTo(Calendar calendar, int hour, int min, int sec, int ms) {
        calendar.set(HOUR_OF_DAY, hour);
        calendar.set(MINUTE, min);
        calendar.set(SECOND, sec);
        calendar.set(MILLISECOND, ms);
    }
}