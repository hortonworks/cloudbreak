package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

@Component("StackStartAction")
public class StackStartAction extends AbstractStackStartAction<StackStatusUpdateContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartAction.class);
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private StackStartStopService stackStartStopService;

    public StackStartAction() {
        super(StackStatusUpdateContext.class);
    }

    @Override
    protected void doExecute(StackStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
        stackStartStopService.startStackStart(context);
        sendEvent(context);
    }

    @Override
    protected StartInstancesRequest createRequest(StackStartStopContext context) {
        LOGGER.info("Assembling start request for stack: {}", context.getStack());
        List<CloudInstance> instances = cloudInstanceConverter.convert(context.getStack().getInstanceMetaDataAsList());
        List<CloudResource> resources = cloudResourceConverter.convert(context.getStack().getResources());
        return new StartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), resources, instances);
    }
}
