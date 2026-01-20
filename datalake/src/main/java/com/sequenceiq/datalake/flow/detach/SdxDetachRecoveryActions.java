package com.sequenceiq.datalake.flow.detach;

import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_SUCCESS_EVENT;

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
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachRecoveryFailedEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachRecoveryEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.attach.SdxAttachService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxDetachRecoveryActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetachRecoveryActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxAttachService sdxAttachService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "SDX_DETACH_RECOVERY_STATE")
    public Action<?, ?> sdxDetachRecoveryAction() {
        return new AbstractSdxAction<>(SdxStartDetachRecoveryEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxStartDetachRecoveryEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartDetachRecoveryEvent payload,
                    Map<Object, Object> variables) throws Exception {
                SdxCluster clusterToReattach = sdxService.getById(payload.getResourceId());
                clusterToReattach = sdxAttachService.reattachDetachedSdxCluster(clusterToReattach);
                LOGGER.info("Successfully restored detached SDX with ID {}.", clusterToReattach.getId());
                sendEvent(context, SDX_DETACH_RECOVERY_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxStartDetachRecoveryEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Failed to recover from detach of SDX with ID {}.", payload.getResourceId());
                return SdxDetachRecoveryFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "SDX_DETACH_RECOVERY_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxDetachRecoveryFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachRecoveryFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachRecoveryFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Detach recovery of SDX cluster with ID {} and name {} failed with error {}.",
                        payload.getResourceId(), payload.getSdxName(), exception.getMessage(), exception);
                String statusReason = Optional.ofNullable(exception.getMessage()).orElse("SDX detach recovery failed");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, statusReason, payload.getResourceId());
                sendEvent(context, SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachRecoveryFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Critical error in SdxDetachRecovery. Failure was not handled correctly.", e);
                return null;
            }
        };
    }
}
