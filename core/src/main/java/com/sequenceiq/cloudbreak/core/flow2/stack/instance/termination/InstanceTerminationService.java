package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REMOVING_INSTANCE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REMOVING_INSTANCE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REMOVING_INSTANCE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SCALING_TERMINATING_HOST_FROM_HOSTGROUP;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class InstanceTerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void instanceTermination(InstanceTerminationContext context) {
        StackView stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REMOVE_INSTANCE, "Removing instance");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_REMOVING_INSTANCE);
        List<InstanceMetadataView> instanceMetaDataList = context.getInstanceMetaDataList();
        for (InstanceMetadataView instanceMetaData : instanceMetaDataList) {
            String hostName = instanceMetaData.getDiscoveryFQDN();
            if (hostName != null) {
                String instanceGroupName = instanceMetaData.getInstanceGroupName();
                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_SCALING_TERMINATING_HOST_FROM_HOSTGROUP,
                        hostName, instanceGroupName);
            }
        }
    }

    public void finishInstanceTermination(InstanceTerminationContext context, RemoveInstanceResult payload)
            throws TransactionService.TransactionExecutionException {
        StackView stack = context.getStack();
        List<InstanceMetadataView> instanceMetaDataList = context.getInstanceMetaDataList();
        Set<Long> privateIds = instanceMetaDataList.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
        stackScalingService.updateInstancesToTerminated(privateIds, stack.getId());
        LOGGER.debug("Terminate instance result: {}", payload);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Instance removed");
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_REMOVING_INSTANCE_FINISHED);
    }

    public void handleInstanceTerminationError(long stackId, StackFailureEvent payload) {
        Exception ex = payload.getException();
        LOGGER.info("Error during instance terminating flow:", ex);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Instance termination failed. " + ex.getMessage());
        flowMessageService.fireEventAndLog(stackId, DELETE_FAILED.name(), STACK_REMOVING_INSTANCE_FAILED, ex.getMessage());
    }
}
