package com.sequenceiq.periscope.service;

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
public class TimeAlertEvaluatorTest {

    @Mock
    private DateTimeService dateTimeService;

    @InjectMocks
    private TimeAlertEvaluator underTest;

    @Before
    public void setUp() {

    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsBeforeTheNextTriggerMoreThanTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "America/Denver";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 19, 13, 15, 0, 0, zoneId);
        long monitorUpdateRate = 1000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentTime);
        TimeAlert timeAlert = createTimeAlert(0L, timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsBeforeTheNextTriggerLessThanTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 11, 59, 10, 0, zoneId);
        long monitorUpdateRate = 1000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeService.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert(1L, timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsAfterTheNextTriggerMoreThanTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "UTC";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 59, 10, 0, zoneId);
        long monitorUpdateRate = 1000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeService.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert(2L, timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsAfterTheNextTriggerLessThanTheMonitorUpdateRateThenItShouldTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 20, 20, 1, 10, 0, ZoneId.systemDefault());
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 1, 10, 0, zoneId);
        long monitorUpdateRate = 90000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert(3L, timeZone);
        assertTrue(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeIsAfterTheNextTriggerWithSizeOfTheMonitorUpdateRateThenItShouldNotTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 0, 10, 0, zoneId);
        long monitorUpdateRate = 10000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeService.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert(4L, timeZone);
        assertFalse(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeEqualsWithTheNextTriggerThenItShouldTriggerAnEvent() {
        String timeZone = "America/New_York";
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 20, 20, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 0, 0, 0, zoneId);
        long monitorUpdateRate = 60000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert(5L, timeZone);
        assertTrue(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenTheCurrentTimeEqualsWithTheNextTriggerAndZonesAreEquivalentThenItShouldTriggerAnEvent() {
        ZoneId zone = ZoneId.systemDefault();
        String timeZone = zone.getId();
        ZonedDateTime currentZonedTime = ZonedDateTime.of(2017, 12, 20, 12, 0, 0, 0, zone.normalized());
        long monitorUpdateRate = 60000L;

        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentZonedTime);
        when(dateTimeService.getZonedDateTime(currentZonedTime.toInstant(), timeZone)).thenReturn(currentZonedTime);
        TimeAlert timeAlert = createTimeAlert(6L, timeZone);
        assertTrue(underTest.isTrigger(timeAlert, monitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenFirstEvaluationBeforeTimeAndSecondOneAfterDesiredTimeMoreThanTheDefaultMonitorUpdateRate() {
        long defaultMonitorUpdateRate = 10000L;
        String cronExpression = "0 20 14 * * *";
        ZoneId zone = ZoneId.systemDefault();
        String timeZone = zone.getId();
        TimeAlert timeAlert = createTimeAlert(7L, timeZone, cronExpression);

        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 19, 14, 19, 59, 753564, zone.normalized());
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentTime);
        assertFalse(underTest.isTrigger(timeAlert, defaultMonitorUpdateRate));

        currentTime = ZonedDateTime.of(2017, 12, 19, 14, 20, 11, 253564, zone.normalized());
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentTime);
        assertTrue(underTest.isTrigger(timeAlert, defaultMonitorUpdateRate));
    }

    @Test
    public void testIsTriggerWhenAllEvaluationsBeforeDesiredTime() {
        long defaultMonitorUpdateRate = 10000L;
        String cronExpression = "0 20 14 * * *";
        ZoneId zone = ZoneId.systemDefault();
        String timeZone = zone.getId();
        TimeAlert timeAlert = createTimeAlert(7L, timeZone, cronExpression);

        ZonedDateTime currentTime = ZonedDateTime.of(2017, 12, 19, 14, 19, 46, 753564, zone.normalized());
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentTime);
        assertFalse(underTest.isTrigger(timeAlert, defaultMonitorUpdateRate));

        currentTime = ZonedDateTime.of(2017, 12, 19, 14, 19, 59, 253564, zone.normalized());
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        when(dateTimeService.getZonedDateTime(currentTime.toInstant(), timeZone)).thenReturn(currentTime);
        assertFalse(underTest.isTrigger(timeAlert, defaultMonitorUpdateRate));
    }

    private TimeAlert createTimeAlert(long id, String timeZone) {
        return createTimeAlert(id, timeZone, "0 0 12 * * ?");
    }

    private TimeAlert createTimeAlert(long id, String timeZone, String cronExpression) {
        TimeAlert testTime = new TimeAlert();
        testTime.setId(id);
        testTime.setName("testAlert");
        testTime.setCron(cronExpression);
        testTime.setTimeZone(timeZone);
        return testTime;
    }
}