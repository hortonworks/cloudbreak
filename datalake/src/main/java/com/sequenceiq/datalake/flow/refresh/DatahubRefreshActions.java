package com.sequenceiq.datalake.flow.refresh;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshFailedEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshStartEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshWaitEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.refresh.SdxRefreshService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatahubRefreshActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubRefreshActions.class);

    private static final String SDX = "SDX";

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRefreshService sdxRefreshService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "DATAHUB_REFRESH_START_STATE")
    public Action<?, ?> startDatahubRefreshAction() {
        return new AbstractSdxAction<>(DatahubRefreshStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatahubRefreshStartEvent payload) {

                //Only called as a part of a resize operation so we should update the cluster to reference the newly created one
                SdxCluster sdxCluster = sdxService.getByNameInAccount(payload.getUserId(), payload.getSdxName());
                LOGGER.info("Updating the Sdx-id in context from {} to {}", payload.getResourceId(), sdxCluster.getId());
                SdxContext sdxContext = SdxContext.from(flowParameters, payload);
                sdxContext.setSdxId(sdxCluster.getId());
                return sdxContext;
            }

            @Override
            protected void doExecute(SdxContext context, DatahubRefreshStartEvent payload, Map<Object, Object> variables) throws Exception {
                payload = new DatahubRefreshStartEvent(context.getSdxId(), payload.getSdxName(), payload.getUserId());
                LOGGER.info("Start Data Hub refresh associated with Sdx: {}", payload.getSdxName());
                SdxCluster sdxCluster = sdxService.getById(context.getSdxId());
                variables.put(SDX, sdxCluster);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.ENVIRONMENT_RESTART_DATAHUB_STARTED,
                        "Data Hub refresh in progress", payload.getResourceId());
                sdxRefreshService.refreshAllDatahubsServices(payload.getResourceId());
                sendEvent(context, DatahubRefreshFlowEvent.DATAHUB_REFRESH_IN_PROGRESS_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(DatahubRefreshStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatahubRefreshFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATAHUB_REFRESH_IN_PROGRESS_STATE")
    public Action<?, ?> datahubRefreshInProgressAction() {
        return new AbstractSdxAction<>(DatahubRefreshStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatahubRefreshStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatahubRefreshStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Data Hub refresh in progress for: {}", payload.getResourceId());
                sendEvent(context, new DatahubRefreshWaitEvent(payload.getResourceId(), payload.getUserId()));
            }

            @Override
            protected Object getFailurePayload(DatahubRefreshStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatahubRefreshFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATAHUB_REFRESH_FINISHED_STATE")
    public Action<?, ?> datahubRefreshFinishedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Data Hub refresh finished for: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING, ResourceEvent.ENVIRONMENT_RESTART_DATAHUB_FINISHED,
                        "Datahub refresh finished", payload.getResourceId()
                );
                sdxRefreshService.refreshAllDatahubsServices(payload.getResourceId());
                sendEvent(context, DatahubRefreshFlowEvent.DATAHUB_REFRESH_FINALIZED_EVENT.selector(), payload);

            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatahubRefreshFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATAHUB_REFRESH_FAILED_STATE")
    public Action<?, ?> handleFaileddatahubRefreshAction() {
        return new AbstractSdxAction<>(DatahubRefreshFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatahubRefreshFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatahubRefreshFailedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.error("Data Hub refresh failed for: {}", payload.getResourceId(), payload.getException());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.ENVIRONMENT_RESTART_DATAHUB_FAILED,
                        "Data Hub refresh failed", payload.getResourceId());
                sendEvent(context, DatahubRefreshFlowEvent.DATAHUB_REFRESH_FAILED_HANDLED_EVENT.selector(), payload);

            }

            @Override
            protected Object getFailurePayload(DatahubRefreshFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
