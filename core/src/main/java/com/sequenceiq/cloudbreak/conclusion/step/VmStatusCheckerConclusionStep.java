package com.sequenceiq.cloudbreak.conclusion.step;

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
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
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
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public Conclusion check(Long resourceId) {
        Stack stack = stackService.getById(resourceId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        List<InstanceMetadataView> runningInstances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId());
        return checkProviderForInstanceStatuses(stack, runningInstances);
    }

    private Conclusion checkProviderForInstanceStatuses(Stack stack, List<InstanceMetadataView> runningInstances) {
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(runningInstances, stack);
        List<CloudVmInstanceStatus> instanceStatuses = stackInstanceStatusChecker.queryInstanceStatuses(stack, cloudInstances);
        LOGGER.debug("Instance statuses based on provider: {}", instanceStatuses);
        Map<Long, InstanceSyncState> instanceSyncStates = instanceStatuses.stream().collect(Collectors.toMap(
                i -> i.getCloudInstance().getParameter(CloudInstance.ID, Long.class),
                i -> InstanceSyncState.getInstanceSyncState(i.getStatus())));
        Set<String> notRunningInstances = runningInstances.stream()
                .filter(i -> !InstanceSyncState.RUNNING.equals(instanceSyncStates.getOrDefault(i.getId(), InstanceSyncState.UNKNOWN)))
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(Objects::nonNull)
                .collect(toSet());

        Set<String> unknownVms = runningInstances.stream()
                .filter(instanceMetadata -> instanceMetadata.getDiscoveryFQDN() == null)
                .map(instanceMetadata -> String.format("privateId: %s", instanceMetadata.getPrivateId()))
                .collect(toSet());

        if (!notRunningInstances.isEmpty() || !unknownVms.isEmpty()) {
            String conclusion = cloudbreakMessagesService.getMessageWithArgs(PROVIDER_NOT_RUNNING_VMS_FOUND, notRunningInstances, unknownVms);
            String details = cloudbreakMessagesService.getMessageWithArgs(PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS, notRunningInstances, unknownVms);
            LOGGER.warn(details);
            return failed(conclusion, details);
        } else {
            return succeeded();
        }
    }
}
