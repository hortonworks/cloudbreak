package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.AbstractUpgradeCcmEventHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractUpgradeCcmAction<P extends StackEvent> extends AbstractAction<UpgradeCcmState, UpgradeCcmStateSelector, UpgradeCcmContext, P>
        implements OperationAwareAction, FlowChainAwareAction {

    @Inject
    private StackService stackService;

    @Inject
    private OperationService operationService;

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
        Tunnel oldTunnel = null;
        LocalDateTime revertTime = null;
        Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin = null;
        if (payload instanceof UpgradeCcmFailureEvent) {
            UpgradeCcmFailureEvent ufe = (UpgradeCcmFailureEvent) payload;
            oldTunnel = ufe.getOldTunnel();
            revertTime = ufe.getRevertTime();
            failureOrigin = ufe.getFailureOrigin();
        } else if (payload instanceof UpgradeCcmEvent) {
            UpgradeCcmEvent ufe = (UpgradeCcmEvent) payload;
            oldTunnel = ufe.getOldTunnel();
        }
        return new UpgradeCcmFailureEvent(
                UPGRADE_CCM_FAILED_EVENT.event(),
                payload.getResourceId(),
                oldTunnel,
                failureOrigin,
                e,
                revertTime,
                e.getMessage(),
                ERROR
        );
    }

    protected void failOperation(String accountId, String failureMessage, Map<Object, Object> variables) {
        if (isOperationIdSet(variables)) {
            operationService.failOperation(accountId, getOperationId(variables), failureMessage);
        }
    }

    protected void completeOperation(String accountId, String environmentCrn, Map<Object, Object> variables) {
        if (isOperationIdSet(variables) && (!isChainedAction(variables) || isFinalChain(variables))) {
            SuccessDetails successDetails = new SuccessDetails(environmentCrn);
            operationService.completeOperation(accountId, getOperationId(variables), Set.of(successDetails), Set.of());
        }
    }
}
