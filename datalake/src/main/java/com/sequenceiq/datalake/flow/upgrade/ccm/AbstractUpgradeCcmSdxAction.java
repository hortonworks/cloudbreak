package com.sequenceiq.datalake.flow.upgrade.ccm;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

abstract class AbstractUpgradeCcmSdxAction<P extends SdxEvent> extends AbstractSdxAction<P> {

    protected AbstractUpgradeCcmSdxAction(Class<P> payloadClass) {
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
