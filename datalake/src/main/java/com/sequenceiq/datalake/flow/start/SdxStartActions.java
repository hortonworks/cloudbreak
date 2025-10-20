package com.sequenceiq.datalake.flow.start;

import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.start.event.RdsStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.RdsWaitingToStartRequest;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartStartEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartWaitRequest;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxStartActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStartActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxStartService startService;

    @Inject
    private SdxMetricService metricService;

    @Bean(name = "SDX_START_RDS_START_STATE")
    public Action<?, ?> sdxRdsStart() {
        return new AbstractSdxAction<>(SdxStartStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxStartStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Execute rds start flow for SDX: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new RdsWaitingToStartRequest(context.getSdxId(), context.getUserId());
            }

            @Override
            protected Object getFailurePayload(SdxStartStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStartFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_START_START_STATE")
    public Action<?, ?> sdxStart() {
        return new AbstractSdxAction<>(RdsStartSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    RdsStartSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, RdsStartSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Execute start flow for SDX: {}", payload.getResourceId());
                startService.start(payload.getResourceId());
                sendEvent(context, SDX_START_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(RdsStartSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStartFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_START_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackStartInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX start in progress: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return SdxStartWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStartFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_START_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(SdxStartSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxStartSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX start finalized: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Datalake is running", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_START_FINISHED, sdxCluster);
                sendEvent(context, SDX_START_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxStartSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_START_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxStartFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxStartFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DatalakeStatusEnum.START_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = "SDX start failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, statusReason, payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_START_FAILED, sdxCluster);
                sendEvent(context, SDX_START_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxStartFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
