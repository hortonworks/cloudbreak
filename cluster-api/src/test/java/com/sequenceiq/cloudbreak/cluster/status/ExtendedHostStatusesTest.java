package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.HEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.UNHEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.CERTIFICATE;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.HOST;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.SERVICES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

public class ExtendedHostStatusesTest {

    private static final Set<HealthCheck> NULL_SET = null;

    private static final HostName TEST_HOST = hostName("host1");

    private static final HostName TEST_HOST_2 = hostName("host2");

    @Test
    public void testCheckIfThereIsExpiredCert() {
        Map<HostName, Set<HealthCheck>> emptyMap = Maps.newHashMap();
        emptyMap.put(TEST_HOST, NULL_SET);
        assertFalse(new ExtendedHostStatuses(emptyMap).isAnyUnhealthyWithType(CERTIFICATE));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, UNHEALTHY)))).isAnyUnhealthyWithType(CERTIFICATE));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(CERTIFICATE, HEALTHY)))).isAnyUnhealthyWithType(CERTIFICATE));
        assertTrue(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(CERTIFICATE, UNHEALTHY)))).isAnyUnhealthyWithType(CERTIFICATE));
        assertTrue(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(CERTIFICATE, HEALTHY)),
                TEST_HOST_2, Set.of(createHealthCheck(CERTIFICATE, UNHEALTHY)))).isAnyUnhealthyWithType(CERTIFICATE));
    }

    @Test
    public void testCheckIfHostHealthy() {
        Map<HostName, Set<HealthCheck>> emptyMap = Maps.newHashMap();
        emptyMap.put(TEST_HOST, NULL_SET);
        assertTrue(new ExtendedHostStatuses(emptyMap).isHostHealthy(TEST_HOST));
        assertTrue(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, HEALTHY)))).isHostHealthy(TEST_HOST));
        assertTrue(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(SERVICES, HEALTHY)))).isHostHealthy(TEST_HOST));
        assertTrue(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(CERTIFICATE, UNHEALTHY)))).isHostHealthy(TEST_HOST));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, UNHEALTHY)))).isHostHealthy(TEST_HOST));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(SERVICES, UNHEALTHY)))).isHostHealthy(TEST_HOST));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, UNHEALTHY), createHealthCheck(SERVICES, HEALTHY)),
                TEST_HOST_2, Set.of(createHealthCheck(HOST, HEALTHY), createHealthCheck(SERVICES, HEALTHY)))).isHostHealthy(TEST_HOST));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, HEALTHY), createHealthCheck(SERVICES, UNHEALTHY)),
                TEST_HOST_2, Set.of(createHealthCheck(HOST, HEALTHY), createHealthCheck(SERVICES, HEALTHY)))).isHostHealthy(TEST_HOST));
        assertFalse(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, UNHEALTHY), createHealthCheck(SERVICES, UNHEALTHY)),
                TEST_HOST_2, Set.of(createHealthCheck(HOST, HEALTHY), createHealthCheck(SERVICES, HEALTHY)))).isHostHealthy(TEST_HOST));
        assertTrue(new ExtendedHostStatuses(Map.of(TEST_HOST, Set.of(createHealthCheck(HOST, UNHEALTHY), createHealthCheck(SERVICES, HEALTHY)),
                TEST_HOST_2, Set.of(createHealthCheck(HOST, HEALTHY), createHealthCheck(SERVICES, HEALTHY)))).isHostHealthy(TEST_HOST_2));
    }

    @Test
    public void testGetReason() {
        assertEquals("reason2 reason3 reason1", new ExtendedHostStatuses(Map.of(TEST_HOST,
                Set.of(createHealthCheck(SERVICES, UNHEALTHY, "reason3"),
                        createHealthCheck(CERTIFICATE, UNHEALTHY, "reason1"),
                        createHealthCheck(HOST, UNHEALTHY, "reason2")))).statusReasonForHost(TEST_HOST));
        assertEquals("", new ExtendedHostStatuses(Map.of(TEST_HOST,
                Set.of(createHealthCheck(SERVICES, HEALTHY, "reason3"),
                        createHealthCheck(CERTIFICATE, HEALTHY, "reason1"),
                        createHealthCheck(HOST, HEALTHY, "reason2")))).statusReasonForHost(TEST_HOST));
        assertEquals("reason1 reason2 reason3", new ExtendedHostStatuses(Map.of(TEST_HOST,
                Set.of(createHealthCheck(SERVICES, UNHEALTHY, "reason2"),
                        createHealthCheck(CERTIFICATE, UNHEALTHY, "reason3"),
                        createHealthCheck(HOST, UNHEALTHY, "reason1")))).statusReasonForHost(TEST_HOST));
        ExtendedHostStatuses extendedHostStatuses2 = new ExtendedHostStatuses(Map.of(TEST_HOST,
                Set.of(createHealthCheck(SERVICES, UNHEALTHY, "reason2"),
                        createHealthCheck(CERTIFICATE, UNHEALTHY, "reason3"),
                        createHealthCheck(HOST, UNHEALTHY, "reason1")),
                TEST_HOST_2, Set.of(createHealthCheck(HOST, UNHEALTHY, "whatever"))));
        assertEquals("reason1 reason2 reason3", extendedHostStatuses2.statusReasonForHost(TEST_HOST));
        assertEquals("whatever", extendedHostStatuses2.statusReasonForHost(TEST_HOST_2));
    }

    private HealthCheck createHealthCheck(HealthCheckType type, HealthCheckResult result) {
        return new HealthCheck(type, result, Optional.empty(), List.of());
    }

    private HealthCheck createHealthCheck(HealthCheckType type, HealthCheckResult result, String reason) {
        return new HealthCheck(type, result, Optional.of(reason), List.of());
    }
}
