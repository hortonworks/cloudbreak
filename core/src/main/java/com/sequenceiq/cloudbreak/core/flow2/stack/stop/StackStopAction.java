package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.service.StackStopService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;

@Component("StackStopAction")
public class StackStopAction extends AbstractStackStopAction<StackStatusUpdateContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopAction.class);
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private StackStopService stackStopService;
    @Inject
    private StackUpdater stackUpdater;

    public StackStopAction() {
        super(StackStatusUpdateContext.class);
    }

    @Override
    protected Long getStackId(StackStatusUpdateContext payload) {
        return payload.getStackId();
    }

    @Override
    protected void doExecute(StackStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        if (stackStopService.isStopPossible(stack)) {
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(context.getStack().getId(), Status.STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.");
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STOPPING, Status.STOP_IN_PROGRESS.name());
            List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData());
            List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
            StopInstancesRequest<StopInstancesResult> stopRequest = new StopInstancesRequest<>(context.getCloudContext(), context.getCloudCredential(),
                    cloudResources, cloudInstances);
            sendEvent(context.getFlowId(), stopRequest.selector(), stopRequest);
        }
    }

}
