package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component("StartProvisioningAction")
public class StartProvisioningAction  extends AbstractStackCreationAction<FlowStackEvent> {
    @Inject
    private StackCreationService stackCreationService;

    public StartProvisioningAction() {
        super(FlowStackEvent.class);
    }

    @Override
    protected void doExecute(final StackContext context, FlowStackEvent payload, Map<Object, Object> variables) {
        variables.put(StackProvisionConstants.START_DATE, new Date());
        stackCreationService.startProvisioning(context);
        FailurePolicy policy = Optional.fromNullable(context.getStack().getFailurePolicy()).or(new FailurePolicy());
        sendEvent(context.getFlowId(), new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                policy.getAdjustmentType(), policy.getThreshold()));
    }

    @Override
    protected Long getStackId(FlowStackEvent payload) {
        return payload.getStackId();
    }
}
