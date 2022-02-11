package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractUpgradeCcmAction<P extends StackEvent> extends AbstractAction<UpgradeCcmState, UpgradeCcmStateSelector, UpgradeCcmContext, P>
        implements OperationAwareAction {

    @Inject
    private StackService stackService;

    protected AbstractUpgradeCcmAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected UpgradeCcmContext createFlowContext(FlowParameters flowParameters, StateContext<UpgradeCcmState, UpgradeCcmStateSelector> stateContext,
            P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        return new UpgradeCcmContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpgradeCcmContext> flowContext, Exception e) {
        return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_EVENT.event(), payload.getResourceId(), e);
    }

}
