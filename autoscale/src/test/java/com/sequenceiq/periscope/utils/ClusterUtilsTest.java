package com.sequenceiq.periscope.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

public class ClusterUtilsTest {

    @Test
    public void testGetRemainingCoolDownWhenNoScalingDone() {
        long remainingTime =
                ClusterUtils.getRemainingCooldownTime(30 * TimeUtil.MIN_IN_MS, 0);
        assertEquals("Remaining Time should be 0 when no scaling", 0, remainingTime);
    }

    @Test
    public void testGetRemainingCoolDownAfterCoolDownTimeThenGreaterThanZero() {
        long remainingTime =
                ClusterUtils.getRemainingCooldownTime(30 * TimeUtil.MIN_IN_MS, Instant.now().minus(35, ChronoUnit.MINUTES).toEpochMilli());
        assertTrue("Remaining Time should be negative when cool down period elapsed.", remainingTime <= 0);
    }

    @Test
    public void testGetRemainingCoolDownBeforeCoolDownTimeThenLesserThanZero() {
        long remainingTime =
                ClusterUtils.getRemainingCooldownTime(30 * TimeUtil.MIN_IN_MS, Instant.now().minus(20, ChronoUnit.MINUTES).toEpochMilli());
        assertTrue("Remaining Time should be positive when cool down period not elapsed.", remainingTime > 0);
    }
}
