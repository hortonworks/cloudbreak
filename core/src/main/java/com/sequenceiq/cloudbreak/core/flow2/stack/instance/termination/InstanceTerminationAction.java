package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow.context.StackInstanceUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.ScalingFailedException;

@Component("InstanceTerminationAction")
public class InstanceTerminationAction extends AbstractInstanceTerminationAction<StackInstanceUpdateContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService messagesService;

    public InstanceTerminationAction() {
        super(StackInstanceUpdateContext.class);
    }

    @Override
    protected Long getStackId(StackInstanceUpdateContext payload) {
        return payload.getStackId();
    }

    @Override
    protected String getInstanceId(StackInstanceUpdateContext payload) {
        return payload.getInstanceId();
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, StackInstanceUpdateContext payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        if (!stack.isDeleteInProgress()) {
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Removing instance");
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                    messagesService.getMessage(Msg.STACK_REMOVING_INSTANCE.code()));
            InstanceMetaData instanceMetaData = context.getInstanceMetaData();
            String hostName = instanceMetaData.getDiscoveryFQDN();
            if (stack.getCluster() != null) {
                HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), hostName);
                if (hostMetadata != null && HostMetadataState.HEALTHY.equals(hostMetadata.getHostMetadataState())) {
                    throw new ScalingFailedException(String.format("Host (%s) is in HEALTHY state. Cannot be removed.", hostName));
                }
            }
            String instanceGroupName = instanceMetaData.getInstanceGroup().getGroupName();
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                    messagesService.getMessage(Msg.STACK_SCALING_TERMINATING_HOST_FROM_HOSTGROUP.code(), Arrays.asList(hostName, instanceGroupName)));
            RemoveInstanceRequest<RemoveInstanceResult> removeInstanceRequest = new RemoveInstanceRequest<>(context.getCloudContext(),
                    context.getCloudCredential(), context.getCloudStack(), context.getCloudResources(), context.getCloudInstance());
            sendEvent(context.getFlowId(), removeInstanceRequest.selector(), removeInstanceRequest);
        } else {
            LOGGER.info("Couldn't remove instance '{}' because other delete in progress", context.getCloudInstance().getInstanceId());
            sendEvent(context.getFlowId(), InstanceTerminationEvent.TERMINATION_FAILED_EVENT.stringRepresentation(),
                    getFailurePayload(context, new IllegalStateException("Other delete operation in progress.")));
        }
    }
}
