package com.sequenceiq.datalake.flow.repair;

import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_IN_PROGRESS_EVENT;

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
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairFailedEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairSuccessEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.notification.NotificationService;

@Configuration
public class SdxRepairActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRepairService repairService;

    @Inject
    private NotificationService notificationService;

    @Bean(name = "SDX_REPAIR_START_STATE")
    public Action<?, ?> sdxDeletion() {
        return new AbstractSdxAction<>(SdxRepairStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxRepairStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxRepairStartEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestIdToMdcContext(context.getRequestId());
                LOGGER.info("Start repair flow for SDX: {}", payload.getResourceId());
                repairService.startSdxRepair(payload.getResourceId(), payload.getRepairRequest());
                sendEvent(context, SDX_REPAIR_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxRepairStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxRepairFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_REPAIR_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackDeletionInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestIdToMdcContext(context.getRequestId());
                LOGGER.info("SDX repair in progress: {}", payload.getResourceId());
                notificationService.send(ResourceEvent.SDX_REPAIR_STARTED, context.getUserId());
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
                MDCBuilder.addRequestIdToMdcContext(context.getRequestId());
                LOGGER.info("SDX repair finalized: {}", payload.getResourceId());
                notificationService.send(ResourceEvent.SDX_REPAIR_FINISHED, payload, context.getUserId());
                sdxService.updateSdxStatus(payload.getResourceId(), SdxClusterStatus.RUNNING);
                sendEvent(context, SDX_REPAIR_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxRepairSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
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
                MDCBuilder.addRequestIdToMdcContext(context.getRequestId());
                Exception exception = payload.getException();
                SdxClusterStatus repairFailedStatus = SdxClusterStatus.REPAIR_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", repairFailedStatus, payload.getResourceId(), exception);
                String statusReason = "SDX repair failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxService.updateSdxStatus(payload.getResourceId(), repairFailedStatus, statusReason);
                notificationService.send(ResourceEvent.SDX_REPAIR_FAILED, payload, context.getUserId());
                sendEvent(context, SDX_REPAIR_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxRepairFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
