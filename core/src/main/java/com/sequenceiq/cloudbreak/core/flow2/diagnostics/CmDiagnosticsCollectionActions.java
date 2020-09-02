package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionHandlerSelectors;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionStateSelectors;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class CmDiagnosticsCollectionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmDiagnosticsCollectionActions.class);

    private static final String LOCAL_LOG_PATH = "/var/lib/filecollector";

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Bean(name = "CM_DIAGNOSTICS_INIT_STATE")
    public Action<?, ?> diagnosticsInitAction() {
        return new CmDiagnosticsCollectionActions.AbstractCmDiagnosticsCollectionActions<>(CmDiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into CM_DIAGNOSTICS_INIT_STATE. resourceCrn: '{}'", resourceCrn);
                InMemoryStateStore.putStack(resourceId, PollGroup.POLLABLE);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_CM_DIAGNOSTICS_INIT_RUNNING);
                CmDiagnosticsCollectionEvent event = CmDiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(CmDiagnosticsCollectionHandlerSelectors.INIT_CM_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "CM_DIAGNOSTICS_COLLECTION_STATE")
    public Action<?, ?> diagnosticsCollectionAction() {
        return new CmDiagnosticsCollectionActions.AbstractCmDiagnosticsCollectionActions<>(CmDiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into CM_DIAGNOSTICS_COLLECTION_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_CM_DIAGNOSTICS_COLLECTION_RUNNING, List.of(payload.getParameters().getDestination().toString()));
                CmDiagnosticsCollectionEvent event = CmDiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(CmDiagnosticsCollectionHandlerSelectors.COLLECT_CM_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "CM_DIAGNOSTICS_UPLOAD_STATE")
    public Action<?, ?> diagnosticsUploadAction() {
        return new CmDiagnosticsCollectionActions.AbstractCmDiagnosticsCollectionActions<>(CmDiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into CM_DIAGNOSTICS_UPLOAD_STATE. resourceCrn: '{}'", resourceCrn);
                fireUploadEvent(resourceId, payload);
                CmDiagnosticsCollectionEvent event = CmDiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(CmDiagnosticsCollectionHandlerSelectors.UPLOAD_CM_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }

            private void fireUploadEvent(Long resourceId, CmDiagnosticsCollectionEvent payload) {
                CmDiagnosticsParameters parameters = payload.getParameters();
                String message;
                switch (parameters.getDestination()) {
                    case CLOUD_STORAGE:
                        String storageLocation = getStorageLocation(parameters);
                        message = "Upload location: " + storageLocation;
                        break;
                    case ENG:
                        message = "Engineering will receive the logs.";
                        break;
                    case SUPPORT:
                        message = String.format("Support ticket will be created for the logs. Ticket: '%s' Comments: '%s'",
                                parameters.getTicketNumber(), parameters.getComments());
                        break;
                    default:
                        message = "Location for logs on each node: " + LOCAL_LOG_PATH;
                        break;
                }
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_CM_DIAGNOSTICS_UPLOAD_RUNNING, List.of(message));
            }

            private String getStorageLocation(CmDiagnosticsParameters parameters) {
                String storageLocation;
                if (StringUtils.isNotBlank(parameters.getS3Location())) {
                    storageLocation = "s3://" + Paths.get(parameters.getS3Bucket(), parameters.getS3Location()).toString();
                } else {
                    storageLocation = "abfs://" + Paths.get(parameters.getAdlsv2StorageContainer(),
                            parameters.getAdlsv2StorageLocation()).toString();
                }
                return storageLocation;
            }
        };
    }

    @Bean(name = "CM_DIAGNOSTICS_CLEANUP_STATE")
    public Action<?, ?> diagnosticsCleanupAction() {
        return new CmDiagnosticsCollectionActions.AbstractCmDiagnosticsCollectionActions<>(CmDiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into CM_DIAGNOSTICS_CLEANUP_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_CM_DIAGNOSTICS_CLEANUP_RUNNING);
                CmDiagnosticsCollectionEvent event = CmDiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(CmDiagnosticsCollectionHandlerSelectors.CLEANUP_CM_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE")
    public Action<?, ?> diagnosticsCollectionFinishedAction() {
        return new CmDiagnosticsCollectionActions.AbstractCmDiagnosticsCollectionActions<>(CmDiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CmDiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), ResourceEvent.STACK_CM_DIAGNOSTICS_COLLECTION_FINISHED);
                InMemoryStateStore.deleteStack(resourceId);
                CmDiagnosticsCollectionEvent event = CmDiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(CmDiagnosticsCollectionStateSelectors.FINALIZE_CM_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "CM_DIAGNOSTICS_COLLECTION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new CmDiagnosticsCollectionActions.AbstractCmDiagnosticsCollectionActions<>(CmDiagnosticsCollectionFailureEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CmDiagnosticsCollectionFailureEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into CM_DIAGNOSTICS_COLLECTION_FAILED_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(),
                        ResourceEvent.STACK_CM_DIAGNOSTICS_COLLECTION_FAILED, List.of(payload.getException().getMessage()));
                InMemoryStateStore.deleteStack(resourceId);
                CmDiagnosticsCollectionEvent event = CmDiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(CmDiagnosticsCollectionStateSelectors.HANDLED_FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    private abstract class AbstractCmDiagnosticsCollectionActions<P extends ResourceCrnPayload>
            extends AbstractAction<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors, CommonContext, P> {

        protected AbstractCmDiagnosticsCollectionActions(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters,
                StateContext<CmDiagnosticsCollectionState, CmDiagnosticsCollectionStateSelectors> stateContext, P payload) {
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
