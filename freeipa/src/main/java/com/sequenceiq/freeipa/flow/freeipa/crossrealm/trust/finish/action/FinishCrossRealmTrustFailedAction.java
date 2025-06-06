package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action;

import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.FINISH_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.converter.CrossRealmTrustAddTrustFailedToFinishCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FinishCrossRealmTrustFailedAction")
public class FinishCrossRealmTrustFailedAction extends AbstractFinishCrossRealmTrustAction<FinishCrossRealmTrustFailureEvent> {

    @Inject
    private OperationService operationService;

    public FinishCrossRealmTrustFailedAction() {
        super(FinishCrossRealmTrustFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaFinishCrossRealmTrustState,
            FreeIpaFinishCrossRealmTrustFlowEvent> stateContext, FinishCrossRealmTrustFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<FinishCrossRealmTrustFailureEvent>> payloadConverters) {
        payloadConverters.add(new CrossRealmTrustAddTrustFailedToFinishCrossRealmTrustFailureEvent());
    }

    @Override
    protected void doExecute(StackContext context, FinishCrossRealmTrustFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to finish cross-realm trust FreeIPA: " + getErrorReason(payload.getException());
        stackUpdater().updateStackStatus(stack, DetailedStackStatus.FINISH_CROSS_REALM_TRUST_FAILED, statusReason);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason);
        sendEvent(context, new StackEvent(FINISH_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT.event(),
                payload.getResourceId()));
    }
}
