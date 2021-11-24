package com.sequenceiq.datalake.flow.datalake.recovery;

import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryCouldNotStartEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryFailedEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryStartEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoverySuccessEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxRecoveryService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeUpgradeRecoveryActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpgradeRecoveryActions.class);

    @Inject
    private SdxRecoveryService sdxRecoveryService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "DATALAKE_RECOVERY_START_STATE")
    public Action<?, ?> datalakeRecoveryStart() {
        return new AbstractSdxAction<>(DatalakeRecoveryStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRecoveryStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRecoveryStartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake recovery has been started for {}", payload.getResourceId());
                sdxRecoveryService.recoverCluster(payload.getResourceId());
                sendEvent(context, DATALAKE_RECOVERY_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRecoveryStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRecoveryCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RECOVERY_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeRecoveryInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake recovery is in progress for {}", payload.getResourceId());
                sendEvent(context, DatalakeRecoveryWaitRequest.from(context));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRecoveryFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RECOVERY_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(DatalakeRecoverySuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRecoverySuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRecoverySuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx recovery was finalized with sdx id: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING,
                        ResourceEvent.SDX_RECOVERY_FINISHED,
                        "Recovery finished",
                        payload.getResourceId());
                sendEvent(context, DATALAKE_RECOVERY_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRecoverySuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.warn("Unexpected error during sdx recovery finalisation", ex);
                return null;
            }
        };
    }

    @Bean(name = "DATALAKE_RECOVERY_COULD_NOT_START_EVENT")
    public Action<?, ?> recoveryCouldNotStart() {
        return new AbstractSdxAction<>(DatalakeRecoveryCouldNotStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRecoveryCouldNotStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRecoveryCouldNotStartEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake recovery could not be started for datalake with id: {}", payload.getResourceId(), exception);
                sendEvent(context, DATALAKE_RECOVERY_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRecoveryCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.warn("Unexpected error during handling sdx recovery not being able to start", ex);
                return null;
            }
        };
    }

    @Bean(name = "DATALAKE_RECOVERY_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(DatalakeRecoveryFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRecoveryFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRecoveryFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx recovery failed for sdxId: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.DATALAKE_RECOVERY_FAILED,
                        "Recovery failed",
                        payload.getResourceId());
                sendEvent(context, DATALAKE_RECOVERY_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRecoveryFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.warn("Unexpected error during handling sdx recovery failure", ex);
                return null;
            }
        };
    }
}
