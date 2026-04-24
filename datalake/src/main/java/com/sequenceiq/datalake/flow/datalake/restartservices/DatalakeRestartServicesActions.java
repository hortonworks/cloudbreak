package com.sequenceiq.datalake.flow.datalake.restartservices;

import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_IN_PROGRESS_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesFailedEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesStartEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesWaitEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeRestartServicesActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRestartServicesActions.class);

    private static final Long WORKSPACE_ID = 0L;

    @Inject
    private SdxService sdxService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Bean(name = "DATALAKE_RESTART_SERVICES_START_STATE")
    public Action<?, ?> datalakeRestartServicesStartAction() {
        return new AbstractSdxAction<>(DatalakeRestartServicesStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRestartServicesStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRestartServicesStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Restart Data Lake services: {}", payload);
                SdxCluster sdxCluster = sdxService.getById(context.getSdxId());
                FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(initiatorUserCrn -> stackV4Endpoint.restartClusterServices(
                        WORKSPACE_ID, sdxCluster.getCrn(), payload.isRollingRestart(), payload.isStaleServicesOnly(), initiatorUserCrn));
                cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
                sendEvent(context, new SdxEvent(DATALAKE_RESTART_SERVICES_IN_PROGRESS_EVENT.event(), payload.getResourceId(), payload.getUserId()));
            }

            @Override
            protected Object getFailurePayload(DatalakeRestartServicesStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeRestartServicesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTART_SERVICES_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeRestartServicesInProgressAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Data Lake restart services in progress for: {}", payload.getResourceId());
                sendEvent(context, new DatalakeRestartServicesWaitEvent(payload.getResourceId(), payload.getUserId()));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeRestartServicesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTART_SERVICES_FINISHED_STATE")
    public Action<?, ?> datalakeRestartServicesFinishedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Data Lake restart services finished for: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING, ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_RESTART_SUCCESS,
                        "Data Lake restart services finished", payload.getResourceId()
                );
                sendEvent(context, DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FINALIZED_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeRestartServicesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTART_SERVICES_FAILED_STATE")
    public Action<?, ?> handleFailedDatalakeRestartServicesAction() {
        return new AbstractSdxAction<>(DatalakeRestartServicesFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRestartServicesFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRestartServicesFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(exception);
                LOGGER.error("Data Lake restart services failed for: {} with error: {}", payload.getResourceId(), errorMessage, exception);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_RESTART_FAILED,
                        Collections.singleton(errorMessage),
                        "Data Lake restart services failed",
                        payload.getResourceId());
                sendEvent(context, DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FAILED_HANDLED_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRestartServicesFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
