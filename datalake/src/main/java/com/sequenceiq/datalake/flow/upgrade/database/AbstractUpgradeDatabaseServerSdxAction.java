package com.sequenceiq.datalake.flow.upgrade.database;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

abstract class AbstractUpgradeDatabaseServerSdxAction<P extends SdxEvent> extends AbstractSdxAction<P> {

    protected AbstractUpgradeDatabaseServerSdxAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, P payload) {
        return SdxContext.from(flowParameters, payload);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<SdxContext> flowContext, Exception ex) {
        return new SdxUpgradeDatabaseServerFailedEvent(payload.getResourceId(), payload.getUserId(), ex,
                "Error happened during Data Lake database server upgrade");
    }

}