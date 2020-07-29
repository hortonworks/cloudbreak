package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class DiagnosticsCollectionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsCollectionActions.class);

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Bean(name = "DIAGNOSTICS_INIT_STATE")
    public Action<?, ?> diagnosticsInitAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_INIT_STATE. resourceCrn: '{}'", resourceCrn);
                InMemoryStateStore.putStack(resourceId, PollGroup.POLLABLE);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_DIAGNOSTICS_INIT_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.INIT_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_COLLECTION_STATE")
    public Action<?, ?> diagnosticsCollectionAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_COLLECTION_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_DIAGNOSTICS_COLLECTION_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.COLLECT_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_UPLOAD_STATE")
    public Action<?, ?> diagnosticsUploadAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_UPLOAD_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_DIAGNOSTICS_UPLOAD_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.UPLOAD_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_CLEANUP_STATE")
    public Action<?, ?> diagnosticsCleanupAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_CLEANUP_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_DIAGNOSTICS_CLEANUP_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.CLEANUP_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_COLLECTION_FINISHED_STATE")
    public Action<?, ?> diagnosticsCollectionFinishedAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_COLLECTION_FINISHED_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), ResourceEvent.STACK_DIAGNOSTICS_COLLECTION_FINISHED);
                InMemoryStateStore.deleteStack(resourceId);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionStateSelectors.FINALIZE_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_COLLECTION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionFailureEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionFailureEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_COLLECTION_FAILED_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(), ResourceEvent.STACK_DIAGNOSTICS_COLLECTION_FAILED);
                InMemoryStateStore.deleteStack(resourceId);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionStateSelectors.HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    private abstract class AbstractDiagnosticsCollectionActions<P extends ResourceCrnPayload>
            extends AbstractAction<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors, CommonContext, P> {

        protected AbstractDiagnosticsCollectionActions(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters,
                StateContext<DiagnosticsCollectionsState, DiagnosticsCollectionStateSelectors> stateContext, P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return payload;
        }

        @Override
        protected void prepareExecution(P payload, Map<Object, Object> variables) {
            if (payload != null) {
                MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
            } else {
                LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
            }
        }
    }
}
