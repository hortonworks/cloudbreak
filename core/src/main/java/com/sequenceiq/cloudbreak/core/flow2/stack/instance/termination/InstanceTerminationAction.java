package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.core.flow.context.StackInstanceUpdateContext;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component("InstanceTerminationAction")
public class InstanceTerminationAction extends AbstractInstanceTerminationAction<StackInstanceUpdateContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationAction.class);
    @Inject
    private InstanceTerminationService instanceTerminationService;

    public InstanceTerminationAction() {
        super(StackInstanceUpdateContext.class);
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, StackInstanceUpdateContext payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        if (!stack.isDeleteInProgress()) {
            instanceTerminationService.instanceTermination(context);
            sendEvent(context);
        } else {
            LOGGER.info("Couldn't remove instance '{}' because other delete in progress", context.getCloudInstance().getInstanceId());
            sendEvent(context.getFlowId(), InstanceTerminationEvent.TERMINATION_FAILED_EVENT.stringRepresentation(),
                    getFailurePayload(payload, Optional.ofNullable(context), new IllegalStateException("Other delete operation in progress.")));
        }
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new RemoveInstanceRequest<>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                context.getCloudResources(), context.getCloudInstance());
    }
}
