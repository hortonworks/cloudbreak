package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CDPOperationDetailsTest {

    @Test
    public void testGetUTCDateTimeWhenTimestampSet() {
        CDPOperationDetails underTest = new CDPOperationDetails();
        LocalDateTime date = LocalDateTime.of(2020, 10, 5, 7, 50);
        underTest.setTimestamp(date.toEpochSecond(ZoneOffset.ofHours(2)) * 1000);

        String actual = underTest.getUTCDateTime();
        String expected = "10/05/2020 05:50:00 +0000";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetUTCDateTimeWhenTimestampNull() {
        CDPOperationDetails underTest = new CDPOperationDetails();
        underTest.setTimestamp(null);
        String actual = underTest.getUTCDateTime();
        String expected = "";
        Assertions.assertEquals(expected, actual);
    }
}
