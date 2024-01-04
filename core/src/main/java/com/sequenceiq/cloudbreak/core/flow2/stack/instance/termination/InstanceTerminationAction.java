package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.event.InstanceTerminationTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component("InstanceTerminationAction")
public class InstanceTerminationAction extends AbstractInstanceTerminationAction<InstanceTerminationTriggerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationAction.class);

    @Inject
    private InstanceTerminationService instanceTerminationService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackDtoService stackDtoService;

    public InstanceTerminationAction() {
        super(InstanceTerminationTriggerEvent.class);
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, InstanceTerminationTriggerEvent payload, Map<Object, Object> variables) {
        instanceTerminationService.instanceTermination(context);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        StackDto stackDto = stackDtoService.getById(context.getStackId());
        List<CloudResource> cloudResources = resourceService.getAllByStackId(context.getStackId()).stream()
                .map(r -> cloudResourceConverter.convert(r))
                .collect(Collectors.toList());
        CloudStack cloudStack = cloudStackConverter.convert(stackDto);
        return new RemoveInstanceRequest(context.getCloudContext(), context.getCloudCredential(), cloudStack,
                cloudResources, context.getCloudInstances());
    }
}
