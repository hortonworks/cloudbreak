package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.HEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.UNHEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.HOST;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.SERVICES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.type.HealthCheck;

class DefaultUnhealthyReasonProviderTest {

    private final DefaultUnhealthyReasonProvider underTest = new DefaultUnhealthyReasonProvider(HOST);

    @Test
    void getReasonReturnsFormattedMessageForSingleHost() {
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("host1"), Set.of(new HealthCheck(HOST, UNHEALTHY, Optional.of("host is down"), List.of("cm", "agent")))
        );

        assertEquals("[host1]: host is down: cm, agent", underTest.getReason(hostsHealth));
    }

    @Test
    void getReasonReturnsAllDistinctMessagesJoined() {
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("host1"), Set.of(new HealthCheck(HOST, UNHEALTHY, Optional.of("host is down"), List.of("cm"))),
                hostName("host2"), Set.of(new HealthCheck(HOST, UNHEALTHY, Optional.of("dns issue"), List.of("resolver")))
        );

        String reason = underTest.getReason(hostsHealth);

        assertTrue(reason.contains("[host1]: host is down: cm") || reason.contains("[host2]: host is down: cm"));
        assertTrue(reason.contains("[host2]: dns issue: resolver") || reason.contains("[host1]: dns issue: resolver"));
        assertTrue(reason.contains(". "));
    }

    @Test
    void getReasonReturnsEmptyWhenNoMatchingUnhealthyChecksFound() {
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("host1"), Set.of(new HealthCheck(HOST, HEALTHY, Optional.of("ok"), List.of("cm"))),
                hostName("host2"), Set.of(new HealthCheck(SERVICES, UNHEALTHY, Optional.of("service down"), List.of("hdfs")))
        );

        assertEquals("", underTest.getReason(hostsHealth));
    }
}

