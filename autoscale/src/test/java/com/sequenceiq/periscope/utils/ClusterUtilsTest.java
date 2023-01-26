package com.sequenceiq.periscope.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

public class ClusterUtilsTest {

    @Test
    public void testGetRemainingCoolDownWhenNoScalingDone() {
        long remainingTime =
                ClusterUtils.getRemainingCooldownTime(30 * TimeUtil.MIN_IN_MS, 0);
        assertEquals(0, remainingTime, "Remaining Time should be 0 when no scaling");
    }

    @Test
    public void testGetRemainingCoolDownAfterCoolDownTimeThenGreaterThanZero() {
        long remainingTime =
                ClusterUtils.getRemainingCooldownTime(30 * TimeUtil.MIN_IN_MS, Instant.now().minus(35, ChronoUnit.MINUTES).toEpochMilli());
        assertTrue(remainingTime <= 0, "Remaining Time should be negative when cool down period elapsed.");
    }

    @Test
    public void testGetRemainingCoolDownBeforeCoolDownTimeThenLesserThanZero() {
        long remainingTime =
                ClusterUtils.getRemainingCooldownTime(30 * TimeUtil.MIN_IN_MS, Instant.now().minus(20, ChronoUnit.MINUTES).toEpochMilli());
        assertTrue(remainingTime > 0, "Remaining Time should be positive when cool down period not elapsed.");
    }
}
