package com.sequenceiq.datalake.flow.repair;

import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FINALIZED_EVENT;

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
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairCouldNotStartEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairFailedEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartRequest;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairSuccessEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairWaitRequest;
import com.sequenceiq.datalake.job.SdxClusterJobAdapter;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxRepairActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private StatusCheckerJobService jobService;

    @Bean(name = "SDX_REPAIR_START_STATE")
    public Action<?, ?> sdxRepairStarted() {
        return new AbstractSdxAction<>(SdxRepairStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxRepairStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxRepairStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Start repair flow for Datalake: {}", payload.getResourceId());
                SdxRepairStartRequest request = new SdxRepairStartRequest(payload.getResourceId(), payload.getUserId(), payload.getRepairSettings());
                sendEvent(context, request.selector(), request);
            }

            @Override
            protected Object getFailurePayload(SdxRepairStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxRepairCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_REPAIR_IN_PROGRESS_STATE")
    public Action<?, ?> sdxRepairInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datalake repair in progress: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return SdxRepairWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxRepairFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_REPAIR_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(SdxRepairSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxRepairSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxRepairSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datalake repair finalized: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.SDX_REPAIR_FINISHED, "Repair finished, Datalake is running", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_REPAIR_FINISHED, sdxCluster);
                jobService.schedule(sdxCluster.getId(), SdxClusterJobAdapter.class);
                sendEvent(context, SDX_REPAIR_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxRepairSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_REPAIR_COULD_NOT_START_STATE")
    public Action<?, ?> repairCouldNotStart() {
        return new AbstractSdxAction<>(SdxRepairCouldNotStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxRepairCouldNotStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxRepairCouldNotStartEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                LOGGER.error("Datalake repair could not be started for datalake with id: {}", payload.getResourceId(), exception);
                sendEvent(context, SDX_REPAIR_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxRepairCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_REPAIR_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxRepairFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxRepairFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxRepairFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                LOGGER.error("Datalake repair failed for datalakeId: {}", payload.getResourceId(), exception);
                String statusReason = "Datalake stack repair failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REPAIR_FAILED, statusReason, payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_REPAIR_FAILED, sdxCluster);
                sendEvent(context, SDX_REPAIR_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxRepairFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
