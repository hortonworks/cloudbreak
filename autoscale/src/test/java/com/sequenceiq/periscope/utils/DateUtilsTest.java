package com.sequenceiq.periscope.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.periscope.domain.TimeAlert;

@RunWith(MockitoJUnitRunner.class)
public class DateUtilsTest {

    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private DateUtils underTest;

    @Before
    public void setUp() {

    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsBeforeTheNextTriggerMoreThanTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "America/Denver";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 19, 13, 15, 0, 0, zoneId);
        long monitorUpdateRate = 1000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeUtils.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?", timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsBeforeTheNextTriggerLessThanTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 11, 59, 10, 0, zoneId);
        long monitorUpdateRate = 1000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeUtils.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?", timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsAfterTheNextTriggerMoreThanTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "UTC";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 59, 10, 0, zoneId);
        long monitorUpdateRate = 1000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeUtils.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?", timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsAfterTheNextTriggerLessThanTheMonitorUpdateRateThenItShouldTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 20, 20, 1, 10, 0, ZoneId.systemDefault());
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 1, 10, 0, zoneId);
        long monitorUpdateRate = 90000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeUtils.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?", timeZone);
        assertTrue(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsAfterTheNextTriggerWithSizeOfTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 0, 10, 0, zoneId);
        long monitorUpdateRate = 10000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeUtils.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?", timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeEqualsWithTheNextTriggerThenItShouldTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 20, 20, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 0, 0, 0, zoneId);
        long monitorUpdateRate = 60000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeUtils.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?",   timeZone);
        assertTrue(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeEqualsWithTheNextTriggerAndZonesAreEquivalentThenItShouldTriggerAnEvent() {
        ZoneId zone = ZoneId.systemDefault();
        String timeZone = zone.getId();
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 0, 0, 0, zone.normalized());
        long monitorUpdateRate = 60000;

        when(dateTimeUtils.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeUtils.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert("0 0 12 * * ?",   timeZone);
        assertTrue(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    private TimeAlert createTimeAlert(String cron, String timeZone) {
        TimeAlert testTime = new TimeAlert();
        testTime.setName("testAlert");
        testTime.setCron(cron);
        testTime.setTimeZone(timeZone);
        return testTime;
    }
}