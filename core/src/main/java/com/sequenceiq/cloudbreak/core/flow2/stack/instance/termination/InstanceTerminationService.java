package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
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
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private HostMetadataService hostMetadataService;

    public void instanceTermination(InstanceTerminationContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REMOVE_INSTANCE, "Removing instance");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_REMOVING_INSTANCE, UPDATE_IN_PROGRESS.name());
        List<InstanceMetaData> instanceMetaDataList = context.getInstanceMetaDataList();
        for (InstanceMetaData instanceMetaData : instanceMetaDataList) {
            String hostName = instanceMetaData.getDiscoveryFQDN();
            if (stack.getCluster() != null) {
                Optional<HostMetadata> hostMetadata = hostMetadataService.findHostInClusterByName(stack.getCluster().getId(), hostName);
                if (hostMetadata.isEmpty()) {
                    LOGGER.debug("Nothing to remove since hostmetadata is null");
                } else if (HostMetadataState.HEALTHY.equals(hostMetadata.get().getHostMetadataState())) {
                    throw new ScalingFailedException(String.format("Host (%s) is in HEALTHY state. Cannot be removed.", hostName));
                }
            }
            if (hostName != null) {
                String instanceGroupName = instanceMetaData.getInstanceGroup().getGroupName();
                flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_SCALING_TERMINATING_HOST_FROM_HOSTGROUP, UPDATE_IN_PROGRESS.name(),
                        hostName, instanceGroupName);
            }
        }
    }

    public void finishInstanceTermination(InstanceTerminationContext context, RemoveInstanceResult payload) throws TransactionExecutionException {
        Stack stack = context.getStack();
        List<InstanceMetaData> instanceMetaDataList = context.getInstanceMetaDataList();
        for (InstanceMetaData instanceMetaData : instanceMetaDataList) {
            String instanceId = instanceMetaData.getInstanceId();
            InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupId(instanceMetaData.getInstanceGroup().getId());
            stackScalingService.updateRemovedResourcesState(stack, Collections.singleton(instanceId), instanceGroup);
            if (stack.getCluster() != null) {
                Optional<HostMetadata> hostMetadata = hostMetadataService.findHostInClusterByName(stack.getCluster().getId(),
                        instanceMetaData.getDiscoveryFQDN());
                if (hostMetadata.isPresent()) {
                    LOGGER.debug("Remove obsolete host: {}", hostMetadata.get().getHostName());
                    stackScalingService.removeHostmetadataIfExists(stack, instanceMetaData, hostMetadata.get());
                }
            }
        }
        LOGGER.debug("Terminate instance result: {}", payload);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Instance removed");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_REMOVING_INSTANCE_FINISHED, AVAILABLE.name());
    }

    public void handleInstanceTerminationError(long stackId, StackFailureEvent payload) {
        Exception ex = payload.getException();
        LOGGER.info("Error during instance terminating flow:", ex);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Instance termination failed. " + ex.getMessage());
        flowMessageService.fireEventAndLog(stackId, Msg.STACK_REMOVING_INSTANCE_FAILED, DELETE_FAILED.name(), ex.getMessage());
    }
}
