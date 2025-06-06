package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action;

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
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.converter.ConfigureDnsFailedToPrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.converter.CrossRealmTrustValidationFailedToPrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.converter.PrepareIpaServerFailedToPrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.converter.StackFailureToPrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.PrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("PrepareCrossRealmTrustFailedAction")
public class PrepareCrossRealmTrustFailedAction extends AbstractPrepareCrossRealmTrustAction<PrepareCrossRealmTrustFailureEvent> {

    @Inject
    private OperationService operationService;

    protected PrepareCrossRealmTrustFailedAction() {
        super(PrepareCrossRealmTrustFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaPrepareCrossRealmTrustState,
            FreeIpaPrepareCrossRealmTrustFlowEvent> stateContext, PrepareCrossRealmTrustFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, PrepareCrossRealmTrustFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to prepare cross-realm trust FreeIPA: " + getErrorReason(payload.getException());
        stackUpdater().updateStackStatus(stack, DetailedStackStatus.PREPARE_CROSS_REALM_TRUST_FAILED, statusReason);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason);
        sendEvent(context, new StackEvent(FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT.event(),
                payload.getResourceId()));
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<PrepareCrossRealmTrustFailureEvent>> payloadConverters) {
        payloadConverters.add(new CrossRealmTrustValidationFailedToPrepareCrossRealmTrustFailureEvent());
        payloadConverters.add(new PrepareIpaServerFailedToPrepareCrossRealmTrustFailureEvent());
        payloadConverters.add(new ConfigureDnsFailedToPrepareCrossRealmTrustFailureEvent());
        payloadConverters.add(new StackFailureToPrepareCrossRealmTrustFailureEvent());
    }
}
