package com.sequenceiq.datalake.flow.atlas.updated;

import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_SUCCESS_EVENT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.atlas.updated.event.CheckAtlasUpdatedFailedEvent;
import com.sequenceiq.datalake.flow.atlas.updated.event.StartCheckAtlasUpdatedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.atlas.updated.CheckAtlasUpdatedService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class CheckAtlasUpdatedActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckAtlasUpdatedActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private CheckAtlasUpdatedService checkAtlasUpdatedService;

    @Bean(name = "CHECK_ATLAS_UPDATED_STATE")
    public Action<?, ?> checkAtlasUpdatedAction() {
        return new AbstractSdxAction<>(StartCheckAtlasUpdatedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    StartCheckAtlasUpdatedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StartCheckAtlasUpdatedEvent payload,
                    Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                LOGGER.info("Checking whether Atlas service of cluster {} is up to date.", sdxCluster.getClusterName());
                checkAtlasUpdatedService.verifyAtlasUpToDate(sdxCluster);
                eventSenderService.notifyEvent(sdxCluster, context, ResourceEvent.CHECK_ATLAS_UPDATED_FINISHED);
                sendEvent(context, CHECK_ATLAS_UPDATED_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StartCheckAtlasUpdatedEvent payload,
                    Optional<SdxContext> flowContext, Exception e) {
                LOGGER.error("Checking for atlas to be up to date failed!", e);
                return CheckAtlasUpdatedFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "CHECK_ATLAS_UPDATED_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(CheckAtlasUpdatedFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext, CheckAtlasUpdatedFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, CheckAtlasUpdatedFailedEvent payload,
                    Map<Object, Object> variables) {
                Exception exception = payload.getException();
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                String errorMessage = exception.getClass().getName();
                if (exception.getMessage() != null) {
                    errorMessage = exception.getMessage();
                }
                eventSenderService.sendEventAndNotification(
                        sdxCluster, sdxCluster.getInitiatorUserCrn(), ResourceEvent.CHECK_ATLAS_UPDATED_FAILED,
                        Set.of(errorMessage)
                );
                getFlow(context.getFlowParameters().getFlowId()).setFlowFailed(payload.getException());
                sendEvent(context, CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(CheckAtlasUpdatedFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Critical error occured while checking if Atlas is up to date.");
                return null;
            }
        };
    }
}
