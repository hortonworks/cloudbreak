package com.sequenceiq.periscope.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlert;

@ExtendWith(MockitoExtension.class)
class DateServiceTest {

    private static final boolean SHOULD_NOT_TRIGGER = false;

    private static final boolean SHOULD_TRIGGER = true;

    @Mock
    private DateTimeService dateTimeService;

    @InjectMocks
    private DateService underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] isTriggerScenarios() {
        return new Object[][] {
            { "Current time is before the next trigger more than the monitor update rate, different time zone",
                "America/New_York", 2017, 12, 19, 16, 59, 58, 0, "UTC", 1000L, SHOULD_NOT_TRIGGER },

            { "Current time is before the next trigger less than the monitor update rate, different time zone",
                "America/New_York", 2017, 12, 19, 16, 59, 58, 0, "UTC", 5000L, SHOULD_NOT_TRIGGER },

            { "Current time is after the next trigger more than the monitor update rate, different time zone",
                "America/New_York", 2017, 12, 19, 17, 0, 10, 0, "UTC", 5000L, SHOULD_NOT_TRIGGER },

            { "Current time is after the next trigger less than the monitor update rate, different time zone",
                "America/New_York", 2017, 12, 19, 17, 0, 4, 0, "UTC", 5000L, SHOULD_TRIGGER },

            { "Current time is after the next trigger exactly with the monitor update rate, different time zone",
                "America/New_York", 2017, 12, 19, 17, 0, 5, 0, "UTC", 5000L, SHOULD_NOT_TRIGGER },

            { "Current time equals with the next trigger, different time zone",
                "America/New_York", 2017, 12, 19, 17, 0, 0, 0, "UTC", 60000L, SHOULD_TRIGGER },

            { "Current time is before the next trigger more than the monitor update rate, same time zone",
                "UTC", 2017, 12, 19, 11, 59, 58, 0, "UTC", 1000L, SHOULD_NOT_TRIGGER },

            { "Current time is before the next trigger less than the monitor update rate, same time zone",
                "UTC", 2017, 12, 19, 11, 59, 58, 0, "UTC", 5000L, SHOULD_NOT_TRIGGER },

            { "Current time is after the next trigger more than the monitor update rate, same time zone",
                "UTC", 2017, 12, 19, 12, 0, 10, 0, "UTC", 5000L, SHOULD_NOT_TRIGGER },

            { "Current time is after the next trigger less than the monitor update rate, same time zone",
                "UTC", 2017, 12, 19, 12, 0, 4, 0, "UTC", 5000L, SHOULD_TRIGGER },

            { "Current time is after the next trigger exactly with the monitor update rate, same time zone",
                "UTC", 2017, 12, 19, 12, 0, 5, 0, "UTC", 5000L, SHOULD_NOT_TRIGGER },

            { "Current time equals with the next trigger, same time zone",
                "UTC", 2017, 12, 19, 12, 0, 0, 0, "UTC", 60000L, SHOULD_TRIGGER }
        };
    }

    // CHECKSTYLE:ON
    // @formatter:on
    @ParameterizedTest(name = "{0}")
    @MethodSource("isTriggerScenarios")
    void testIsTrigger(String testCaseName, String alertTimeZone, int year, int month, int day, int hour, int min, int sec, int nano, String currentTimeZone,
            long updateRate, boolean expectedIsTrigger) {
        ZonedDateTime currentTime = ZonedDateTime.of(year, month, day, hour, min, sec, nano, ZoneId.of(currentTimeZone));
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(currentTime);
        TimeAlert timeAlert = createTimeAlert(alertTimeZone);
        assertThat(underTest.isTrigger(timeAlert, updateRate)).isEqualTo(expectedIsTrigger);
    }

    @Test
    void testValidateTimeZoneWhenValid() throws Exception {
        underTest.validateTimeZone("GMT");
    }

    @Test
    void testValidateTimeZoneWhenInvalid() {
        assertThrows(ParseException.class,
                () -> underTest.validateTimeZone("GMTzen"));
    }

    private TimeAlert createTimeAlert(String timeZone) {
        TimeAlert testTime = new TimeAlert();
        testTime.setName("testAlert");
        testTime.setCron("0 0 12 * * ?");
        testTime.setTimeZone(timeZone);
        Cluster cluster = new Cluster();
        cluster.setStackCrn("testCrn");
        testTime.setCluster(cluster);
        return testTime;
    }
}

