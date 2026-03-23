package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.UNHEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.SERVICE_CONFIG_STALENESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.type.HealthCheck;

class StaleServiceConfigUnhealthyReasonProviderTest {

    private final StaleServiceConfigUnhealthyReasonProvider underTest = new StaleServiceConfigUnhealthyReasonProvider();

    @Test
    void getReasonReturnsFormattedGroupedMessage() {
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("master0"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"),
                        List.of("Knox", "Atlas", "HBase", "HDFS", "Hive Metastore", "Kafka", "Ranger", "Solr"))),
                hostName("master1"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"),
                        List.of("Knox"))),
                hostName("master2"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"),
                        List.of("Knox"))),
                hostName("idbroker0"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"),
                        List.of("Knox")))
        );

        assertEquals("The following services are running with stale configurations: Knox (on master[0-2] and idbroker0) and "
                        + "Atlas, HBase, HDFS, Hive Metastore, Kafka, Ranger, and Solr (on master0). "
                        + "Please redeploy client configurations or restart these services to apply the pending updates.",
                underTest.getReason(hostsHealth));
    }

    @Test
    void getReasonReturnsEmptyWhenNoDetailsFound() {
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("master0"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"), List.of()))
        );

        assertEquals("", underTest.getReason(hostsHealth));
    }
}

