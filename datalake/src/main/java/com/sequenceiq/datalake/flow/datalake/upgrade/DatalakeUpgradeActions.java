package com.sequenceiq.datalake.flow.datalake.upgrade;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeImageChangeEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeCouldNotStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeSuccessEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeUpgradeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpgradeActions.class);

    private static final String TARGET_IMAGE = "TARGET_IMAGE";

    @Inject
    private SdxUpgradeService sdxUpgradeService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "DATALAKE_UPGRADE_START_STATE")
    public Action<?, ?> datalakeUpgrade() {
        return new AbstractSdxAction<>(DatalakeUpgradeStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeUpgradeStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void prepareExecution(DatalakeUpgradeStartEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(TARGET_IMAGE, payload.getImageId());
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpgradeStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datalake upgrade has been started for {}", payload.getResourceId());
                sdxUpgradeService.upgradeCluster(payload.getResourceId(), payload.getImageId());
                sendEvent(context, DATALAKE_UPGRADE_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpgradeStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeUpgradeCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPGRADE_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeUpgradeInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                String imageId = (String) variables.get(TARGET_IMAGE);
                LOGGER.info("Datalake upgrade is in progress for {} with image: {}", payload.getResourceId(), imageId);
                sendEvent(context, DatalakeUpgradeWaitRequest.from(context, imageId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_IMAGE_CHANGE_STATE")
    public Action<?, ?> imageChanged() {
        return new AbstractSdxAction<>(DatalakeImageChangeEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeImageChangeEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeImageChangeEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datalake upgrade image change is in progress for {} ", payload.getResourceId());
                String catalogName = sdxUpgradeService.getCurrentImageCatalogName(payload.getResourceId());
                UpgradeOptionV4Response upgrade = new UpgradeOptionV4Response().upgrade(
                        new ImageInfoV4Response().imageId(payload.getImageId()).imageCatalogName(catalogName)
                );
                sdxUpgradeService.changeImage(payload.getResourceId(), upgrade);
                sendEvent(context, DatalakeChangeImageWaitRequest.from(context, upgrade));
            }

            @Override
            protected Object getFailurePayload(DatalakeImageChangeEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPGRADE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(DatalakeUpgradeSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeUpgradeSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpgradeSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Sdx upgrade was finalized with sdx id: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_UPGRADE_FINISHED,
                        "Upgrade finished",
                        payload.getResourceId());
                sendEvent(context, DATALAKE_UPGRADE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpgradeSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_UPGRADE_COULD_NOT_START_STATE")
    public Action<?, ?> upgradeCouldNotStart() {
        return new AbstractSdxAction<>(DatalakeUpgradeCouldNotStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeUpgradeCouldNotStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpgradeCouldNotStartEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                LOGGER.error("Datalake upgrade could not be started for datalake with id: {}", payload.getResourceId(), exception);
                sendEvent(context, DATALAKE_UPGRADE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpgradeCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "DATALAKE_UPGRADE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(DatalakeUpgradeFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeUpgradeFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpgradeFailedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Sdx upgrade failed for sdxId: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.DATALAKE_UPGRADE_FAILED,
                        "Upgrade failed",
                        payload.getResourceId());
                sendEvent(context, DATALAKE_UPGRADE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpgradeFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
