package com.sequenceiq.datalake.flow.diagnostics;

import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT;

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
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsFailedEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsSuccessEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxDiagnosticsActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDiagnosticsActions.class);

    @Inject
    private SdxDiagnosticsFlowService diagnosticsFlowService;

    @Bean(name = "DIAGNOSTICS_COLLECTION_START_STATE")
    public Action<?, ?> startDiagnosticsCollection() {
        return new AbstractSdxAction<>(SdxDiagnosticsCollectionEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxDiagnosticsCollectionEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Start diagnostics collection for sdx cluster with id: {}", context.getSdxId());
                FlowIdentifier flowIdentifier = diagnosticsFlowService.startDiagnosticsCollection(payload.getProperties());
                SdxDiagnosticsCollectionEvent event = new SdxDiagnosticsCollectionEvent(payload.getResourceId(),
                        payload.getUserId(), payload.getProperties(), flowIdentifier);
                sendEvent(context, SDX_DIAGNOSTICS_COLLECTION_IN_PROGRESS_EVENT.event(), event);
            }

            @Override
            protected Object getFailurePayload(SdxDiagnosticsCollectionEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDiagnosticsFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean("DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE")
    public Action<?, ?> startDiagnosticsCollectionInProgress() {
        return new AbstractSdxAction<>(SdxDiagnosticsCollectionEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxDiagnosticsCollectionEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Diagnostics collection is in progress for sdx cluster");
                sendEvent(context, SdxDiagnosticsWaitRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(SdxDiagnosticsCollectionEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDiagnosticsFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean("DIAGNOSTICS_COLLECTION_FINISHED_STATE")
    public Action<?, ?> diagnosticsCollectionFinished() {
        return new AbstractSdxAction<>(SdxDiagnosticsSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxDiagnosticsSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDiagnosticsSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Diagnostics collection is finished for sdx cluster");
                sendEvent(context, SDX_DIAGNOSTICS_COLLECTION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDiagnosticsSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDiagnosticsFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean("DIAGNOSTICS_COLLECTION_FAILED_STATE")
    public Action<?, ?> diagnosticsCollectionFailed() {
        return new AbstractSdxAction<>(SdxDiagnosticsFailedEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxDiagnosticsFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDiagnosticsFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Diagnostics collection failed for sdx cluster");
                sendEvent(context, SDX_DIAGNOSTICS_COLLECTION_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDiagnosticsFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

}
