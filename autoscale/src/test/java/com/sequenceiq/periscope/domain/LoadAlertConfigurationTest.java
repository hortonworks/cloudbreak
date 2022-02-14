package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LoadAlertConfigurationTest {

    private LoadAlertConfiguration underTest = new LoadAlertConfiguration();

    public static Stream<Arguments> pollingCoolDownMins() {
        return Stream.of(
                //TestCase,CoolDownMins,ScaleUpCoolDownMins,ScaleDownCoolDownMins,ExpectedPollingCoolDownMins
                Arguments.of("DEFAULT_POLLING_COOLDOWN_MINS", -1, -1, -1, DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS),
                Arguments.of("POLLING_COOLDOWN_DEFINED1", 50, -1, -1, 50),
                Arguments.of("POLLING_COOLDOWN_DEFINED2", 50, 10, 10, 10),
                Arguments.of("POLLING_COOLDOWN_DEFINED2", 50, 10, 5, 5),

                Arguments.of("POLLING_SCALEUP_DEFINED", -1, 10, -1, DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS),
                Arguments.of("POLLING_SCALEDOWN_DEFINED", -1, -1, 10, DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS),

                Arguments.of("POLLING_SCALEUP_SCALEDOWN_DEFINED1", -1, 10, 10, 10),
                Arguments.of("POLLING_SCALEUP_SCALEDOWN_DEFINED2", -1, 5, 10, 5)
        );
    }

    @ParameterizedTest(name = "{0}: With coolDownMins={1}, scaleUpCoolDownMins ={2}, scaleDownCoolDownMins={3}, expectedPollingCoolDownMins={4} ")
    @MethodSource("pollingCoolDownMins")
    public void testDefaultPollingCoolDownMins(String testType, int coolDownMins, int scaleUpCoolDownMins,
            int scaleDownCoolDownMins, int expectedPollingCoolDownMins) {
        if (coolDownMins != -1) {
            underTest.setCoolDownMinutes(coolDownMins);
        }
        if (scaleUpCoolDownMins != -1) {
            underTest.setScaleUpCoolDownMinutes(scaleUpCoolDownMins);
        }
        if (scaleDownCoolDownMins != -1) {
            underTest.setScaleDownCoolDownMinutes(scaleDownCoolDownMins);
        }
        assertEquals("Polling CoolDown Mins should match : " + testType, expectedPollingCoolDownMins,
                TimeUnit.MINUTES.convert(underTest.getPollingCoolDownMillis(), TimeUnit.MILLISECONDS));
    }

    public static Stream<Arguments> scaleUpCoolDownMins() {
        return Stream.of(
                //TestCase,CoolDownMins,ScaleUpCoolDownMins,ExpectedScaleUpCoolDownMins
                Arguments.of("DEFAULT_SCALEUP_COOLDOWN_MINS", -1, -1, DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS),
                Arguments.of("SCALEUP_COOLDOWN_MINS1", 10, -1, 10),
                Arguments.of("SCALEUP_COOLDOWN_MINS2", 10, 5, 5),
                Arguments.of("SCALEUP_COOLDOWN_MINS3", -1, 12, 12),
                Arguments.of("SCALEUP_COOLDOWN_MINS4", -1, 4, 4)
        );
    }

    @ParameterizedTest(name = "{0}: With coolDownMins={1}, scaleUpCoolDownMins ={2}, expectedScaleUpCoolDownMins={3} ")
    @MethodSource("scaleUpCoolDownMins")
    public void testScaleUpCoolDownMins(String testType, int coolDownMins, int scaleUpCoolDownMins, int expectedScaleUpCoolDownMins) {
        if (coolDownMins != -1) {
            underTest.setCoolDownMinutes(coolDownMins);
        }
        if (scaleUpCoolDownMins != -1) {
            underTest.setScaleUpCoolDownMinutes(scaleUpCoolDownMins);
        }
        assertEquals("Scaleup CoolDown Mins should match : " + testType, expectedScaleUpCoolDownMins,
                TimeUnit.MINUTES.convert(underTest.getScaleUpCoolDownMillis(), TimeUnit.MILLISECONDS));
    }

    public static Stream<Arguments> scaleDownCoolDownMins() {
        return Stream.of(
                //TestCase,CoolDownMins,ScaleDownCoolDownMins,ExpectedScaleDownCoolDownMins
                Arguments.of("DEFAULT_SCALEDOWN_COOLDOWN_MINS", -1, -1, DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS),
                Arguments.of("SCALEDOWN_COOLDOWN_MINS1", 10, -1, 10),
                Arguments.of("SCALEDOWN_COOLDOWN_MINS2", 10, 5, 5),
                Arguments.of("SCALEDOWN_COOLDOWN_MINS3", -1, 12, 12),
                Arguments.of("SCALEDOWN_COOLDOWN_MINS4", -1, 4, 4)
        );
    }

    @ParameterizedTest(name = "{0}: With coolDownMins={1}, scaleDownCoolDownMins ={2}, expectedScaleDownCoolDownMins={3} ")
    @MethodSource("scaleDownCoolDownMins")
    public void testScaleDownCoolDownMins(String testType, int coolDownMins, int scaleDownCoolDownMins, int expectedScaleDownCoolDownMins) {
        if (coolDownMins != -1) {
            underTest.setCoolDownMinutes(coolDownMins);
        }
        if (scaleDownCoolDownMins != -1) {
            underTest.setScaleDownCoolDownMinutes(scaleDownCoolDownMins);
        }
        assertEquals("ScaleDown CoolDown Mins should match : " + testType, expectedScaleDownCoolDownMins,
                TimeUnit.MINUTES.convert(underTest.getScaleDownCoolDownMillis(), TimeUnit.MILLISECONDS));
    }
}
