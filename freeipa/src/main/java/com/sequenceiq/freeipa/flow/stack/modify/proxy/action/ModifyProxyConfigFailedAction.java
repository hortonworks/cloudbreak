package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("ModifyProxyConfigFailedAction")
public class ModifyProxyConfigFailedAction extends ModifyProxyConfigAction<StackFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigFailedAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    public ModifyProxyConfigFailedAction() {
        super(StackFailureEvent.class);
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Failed modify proxy config state", payload.getException());
        stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MODIFY_PROXY_CONFIG_FAILED, getErrorReason(payload.getException()));
        LOGGER.debug("Failing operation with id: [{}]", getOperationId(variables));
        operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables), payload.getException().getMessage());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new StackEvent(ModifyProxyConfigEvent.MODIFY_PROXY_FAILURE_HANDLED_EVENT.selector(), context.getStack().getId());
    }
}