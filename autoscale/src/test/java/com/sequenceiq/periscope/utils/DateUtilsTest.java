package com.sequenceiq.periscope.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.CronExpression;

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
    public void testCronExpressionValidThenShouldReturnTrue() throws ParseException {
        assertNotNull(underTest.getCronExpression("0 0 12 * * ?"));
    }

    @Test
    public void testCronExpressionIfTheUserSendingOnlyFiveSegment() throws ParseException {
        CronExpression cronExpression = underTest.getCronExpression("0 8 * * 5");
        assertEquals("0 8 * * 5 ?", cronExpression.getCronExpression());
        assertNotNull(cronExpression);
    }

    @Test
    public void testCronExpressionIfTheUserSendingOnlyFourSegment() throws ParseException {
        CronExpression cronExpression = underTest.getCronExpression("0 8 * *");
        assertEquals("0 8 * * * ?", cronExpression.getCronExpression());
        assertNotNull(cronExpression);
    }

    @Test(expected = ParseException.class)
    public void testCronExpressionIfTheUserSendingOnlyThreeSegment() throws ParseException {
        assertNotNull(underTest.getCronExpression("0 8 *"));
    }

    @Test(expected = ParseException.class)
    public void testCronExpressionIfTheUserSendingOnlyTwoSegment() throws ParseException {
        assertNotNull(underTest.getCronExpression("0 8"));
    }

    @Test(expected = ParseException.class)
    public void testCronExpressionInValidThenShouldReturnParseException() throws ParseException {
        assertNotNull(underTest.getCronExpression("0 0 ! * * ?"));
    }

    @Test
    public void testIsNotTriggerIfIsNotInTimeThenItShouldNotTriggerAnEvent() {
        DateTime currentTime = new DateTime(2017, 12, 19, 13, 15, 0);
        DateTime nextTime = new DateTime(2017, 12, 19, 13, 16, 0);

        when(dateTimeUtils.getCurrentDate(anyString())).thenReturn(currentTime);
        when(dateTimeUtils.getDateTime(anyObject(), anyString())).thenReturn(nextTime);
        assertFalse(underTest.isTrigger("0 0 12 * * ?", "UTC", 400));
    }

    @Test
    public void testIsTriggerIfIsInTimeThenItShouldTriggerAnEvent() {
        DateTime currentTime = new DateTime(2017, 12, 19, 13, 15, 0);
        DateTime nextTime = new DateTime(2017, 12, 19, 13, 16, 30);
        long oneMinute = 60000;

        when(dateTimeUtils.getCurrentDate(anyString())).thenReturn(currentTime);
        when(dateTimeUtils.getDateTime(anyObject(), anyString())).thenReturn(nextTime);
        assertTrue(underTest.isTrigger("0 0 12 * * ?", "GMT", oneMinute));
    }
}