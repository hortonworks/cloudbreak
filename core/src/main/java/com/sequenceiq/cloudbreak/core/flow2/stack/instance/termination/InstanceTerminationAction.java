package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.core.flow2.event.InstanceTerminationTriggerEvent;

@Component("InstanceTerminationAction")
public class InstanceTerminationAction extends AbstractInstanceTerminationAction<InstanceTerminationTriggerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationAction.class);
    @Inject
    private InstanceTerminationService instanceTerminationService;

    public InstanceTerminationAction() {
        super(InstanceTerminationTriggerEvent.class);
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, InstanceTerminationTriggerEvent payload, Map<Object, Object> variables) throws Exception {
        instanceTerminationService.instanceTermination(context);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new RemoveInstanceRequest<>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                context.getCloudResources(), context.getCloudInstance());
    }
}
