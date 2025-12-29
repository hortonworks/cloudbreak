package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.util.SaltUpdateSkipHighstateFlagUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesRequest;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public class UpdateSaltFilesAction extends AbstractStackProvisionAction<SaltUpdateTriggerEvent> implements OperationAwareAction, FlowChainAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSaltFilesAction.class);

    @Inject
    private StackUpdater stackUpdater;

    public UpdateSaltFilesAction(Class<SaltUpdateTriggerEvent> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected void prepareExecution(SaltUpdateTriggerEvent payload, Map<Object, Object> variables) {
        SaltUpdateSkipHighstateFlagUtil.putToVariables(payload.isSkipHighstate(), variables);
    }

    @Override
    protected void doExecute(StackContext context, SaltUpdateTriggerEvent payload, Map<Object, Object> variables) {
        stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.SALT_STATE_UPDATE_IN_PROGRESS, "Salt state update in progress");
        setOperationId(variables, payload.getOperationId());
        setChainedAction(variables, payload.isChained());
        setFinalChain(variables, payload.isFinalChain());
        LOGGER.info("Reupload salt state files");
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return new BootstrapMachinesRequest(context.getStack().getId());
    }
}