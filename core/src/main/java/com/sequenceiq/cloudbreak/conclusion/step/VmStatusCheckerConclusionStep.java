package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_UNHEALTHY_VMS_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_UNHEALTHY_VMS_FOUND_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.PROVIDER_NOT_RUNNING_VMS_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS;
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

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class VmStatusCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmStatusCheckerConclusionStep.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private StackInstanceStatusChecker stackInstanceStatusChecker;

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
            return checkProviderForInstanceStatuses(stack, runningInstances);
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
            String conclusion = cloudbreakMessagesService.getMessage(CM_UNHEALTHY_VMS_FOUND);
            String details = cloudbreakMessagesService.getMessageWithArgs(CM_UNHEALTHY_VMS_FOUND_DETAILS, unhealthyHosts, noReportHosts);
            LOGGER.warn(details);
            return failed(conclusion, details);
        } else {
            return succeeded();
        }
    }

    private Conclusion checkProviderForInstanceStatuses(Stack stack, List<InstanceMetadataView> runningInstances) {
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(runningInstances, stack);
        List<CloudVmInstanceStatus> instanceStatuses = stackInstanceStatusChecker.queryInstanceStatuses(stack, cloudInstances);
        LOGGER.debug("Instance statuses based on provider: {}", instanceStatuses);
        Map<String, InstanceSyncState> instanceSyncStates = instanceStatuses.stream()
                .collect(Collectors.toMap(i -> i.getCloudInstance().getInstanceId(), i -> InstanceSyncState.getInstanceSyncState(i.getStatus())));
        Set<String> notRunningInstances = runningInstances.stream()
                .filter(i -> !InstanceSyncState.RUNNING.equals(instanceSyncStates.getOrDefault(i.getInstanceId(), InstanceSyncState.UNKNOWN)))
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(Objects::nonNull)
                .collect(toSet());

        if (!notRunningInstances.isEmpty()) {
            String conclusion = cloudbreakMessagesService.getMessage(PROVIDER_NOT_RUNNING_VMS_FOUND);
            String details = cloudbreakMessagesService.getMessageWithArgs(PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS, notRunningInstances);
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
