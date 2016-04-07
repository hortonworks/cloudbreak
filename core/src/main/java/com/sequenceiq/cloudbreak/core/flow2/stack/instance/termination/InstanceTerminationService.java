package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.flow.ScalingFailedException;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

@Service
public class InstanceTerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationService.class);
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private StackScalingService stackScalingService;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private HostMetadataRepository hostMetadataRepository;

    public void instanceTermination(InstanceTerminationContext context) {
        Stack stack = context.getStack();
        if (!stack.isDeleteInProgress()) {
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Removing instance");
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_REMOVING_INSTANCE, UPDATE_IN_PROGRESS.name());
            InstanceMetaData instanceMetaData = context.getInstanceMetaData();
            String hostName = instanceMetaData.getDiscoveryFQDN();
            if (stack.getCluster() != null) {
                HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), hostName);
                if (hostMetadata != null && HostMetadataState.HEALTHY.equals(hostMetadata.getHostMetadataState())) {
                    throw new ScalingFailedException(String.format("Host (%s) is in HEALTHY state. Cannot be removed.", hostName));
                }
            }
            String instanceGroupName = instanceMetaData.getInstanceGroup().getGroupName();
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_SCALING_TERMINATING_HOST_FROM_HOSTGROUP, UPDATE_IN_PROGRESS.name(),
                    hostName, instanceGroupName);
        }
    }

    public void finishInstanceTermination(InstanceTerminationContext context, RemoveInstanceResult payload) throws Exception {
        Stack stack = context.getStack();
        InstanceMetaData instanceMetaData = context.getInstanceMetaData();
        String instanceId = instanceMetaData.getInstanceId();
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupId(instanceMetaData.getInstanceGroup().getId());
        stackScalingService.updateRemovedResourcesState(stack, Collections.singleton(instanceId), instanceGroup);
        if (stack.getCluster() != null) {
            HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN());
            if (hostMetadata != null) {
                LOGGER.info("Remove obsolete host: {}", hostMetadata.getHostName());
                stackScalingService.removeHostmetadataIfExists(stack, instanceMetaData, hostMetadata);
            }
        }
        LOGGER.info("Terminate instance result: {}", payload);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Instance removed");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_REMOVING_INSTANCE_FINISHED, AVAILABLE.name());
    }

    public void handleInstanceTerminationError(InstanceTerminationContext context, RemoveInstanceResult payload) {
        LOGGER.error("Error during instance terminating flow:", payload.getErrorDetails());
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Stack update failed. " + payload.getStatusReason());
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, AVAILABLE.name(), payload.getStatusReason());
    }
}
