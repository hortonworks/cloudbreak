package com.sequenceiq.datalake.flow.datalake.scale;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_HORIZONTAL_SCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_START_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleHandlerEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_HANDLER;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleHandlerEvent.DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS_HANDLER;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent.DatalakeHorizontalScaleFlowEventBuilder;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleSdxEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxHorizontalScalingService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DatalakeHorizontalScaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeHorizontalScaleActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private SdxHorizontalScalingService sdxHorizontalScalingService;

    @Bean(name = "DATALAKE_HORIZONTAL_SCALE_VALIDATION_STATE")
    public Action<?, ?> datalakeHorizontalScaleValidationStart() {
        return new AbstractSdxAction<>(DatalakeHorizontalScaleSdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeHorizontalScaleSdxEvent payload, Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_VALIDATION_IN_PROGRESS,
                        "Validation In Progress", sdxCluster);
                eventSenderService.sendEventAndNotification(sdxCluster, ResourceEvent.DATALAKE_HORIZONTAL_SCALE_VALIDATION_IN_PROGRESS, List.of());
                LOGGER.info("Datalake horizontal scale started!");
                try {
                    sdxHorizontalScalingService.validateHorizontalScaleRequest(sdxCluster, payload.getScaleRequest());
                    sendEvent(context, DATALAKE_HORIZONTAL_SCALE_START_EVENT.selector(), payload);
                } catch (Exception e) {
                    LOGGER.warn("Datalake horizontal scale validation failed. Datalake {}. Error message: {}", payload.getResourceId(), e.getMessage());
                    DatalakeHorizontalScaleFlowEvent failedPayload = new DatalakeHorizontalScaleFlowEvent(DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector(),
                            payload.getResourceId(), payload.getSdxName(), payload.getResourceCrn(), payload.getUserId(),
                            payload.getScaleRequest(), payload.getCommandId(), e);
                    sendEvent(context, DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector(), failedPayload);
                }
            }

            @Override
            protected Object getFailurePayload(DatalakeHorizontalScaleSdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                String message = ex.getMessage();
                eventSenderService.sendEventAndNotification(sdxCluster, ResourceEvent.DATALAKE_HORIZONTAL_SCALE_VALIDATION_FAILED,
                        List.of(message));
                return new DatalakeHorizontalScaleSdxEvent(DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector(), payload.getResourceId(), payload.getSdxName(),
                        payload.getUserId(), payload.getResourceCrn(), payload.getScaleRequest(), ex, payload.getCommandId());
            }
        };
    }

    @Bean(name = "DATALAKE_HORIZONTAL_SCALE_START_STATE")
    public Action<?, ?> datalakeHorizontalScaleStart() {
        return new AbstractSdxAction<>(DatalakeHorizontalScaleSdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeHorizontalScaleSdxEvent payload, Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS,
                        "Horizontal Scale In Progress", sdxCluster);
                eventSenderService.sendEventAndNotification(sdxCluster, DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS);
                StackScaleV4Request scaleRequest = new StackScaleV4Request();
                scaleRequest.setGroup(payload.getScaleRequest().getGroup());
                scaleRequest.setDesiredCount(payload.getScaleRequest().getDesiredCount());
                scaleRequest.setStackId(sdxCluster.getStackId());
                LOGGER.info("Horizontal scale starts in group of {} with desired count {}", scaleRequest.getGroup(), scaleRequest.getDesiredCount());
                String flowId = sdxHorizontalScalingService.triggerScalingFlow(sdxCluster, scaleRequest);
                LOGGER.info("Put scaling flow triggered for datalake horizontal scale. FlowId: {}", flowId);
                sendEvent(context, DATALAKE_HORIZONTAL_SCALE_WAIT_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeHorizontalScaleSdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.warn("Datalake horizontal scale failed to create new instance.", ex);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_FAILED,
                        "Failed to create new instance",
                        payload.getResourceId());
                return payload;
            }
        };
    }

    @Bean(name = "DATALAKE_WAIT_FOR_HORIZONTAL_SCALE_STATE")
    public Action<?, ?> datalakeWaitForHorizontalScale() {
        return new AbstractSdxAction<>(DatalakeHorizontalScaleSdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeHorizontalScaleSdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Wait for Datalake Horizontal scale.");
                DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = new DatalakeHorizontalScaleFlowEventBuilder()
                        .setResourceId(payload.getResourceId())
                        .setResourceName(payload.getSdxName())
                        .setResourceCrn(payload.getResourceCrn())
                        .setUserId(payload.getUserId())
                        .setScaleRequest(payload.getScaleRequest())
                        .setSelector(DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS_HANDLER.selector());
                sendEvent(context, DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS_HANDLER.selector(), resultEventBuilder.build());
            }

            @Override
            protected Object getFailurePayload(DatalakeHorizontalScaleSdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.warn("Failed to wait for Datalake horizontal scale.", ex);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_FAILED,
                        "Failed to wait for horizontal scale",
                        payload.getResourceId());
                return payload;
            }
        };
    }

    @Bean(name = "DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_STATE")
    public Action<?, ?> rollingRestartServices() {
        return new AbstractDatalakeHorizontalScaleAction<>(DatalakeHorizontalScaleFlowEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeHorizontalScaleFlowEvent payload, Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_IN_PROGRESS,
                        "Rolling restart of services in progress",
                        sdxCluster);
                LOGGER.info("Datalake Horizontal scale, RollingRestart of services started!");
                sdxHorizontalScalingService.rollingRestartServices(payload.getResourceId(), payload.getResourceCrn());
                sendEvent(context, DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeHorizontalScaleFlowEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_FAILED,
                        "CM rolling restart failed",
                        payload.getResourceId());
                LOGGER.warn("Datalake Horizontal scale CM rolling restart Failed with error {}", ex.getMessage());
                return payload;
            }
        };
    }

    @Bean(name = "DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_IN_PROGRESS_STATE")
    public Action<?, ?> rollingRestartServicesInProgress() {
        return new AbstractDatalakeHorizontalScaleAction<>(DatalakeHorizontalScaleFlowEvent.class) {

            @Override
            protected void doExecute(CommonContext context, DatalakeHorizontalScaleFlowEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Rolling restart of services is in progress for {}", payload.getResourceId());
                String selector = DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_HANDLER.selector();
                DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = DatalakeHorizontalScaleFlowEvent
                        .datalakeHorizontalScaleFlowEventBuilderFactory(payload)
                        .setSelector(selector);
                sendEvent(context, selector, resultEventBuilder.build());
            }

            @Override
            protected Object getFailurePayload(DatalakeHorizontalScaleFlowEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_FAILED,
                        "CM rolling restart failed",
                        payload.getResourceId());
                LOGGER.warn("Datalake Horizontal scale CM rolling restart failed with error {}", ex.getMessage());
                return payload;
            }
        };
    }

    @Bean(name = "DATALAKE_HORIZONTAL_SCALE_FINISHED_STATE")
    public Action<?, ?> datalakeHorizontalScaleFinish() {
        return new AbstractDatalakeHorizontalScaleAction<>(DatalakeHorizontalScaleFlowEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeHorizontalScaleFlowEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_FINISHED,
                        "Data lake horizontal scale finished",
                        payload.getResourceId());
                LOGGER.info("Datalake horizontal scale finished!");
                DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = DatalakeHorizontalScaleFlowEvent
                        .datalakeHorizontalScaleFlowEventBuilderFactory(payload)
                        .setSelector(DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT.selector());
                sendEvent(context, DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT.selector(), resultEventBuilder.build());
            }
        };
    }

    @Bean(name = "DATALAKE_HORIZONTAL_SCALE_FAILED_STATE")
    public Action<?, ?> datalakeHorizontalScaleFailed() {
        return new AbstractDatalakeHorizontalScaleAction<>(DatalakeHorizontalScaleFlowEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeHorizontalScaleFlowEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Datalake horizontal scale failed!");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_HORIZONTAL_SCALE_FAILED,
                        "Datalake Horizontal Scaling failed.", payload.getResourceId());
                if (null != payload.getException()) {
                    SdxCluster sdxCluster = sdxService.getByCrn(payload.getResourceCrn());
                    eventSenderService.sendEventAndNotification(sdxCluster, DATALAKE_HORIZONTAL_SCALE_FAILED, List.of(payload.getException().getMessage()));
                }
                String selector = DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector();
                DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = DatalakeHorizontalScaleFlowEvent
                        .datalakeHorizontalScaleFlowEventBuilderFactory(payload)
                        .setSelector(selector);
                sendEvent(context, selector, resultEventBuilder.build());
            }
        };
    }
}
