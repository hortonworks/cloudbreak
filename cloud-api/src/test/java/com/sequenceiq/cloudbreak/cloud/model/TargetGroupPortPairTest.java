package com.sequenceiq.cloudbreak.cloud.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TargetGroupPortPairTest {

    private static final int TRAFFIC_PORT = 443;

    private static final int HEALTH_PORT = 8443;

    @Test
    public void testEquals() {
        HealthProbeParameters healthProbeParameters = new HealthProbeParameters("", HEALTH_PORT, NetworkProtocol.TCP, 0, 0);
        TargetGroupPortPair a = new TargetGroupPortPair(TRAFFIC_PORT, NetworkProtocol.TCP, healthProbeParameters);
        TargetGroupPortPair b = new TargetGroupPortPair(TRAFFIC_PORT, NetworkProtocol.TCP, healthProbeParameters);
        assert a.equals(b);
        assert b.equals(a);
    }

    @Test
    public void testHashCode() {
        HealthProbeParameters healthProbeParameters = new HealthProbeParameters("", HEALTH_PORT, NetworkProtocol.TCP, 0, 0);
        TargetGroupPortPair a = new TargetGroupPortPair(TRAFFIC_PORT, NetworkProtocol.TCP, healthProbeParameters);
        TargetGroupPortPair b = new TargetGroupPortPair(TRAFFIC_PORT, NetworkProtocol.TCP, healthProbeParameters);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testSetKey() {
        HealthProbeParameters healthProbeParameters = new HealthProbeParameters("", HEALTH_PORT, NetworkProtocol.TCP, 0, 0);
        TargetGroupPortPair a = new TargetGroupPortPair(TRAFFIC_PORT, NetworkProtocol.TCP, healthProbeParameters);
        TargetGroupPortPair b = new TargetGroupPortPair(TRAFFIC_PORT, NetworkProtocol.TCP, healthProbeParameters);
        Set<TargetGroupPortPair> set = new HashSet<>();
        set.add(a);
        set.add(b);
        assertEquals(1, set.size());
    }
}
