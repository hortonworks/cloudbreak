package com.sequenceiq.datalake.flow.datalake.upgrade.preparation;

import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_EVENT;

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
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxUpgradePrepareService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeUpgradePreparationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpgradePreparationActions.class);

    @Bean(name = "DATALAKE_UPGRADE_PREPARATION_START_STATE")
    public Action<?, ?> prepareDatalakeUpgrade() {
        return new AbstractSdxAction<>(DatalakeUpgradePreparationStartEvent.class) {

            @Inject
            private SdxUpgradePrepareService upgradePrepareService;

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeUpgradePreparationStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpgradePreparationStartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake upgrade preparation has been started for {}", payload.getResourceId());
                upgradePrepareService.prepareUpgrade(payload.getResourceId(), payload.getImageId());
                sendEvent(context, new SdxEvent(DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_EVENT.event(), context));
            }

            @Override
            protected Object getFailurePayload(DatalakeUpgradePreparationStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Datalake upgrade preparation failed to start for event: {}", payload, ex);
                return DatalakeUpgradePreparationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeUpgradePreparationInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake upgrade preparation is in progress for {}", payload.getResourceId());
                sendEvent(context, DatalakeUpgradePreparationWaitRequest.from(context));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Datalake upgrade preparation failed to poll: {}", payload, ex);
                return DatalakeUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPGRADE_PREPARATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Inject
            private SdxStatusService sdxStatusService;

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx upgrade was finalized with sdx id: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_UPGRADE_PREPARATION_FINISHED,
                        "Upgrade preparation finished",
                        payload.getResourceId());
                sendEvent(context, DATALAKE_UPGRADE_PREPARATION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Datalake upgrade preparation finalization failed: {}", payload, ex);
                return DatalakeUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPGRADE_PREPARATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(DatalakeUpgradePreparationFailedEvent.class) {

            @Inject
            private SdxStatusService sdxStatusService;

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeUpgradePreparationFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpgradePreparationFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx upgrade preparation failed for sdxId: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.DATALAKE_UPGRADE_PREPARATION_FAILED,
                        "Upgrade preparation failed: " + payload.getException().getMessage(),
                        payload.getResourceId());
                sendEvent(context, DATALAKE_UPGRADE_PREPARATION_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpgradePreparationFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Upgrade preparation flow failure handling failed unexpectedly!", ex);
                return null;
            }
        };
    }
}
