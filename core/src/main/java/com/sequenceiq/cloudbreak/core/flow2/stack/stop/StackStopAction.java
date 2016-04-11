package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;

@Component("StackStopAction")
public class StackStopAction extends AbstractStackStopAction<StackStatusUpdateContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopAction.class);
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private StackStartStopService stackStartStopService;

    public StackStopAction() {
        super(StackStatusUpdateContext.class);
    }

    @Override
    protected void doExecute(StackStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
        stackStartStopService.startStackStop(context);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackStartStopContext context) {
        if (stackStartStopService.isStopPossible(context.getStack())) {
            List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData());
            List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
            return new StopInstancesRequest<StopInstancesResult>(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
        }
        //finish the flow
        return new SelectableFlowStackEvent(context.getStack().getId(), StackStopEvent.STOP_FAILURE_EVENT.stringRepresentation());
    }

}
