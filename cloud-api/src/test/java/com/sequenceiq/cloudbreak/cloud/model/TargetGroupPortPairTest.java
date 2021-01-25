package com.sequenceiq.cloudbreak.cloud.model;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TargetGroupPortPairTest {

    private static final int TRAFFIC_PORT = 443;

    private static final int HEALTH_PORT = 8443;

    @Test
    public void testEquals() {
        TargetGroupPortPair a = new TargetGroupPortPair(TRAFFIC_PORT, HEALTH_PORT);
        TargetGroupPortPair b = new TargetGroupPortPair(TRAFFIC_PORT, HEALTH_PORT);
        assert a.equals(b);
        assert b.equals(a);
    }

    @Test
    public void testHashCode() {
        TargetGroupPortPair a = new TargetGroupPortPair(TRAFFIC_PORT, HEALTH_PORT);
        TargetGroupPortPair b = new TargetGroupPortPair(TRAFFIC_PORT, HEALTH_PORT);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testSetKey() {
        TargetGroupPortPair a = new TargetGroupPortPair(TRAFFIC_PORT, HEALTH_PORT);
        TargetGroupPortPair b = new TargetGroupPortPair(TRAFFIC_PORT, HEALTH_PORT);
        Set<TargetGroupPortPair> set = new HashSet<>();
        set.add(a);
        set.add(b);
        assertEquals(1, set.size());
    }
}
