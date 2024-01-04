package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("ModifyProxyConfigFinishedAction")
public class ModifyProxyConfigFinishedAction extends ModifyProxyConfigAction<StackEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigFinishedAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    public ModifyProxyConfigFinishedAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Finished modify proxy config state");
        stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE,
                "Successfully updated proxy config settings on all instances");
        LOGGER.debug("Complete operation with id: [{}]", getOperationId(variables));
        SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
        if (isOperationIdSet(variables) && (!isChainedAction(variables) || isFinalChain(variables))) {
            operationService.completeOperation(context.getStack().getAccountId(), getOperationId(variables), Set.of(successDetails), Set.of());
        }
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new StackEvent(ModifyProxyConfigEvent.MODIFY_PROXY_FINISHED_EVENT.selector(), context.getStack().getId());
    }
}
