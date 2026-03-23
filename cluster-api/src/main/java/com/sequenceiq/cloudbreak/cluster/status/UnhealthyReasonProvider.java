package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;

@FunctionalInterface
public interface UnhealthyReasonProvider {

    String getReason(Map<HostName, Set<HealthCheck>> hostsHealth);
}

