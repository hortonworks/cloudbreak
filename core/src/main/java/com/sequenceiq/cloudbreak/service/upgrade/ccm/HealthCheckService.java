package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class HealthCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(delay = 5000L))
    public Set<String> getUnhealthyHosts(Long stackId) {
        Stack stack = stackService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        LOGGER.debug("Fetching extended host statuses for stack {} with retries", stack.getName());
        ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses(
                runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()));
        LOGGER.debug("Returned statuses: {}", extendedHostStatuses);
        return extendedHostStatuses.getHostsHealth().entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(hc -> hc.getType() == HealthCheckType.HOST && hc.getResult() == HealthCheckResult.UNHEALTHY))
                .map(e -> e.getKey().value())
                .collect(Collectors.toSet());
    }

}
