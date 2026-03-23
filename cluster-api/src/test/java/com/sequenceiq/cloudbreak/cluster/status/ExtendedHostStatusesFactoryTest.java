package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.UNHEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.HOST;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.SERVICE_CONFIG_STALENESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.type.HealthCheck;

class ExtendedHostStatusesFactoryTest {

    @Test
    void createShouldWireInjectedProviders() {
        ExtendedHostStatusesFactory underTest = new ExtendedHostStatusesFactory(new StaleServiceConfigUnhealthyReasonProvider());
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("master0"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"), List.of("Knox"))),
                hostName("idbroker0"), Set.of(new HealthCheck(SERVICE_CONFIG_STALENESS, UNHEALTHY, Optional.of("stale"), List.of("Knox")))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.create(hostsHealth);

        String reason = extendedHostStatuses.getUnhealthyReasonWithType(SERVICE_CONFIG_STALENESS);
        assertTrue(
                "The following services are running with stale configurations: Knox (on idbroker0 and master0). "
                        .concat("Please redeploy client configurations or restart these services to apply the pending updates.")
                        .equals(reason)
                        || "The following services are running with stale configurations: Knox (on master0 and idbroker0). "
                        .concat("Please redeploy client configurations or restart these services to apply the pending updates.")
                        .equals(reason)
        );
    }

    @Test
    void createShouldWireDefaultHealthCheckTypeProvider() {
        ExtendedHostStatusesFactory underTest = new ExtendedHostStatusesFactory(new StaleServiceConfigUnhealthyReasonProvider());
        Map<com.sequenceiq.cloudbreak.cloud.model.HostName, Set<HealthCheck>> hostsHealth = Map.of(
                hostName("host1"), Set.of(new HealthCheck(HOST, UNHEALTHY, Optional.of("host is down"), List.of("cm")))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.create(hostsHealth);

        assertEquals("[host1]: host is down: cm", extendedHostStatuses.getUnhealthyReasonWithType(HOST));
    }
}