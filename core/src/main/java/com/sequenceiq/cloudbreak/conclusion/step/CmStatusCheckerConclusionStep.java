package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_SERVER_UNREACHABLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_UNHEALTHY_VMS_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_UNHEALTHY_VMS_FOUND_DETAILS;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class CmStatusCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmStatusCheckerConclusionStep.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public Conclusion check(Long resourceId) {
        Stack stack = stackService.getById(resourceId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        List<InstanceMetadataView> runningInstances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId());
        if (isClusterManagerRunning(stack, connector)) {
            return checkCMForInstanceStatuses(connector, runningInstances, stack.getCluster().getId());
        } else {
            String conclusion = cloudbreakMessagesService.getMessage(CM_SERVER_UNREACHABLE);
            LOGGER.warn(conclusion);
            return failed(conclusion, conclusion);
        }
    }

    private Conclusion checkCMForInstanceStatuses(ClusterApi connector, List<InstanceMetadataView> runningInstances, Long clusterId) {
        ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses(
                runtimeVersionService.getRuntimeVersion(clusterId));
        Map<HostName, Set<HealthCheck>> hostStatuses = extendedHostStatuses.getHostsHealth();
        LOGGER.debug("Instance statuses based on CM: {}", hostStatuses);
        Map<String, String> unhealthyHosts = hostStatuses.keySet().stream()
                .filter(hostName -> !extendedHostStatuses.isHostHealthy(hostName))
                .collect(Collectors.toMap(StringType::value, extendedHostStatuses::statusReasonForHost));
        Set<String> noReportHosts = runningInstances.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(Objects::nonNull)
                .filter(discoveryFQDN -> !hostStatuses.containsKey(hostName(discoveryFQDN)))
                .collect(toSet());

        if (!unhealthyHosts.isEmpty() || !noReportHosts.isEmpty()) {
            String conclusion = cloudbreakMessagesService.getMessageWithArgs(CM_UNHEALTHY_VMS_FOUND, unhealthyHosts, noReportHosts);
            String details = cloudbreakMessagesService.getMessageWithArgs(CM_UNHEALTHY_VMS_FOUND_DETAILS, unhealthyHosts, noReportHosts);
            LOGGER.warn(details);
            return failed(conclusion, details);
        } else {
            return succeeded();
        }
    }

    private boolean isClusterManagerRunning(Stack stack, ClusterApi connector) {
        return !stack.isStopped()
                && !stack.isStackInDeletionOrFailedPhase()
                && isCMRunning(connector);
    }

    private boolean isCMRunning(ClusterApi connector) {
        return connector.clusterStatusService().isClusterManagerRunningQuickCheck();
    }
}
