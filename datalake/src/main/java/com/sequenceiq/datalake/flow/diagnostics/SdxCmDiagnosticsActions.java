package com.sequenceiq.datalake.flow.diagnostics;

import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsFailedEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsSuccessEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxCmDiagnosticsActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCmDiagnosticsActions.class);

    @Inject
    private SdxDiagnosticsFlowService diagnosticsFlowService;

    @Bean(name = "CM_DIAGNOSTICS_COLLECTION_START_STATE")
    public Action<?, ?> startCmDiagnosticsCollection() {
        return new AbstractSdxAction<>(SdxCmDiagnosticsCollectionEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxCmDiagnosticsCollectionEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Start CM based diagnostics collection for sdx cluster with id: {}", context.getSdxId());
                FlowIdentifier flowIdentifier = diagnosticsFlowService.startCmDiagnosticsCollection(payload.getProperties());
                SdxCmDiagnosticsCollectionEvent event = new SdxCmDiagnosticsCollectionEvent(payload.getResourceId(),
                        payload.getUserId(), payload.getProperties(), flowIdentifier);
                sendEvent(context, SDX_CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT.event(), event);
            }

            @Override
            protected Object getFailurePayload(SdxCmDiagnosticsCollectionEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCmDiagnosticsFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean("CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE")
    public Action<?, ?> startCmDiagnosticsCollectionInProgress() {
        return new AbstractSdxAction<>(SdxCmDiagnosticsCollectionEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxCmDiagnosticsCollectionEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Diagnostics collection is in progress for sdx cluster");
                sendEvent(context, SdxCmDiagnosticsWaitRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(SdxCmDiagnosticsCollectionEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCmDiagnosticsFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean("CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE")
    public Action<?, ?> cmDiagnosticsCollectionFinished() {
        return new AbstractSdxAction<>(SdxCmDiagnosticsSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxCmDiagnosticsSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCmDiagnosticsSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("CM based diagnostics collection is finished for sdx cluster");
                sendEvent(context, SDX_CM_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCmDiagnosticsSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCmDiagnosticsFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean("CM_DIAGNOSTICS_COLLECTION_FAILED_STATE")
    public Action<?, ?> cmDagnosticsCollectionFailed() {
        return new AbstractSdxAction<>(SdxCmDiagnosticsFailedEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxCmDiagnosticsFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCmDiagnosticsFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("CM based diagnostics collection failed for sdx cluster");
                sendEvent(context, SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCmDiagnosticsFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
