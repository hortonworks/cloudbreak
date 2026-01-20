package com.sequenceiq.datalake.flow.datahub;

import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_SUCCESS_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datahub.event.StartDatahubFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class StartDatahubActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartDatahubActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxStartService sdxStartService;

    @Bean(name = "START_DATAHUB_STATE")
    public Action<?, ?> startDatahubStartAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Execute start datahub for sdx: {}", payload.getResourceId());

                String userId = payload.getUserId();
                Long resourceId = payload.getResourceId();
                try {
                    sdxStartService.startAllDatahubs(resourceId);
                    sendEvent(context, new SdxEvent(START_DATAHUB_IN_PROGRESS_EVENT.event(), resourceId, userId));
                } catch (Exception e) {
                    LOGGER.error("Failed to start datahub, sdx: {}, user: {}", resourceId, userId, e);
                    sendEvent(context, new SdxEvent(START_DATAHUB_FAILED_EVENT.event(), resourceId, userId));
                }
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return StartDatahubFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "START_DATAHUB_FINISHED_STATE")
    public Action<?, ?> startDatahubFinishedEvent() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datahub start finalized: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Datahub is starting", payload.getResourceId());
                sendEvent(context, START_DATAHUB_SUCCESS_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return StartDatahubFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "START_DATAHUB_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(StartDatahubFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, StartDatahubFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StartDatahubFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DatalakeStatusEnum.START_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = "Datahub start failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, statusReason, payload.getResourceId());
                sendEvent(context, START_DATAHUB_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StartDatahubFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
