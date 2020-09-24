package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.action;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.AbstractCommonChainAction;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayContext;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractChangePrimaryGatewayAction<P extends Payload> extends
        AbstractCommonChainAction<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent, ChangePrimaryGatewayContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractChangePrimaryGatewayAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ChangePrimaryGatewayContext createFlowContext(FlowParameters flowParameters,
            StateContext<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        addMdcOperationIdIfPresent(stateContext.getExtendedState().getVariables());
        return new ChangePrimaryGatewayContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ChangePrimaryGatewayContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    protected DetailedStackStatus getChangePrimaryGatewayCompleteStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isFinalChain(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_COMPLETED;
        } else {
            stackStatus = DetailedStackStatus.REPAIR_IN_PROGRESS;
        }
        return stackStatus;
    }
}
