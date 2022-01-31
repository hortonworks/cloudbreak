package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;

abstract class AbstractDiagnosticsCollectionActions<P extends ResourceCrnPayload>
        extends AbstractAction<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDiagnosticsCollectionActions.class);

    protected AbstractDiagnosticsCollectionActions(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
            StateContext<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors> stateContext, P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        return new DiagnosticsCollectionFailureEvent(payload.getResourceId(), ex, payload.getResourceCrn(), new DiagnosticParameters(),
                UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET.name());
    }

    @Override
    protected void prepareExecution(P payload, Map<Object, Object> variables) {
        if (payload != null) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
        } else {
            LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
        }
    }
}
