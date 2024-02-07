package com.sequenceiq.datalake.flow.imdupdate;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

abstract class AbstractInstanceMetadataUpdateSdxAction<P extends SdxEvent> extends AbstractSdxAction<P> {

    protected AbstractInstanceMetadataUpdateSdxAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, P payload) {
        return SdxContext.from(flowParameters, payload);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<SdxContext> flowContext, Exception ex) {
        return new SdxInstanceMetadataUpdateFailedEvent(payload.getResourceId(), payload.getUserId(), ex,
                "Error happened during Data Lake instance metadata update");
    }

}