package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.common.type.HealthCheckType.SERVICE_CONFIG_STALENESS;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class ExtendedHostStatusesFactory {

    private final Map<HealthCheckType, UnhealthyReasonProvider> unhealthyReasonProviders;

    public ExtendedHostStatusesFactory(StaleServiceConfigUnhealthyReasonProvider staleServiceConfigUnhealthyReasonProvider) {
        Map<HealthCheckType, UnhealthyReasonProvider> providerMap = new EnumMap<>(HealthCheckType.class);
        providerMap.put(SERVICE_CONFIG_STALENESS, staleServiceConfigUnhealthyReasonProvider);
        unhealthyReasonProviders = providerMap;
    }

    public ExtendedHostStatuses create(Map<HostName, Set<HealthCheck>> hostsHealth) {
        return new ExtendedHostStatuses(hostsHealth, unhealthyReasonProviders);
    }
}

