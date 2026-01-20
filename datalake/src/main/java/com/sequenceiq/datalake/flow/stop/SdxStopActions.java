package com.sequenceiq.datalake.flow.stop;

import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_ALL_DATAHUB_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_IN_PROGRESS_EVENT;

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
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.stop.event.RdsStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.RdsWaitingToStopRequest;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopAllDatahubRequest;
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Configuration
public class SdxStopActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxStopService stopService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Bean(name = "SDX_STOP_START_STATE")
    public Action<?, ?> sdxStop() {
        return new AbstractSdxAction<>(SdxStartStopEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxStartStopEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartStopEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Execute stop flow for SDX: {}", payload.getResourceId());
                stopService.stop(payload.getResourceId());
                if (payload.isStopDataHubs()) {
                    sendEvent(context, SDX_STOP_ALL_DATAHUB_EVENT.event(), payload);
                } else {
                    sendEvent(context, SDX_STOP_IN_PROGRESS_EVENT.event(), payload);
                }
            }

            @Override
            protected Object getFailurePayload(SdxStartStopEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStopFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STOP_ALL_DATAHUBS_STATE")
    public Action<?, ?> sdxStackStopAllDatahub() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Stopping all datahub clusters for SDX in progress: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return SdxStopAllDatahubRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStopFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STOP_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackStopInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX stop in progress: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return SdxStopWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStopFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STOP_RDS_STOP_STATE")
    public Action<?, ?> sdxRdsStop() {
        return new AbstractSdxAction<>(SdxStopSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxStopSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStopSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX RDS stop in progress: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new RdsWaitingToStopRequest(context.getSdxId(), context.getUserId());
            }

            @Override
            protected Object getFailurePayload(SdxStopSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxStopFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STOP_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(RdsStopSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    RdsStopSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, RdsStopSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX stop finalized: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, "Datalake is stopped", payload.getResourceId());
                sendEvent(context, SDX_STOP_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(RdsStopSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_STOP_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DatalakeStatusEnum.STOP_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = "SDX stop failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }

                // If this is part of DL resize, mark failure as such in order to enable proper recovery.
                if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeFlowEventChainFactory.class.getSimpleName(),
                        flowLogService.getLastFlowLog(context.getFlowParameters().getFlowId()))) {
                    statusReason = "Datalake resize failure: " + statusReason;
                }

                sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, statusReason, payload.getResourceId());
                sendEvent(context, SDX_STOP_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
