package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;

@Component("StackStartAction")
public class StackStartAction extends AbstractStackStartAction<StackStatusUpdateContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartAction.class);
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    public StackStartAction() {
        super(StackStatusUpdateContext.class);
    }

    @Override
    protected Long getStackId(StackStatusUpdateContext payload) {
        return payload.getStackId();
    }

    @Override
    protected void doExecute(StackStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        MDCBuilder.buildMdcContext(stack);
        stackUpdater.updateStackStatus(stack.getId(), Status.START_IN_PROGRESS, "Cluster infrastructure is now starting.");
        fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STARTING, Status.START_IN_PROGRESS.name());
        LOGGER.info("Assembling start request for stack: {}", stack);
        List<CloudInstance> instances = cloudInstanceConverter.convert(stack.getInstanceMetaDataAsList());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        StartInstancesRequest startRequest = new StartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), resources, instances);
        sendEvent(context.getFlowId(), startRequest.selector(), startRequest);
    }
}
