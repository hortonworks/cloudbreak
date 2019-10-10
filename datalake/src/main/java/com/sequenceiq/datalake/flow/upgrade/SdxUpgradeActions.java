package com.sequenceiq.datalake.flow.upgrade;

import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.upgrade.event.SdxChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.upgrade.event.SdxImageChangedEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeStartEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxUpgradeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeActions.class);

    @Inject
    private SdxUpgradeService sdxUpgradeService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "SDX_UPGRADE_START_STATE")
    public Action<?, ?> datalakeUpgrade() {
        return new AbstractSdxAction<>(SdxUpgradeStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxUpgradeStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeStartEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                sdxUpgradeService.changeImage(payload.getResourceId(), payload.getUpgradeOption());
                sendEvent(context, SdxChangeImageWaitRequest.from(context, payload.getUpgradeOption()));
            }

            @Override
            protected Object getFailurePayload(SdxUpgradeStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_IMAGE_CHANGED_STATE")
    public Action<?, ?> imageChanged() {
        return new AbstractSdxAction<>(SdxImageChangedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxImageChangedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxImageChangedEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                sendEvent(context, SDX_UPGRADE_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxImageChangedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_UPGRADE_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeUpgradeInProgress() {
        return new AbstractSdxAction<>(SdxImageChangedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxImageChangedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxImageChangedEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                sdxUpgradeService.upgrade(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return SdxUpgradeWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxImageChangedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxUpgradeFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_UPGRADE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(SdxUpgradeSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxUpgradeSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                LOGGER.info("Sdx upgrade finalized sdxId: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING,
                        ResourceEvent.SDX_UPGRADE_FINISHED,
                        "Upgrade finished",
                        payload.getResourceId());
                sendEvent(context, SDX_UPGRADE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxUpgradeSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_UPGRADE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxUpgradeFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxUpgradeFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeFailedEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                LOGGER.info("Sdx upgrade failed for sdxId: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.UPGRADE_FAILED,
                        ResourceEvent.SDX_UPGRADE_FAILED,
                        "Upgrade failed",
                        payload.getResourceId());
                sendEvent(context, SDX_UPGRADE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxUpgradeFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
