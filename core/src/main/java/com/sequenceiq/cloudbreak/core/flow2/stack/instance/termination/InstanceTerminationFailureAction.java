package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;

@Component("InstanceTerminationFailureAction")
public class InstanceTerminationFailureAction extends AbstractInstanceTerminationAction<RemoveInstanceResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFailureAction.class);
    @Inject
    private InstanceTerminationService instanceTerminationService;

    public InstanceTerminationFailureAction() {
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
    protected void doExecute(InstanceTerminationContext context, RemoveInstanceResult payload, Map<Object, Object> variables) {
        instanceTerminationService.handleInstanceTerminationError(context, payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new SelectableEvent(InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation());
    }
}