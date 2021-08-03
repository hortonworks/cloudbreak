package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public class SaltUpdateFinishedAction extends AbstractStackProvisionAction<InstallFreeIpaServicesSuccess> implements OperationAwareAction, FlowChainAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateFinishedAction.class);

    @Inject
    private OperationService operationService;

    @Inject
    private StackUpdater stackUpdater;

    public SaltUpdateFinishedAction() {
        super(InstallFreeIpaServicesSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) {
        stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE, "Salt update finished");
        if (isOperationIdSet(variables) && (!isChainedAction(variables) || isFinalChain(variables))) {
            LOGGER.debug("Complete operation with id: [{}]", getOperationId(variables));
            SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
            operationService.completeOperation(context.getStack().getAccountId(), getOperationId(variables), Set.of(successDetails), Set.of());
        }
        LOGGER.info("Salt state update finished successfully");
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT.event(), context.getStack().getId());
    }
}
