package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

@Component("InstanceTerminationFinishedAction")
public class InstanceTerminationFinishedAction extends AbstractInstanceTerminationAction<RemoveInstanceResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFinishedAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService messagesService;

    public InstanceTerminationFinishedAction() {
        super(RemoveInstanceResult.class);
    }

    @Override
    protected Long getStackId(RemoveInstanceResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }

    @Override
    protected String getInstanceId(RemoveInstanceResult payload) {
        CloudInstance cloudInstance = payload.getCloudInstance();
        return cloudInstance == null ? null : cloudInstance.getInstanceId();
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, RemoveInstanceResult payload, Map<Object, Object> variables) throws Exception {
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
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), messagesService.getMessage(Msg.STACK_REMOVING_INSTANCE_FINISHED.code()));
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new SelectableEvent(InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation());
    }
}
