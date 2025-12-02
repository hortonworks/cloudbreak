package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class ModifyProxyConfigAction<P extends StackEvent>
        extends AbstractStackAction<ModifyProxyConfigState, ModifyProxyConfigEvent, ModifyProxyConfigContext, P>
        implements OperationAwareAction, FlowChainAwareAction {

    @Inject
    private StackService stackService;

    protected ModifyProxyConfigAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ModifyProxyConfigContext createFlowContext(FlowParameters flowParameters, StateContext<ModifyProxyConfigState,
            ModifyProxyConfigEvent> stateContext, P payload) {
        Stack stack = stackService.getStackById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        return new ModifyProxyConfigContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ModifyProxyConfigContext> flowContext, Exception ex) {
        return new StackFailureEvent(ModifyProxyConfigEvent.MODIFY_PROXY_FAILED_EVENT.selector(), payload.getResourceId(), ex, ERROR);
    }
}
