package com.sequenceiq.datalake.flow.certrotation;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

abstract class AbstractRotateCertificateSdxAction<P extends SdxEvent> extends AbstractSdxAction<P> {

    protected AbstractRotateCertificateSdxAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, P payload) {
        return SdxContext.from(flowParameters, payload);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<SdxContext> flowContext, Exception ex) {
        return payload;
    }

}
