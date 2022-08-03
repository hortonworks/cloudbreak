package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
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
import com.sequenceiq.cloudbreak.telemetry.diagnostics.DiagnosticsOperationsService;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.CloudStorageDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.GcsDiagnosticsParameters;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class DiagnosticsCollectionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsCollectionActions.class);

    private static final String LOCAL_LOG_PATH = "/var/lib/filecollector";

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private DiagnosticsOperationsService diagnosticsOperationsService;

    @Bean(name = "DIAGNOSTICS_SALT_VALIDATION_STATE")
    public Action<?, ?> diagnosticsSaltValidationAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_SALT_VALIDATION_STATE. resourceCrn: '{}'", resourceCrn);
                InMemoryStateStore.putStack(resourceId, PollGroup.POLLABLE);
                String excludedHosts = CollectionUtils.isEmpty(payload.getParameters().getExcludeHosts())
                        ? "[NONE]" : String.format("[%s]", String.join(",", payload.getParameters().getExcludeHosts()));
                if (payload.getParameters().getSkipUnresponsiveHosts()) {
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                            ResourceEvent.STACK_DIAGNOSTICS_SALT_VALIDATION_RUNNING_SKIP_UNRESPONSIVE, List.of(excludedHosts));
                } else {
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                            ResourceEvent.STACK_DIAGNOSTICS_SALT_VALIDATION_RUNNING, List.of(excludedHosts));
                }
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.SALT_VALIDATION_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE")
    public Action<?, ?> diagnosticsSaltPillarUpdateAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_DIAGNOSTICS_SALT_PILLAR_UPDATE_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.SALT_PILLAR_UPDATE_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_SALT_STATE_UPDATE_STATE")
    public Action<?, ?> diagnosticsSaltStateUpdateAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into SALT_STATE_UPDATE_DIAGNOSTICS_EVENT. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), ResourceEvent.STACK_DIAGNOSTICS_SALT_STATE_UPDATE_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_PREFLIGHT_CHECK_STATE")
    public Action<?, ?> diagnosticsPreFlightCheckAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_PREFLIGHT_CHECK_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_INIT_STATE")
    public Action<?, ?> diagnosticsInitAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_INIT_STATE. resourceCrn: '{}'", resourceCrn);
                String hosts = CollectionUtils.isEmpty(payload.getHosts())
                        ? "[ALL]" : String.format("[%s]", String.join(",", payload.getHosts()));
                String excludedHosts = CollectionUtils.isEmpty(payload.getExcludeHosts())
                        ? "[NONE]" : String.format("[%s]", String.join(",", payload.getExcludeHosts()));
                String hostGroups = CollectionUtils.isEmpty(payload.getHostGroups())
                        ? "[ALL]" : String.format("[%s]", String.join(",", payload.getHostGroups()));
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_INIT_RUNNING, List.of(hosts, excludedHosts, hostGroups));
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.INIT_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_UPGRADE_STATE")
    public Action<?, ?> diagnosticsUpgradeTelemetryAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_UPGRADE_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_TELEMETRY_UPGRADE_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.UPGRADE_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE")
    public Action<?, ?> diagnosticsVmPreFlightCheckAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_VM_PREFLIGHT_CHECK_RUNNING);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.VM_PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_ENSURE_MACHINE_USER_STATE")
    public Action<?, ?> diagnosticsEnsureMachineUserAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_CREATE_MACHINE_USER_STATE. resourceCrn: '{}'", resourceCrn);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_ENSURE_MACHINE_USER);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.ENSURE_MACHINE_USER_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
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
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_COLLECTION_RUNNING, List.of(payload.getParameters().getDestination().toString()));
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.COLLECT_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
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
                fireUploadEvent(resourceId, payload);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.UPLOAD_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                sendEvent(context, event);
            }

            private void fireUploadEvent(Long resourceId, DiagnosticsCollectionEvent payload) {
                DiagnosticParameters parameters = payload.getParameters();
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
                        if (StringUtils.isNotBlank(parameters.getIssue())) {
                            message = String.format("Diagnostics have been sent to support. " +
                                            "Case number: '%s' Description: '%s'",
                                    parameters.getIssue(), parameters.getDescription());
                        } else {
                            message = String.format("Diagnostics have been sent to support. " +
                                            "A ticket will be created for the diagnostics. Description: '%s'",
                                    parameters.getDescription());
                        }
                        break;
                    default:
                        message = "Location for logs on each node: " + LOCAL_LOG_PATH;
                        break;
                }
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_UPLOAD_RUNNING, List.of(message));
            }

            private String getStorageLocation(DiagnosticParameters parameters) {
                String storageLocation;
                CloudStorageDiagnosticsParameters csDiagnosticsParams = parameters.getCloudStorageDiagnosticsParameters();
                if (csDiagnosticsParams instanceof AwsDiagnosticParameters) {
                    AwsDiagnosticParameters awsParameters = (AwsDiagnosticParameters) csDiagnosticsParams;
                    storageLocation = "s3://" + Paths.get(awsParameters.getS3Bucket(), awsParameters.getS3Location()).toString();
                } else if (csDiagnosticsParams instanceof AzureDiagnosticParameters) {
                    AzureDiagnosticParameters azureParameters = (AzureDiagnosticParameters) csDiagnosticsParams;
                    storageLocation = "abfs://" + Paths.get(azureParameters.getAdlsv2StorageContainer(),
                            azureParameters.getAdlsv2StorageLocation()).toString();
                } else {
                    GcsDiagnosticsParameters gcsParameters = (GcsDiagnosticsParameters) csDiagnosticsParams;
                    storageLocation = "gcs://" + Paths.get(gcsParameters.getBucket(), gcsParameters.getGcsLocation()).toString();
                }
                return storageLocation;
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
                        .withExcludeHosts(payload.getExcludeHosts())
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
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                diagnosticsOperationsService.vmDiagnosticsReport(resourceCrn, payload.getParameters());
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
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_COLLECTION_FAILED, List.of(payload.getException().getMessage()));
                InMemoryStateStore.deleteStack(resourceId);
                DiagnosticParameters parameters = payload.getParameters();
                if (payload.getException() != null) {
                    parameters.setStatusReason(payload.getException().getMessage());
                }
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(resourceId)
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionStateSelectors.HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .withHosts(payload.getHosts())
                        .withHostGroups(payload.getHostGroups())
                        .withExcludeHosts(payload.getExcludeHosts())
                        .build();
                diagnosticsOperationsService.vmDiagnosticsReport(resourceCrn, payload.getParameters(),
                        UsageProto.CDPVMDiagnosticsFailureType.Value.valueOf(payload.getFailureType()), payload.getException());
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
            return new DiagnosticsCollectionFailureEvent(payload.getResourceId(), ex, payload.getResourceCrn(), new DiagnosticParameters(),
                    UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET.name());
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
