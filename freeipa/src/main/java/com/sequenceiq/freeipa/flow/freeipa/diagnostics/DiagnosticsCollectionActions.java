package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.telemetry.diagnostics.DiagnosticsOperationsService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;

@Configuration
public class DiagnosticsCollectionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsCollectionActions.class);

    @Inject
    private DiagnosticsOperationsService diagnosticsOperationsService;

    @Bean(name = "DIAGNOSTICS_SALT_VALIDATION_STATE")
    public Action<?, ?> diagnosticsSaltValidateAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_SALT_VALIDATION_STATE. resourceCrn: '{}'", resourceCrn);
                InMemoryStateStore.putStack(payload.getResourceId(), PollGroup.POLLABLE);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.SALT_VALIDATION_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE")
    public Action<?, ?> diagnosticsSaltPillaarUpdateAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.SALT_PILLAR_UPDATE_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_SALT_STATE_UPDATE_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_PREFLIGHT_CHECK_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_INIT_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.INIT_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_UPGRADE_STATE")
    public Action<?, ?> diagnosticsUpgradeAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_UPGRADE_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.UPGRADE_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withSelector(DiagnosticsCollectionHandlerSelectors.VM_PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "DIAGNOSTICS_ENSURE_MACHINE_USER_STATE")
    public Action<?, ?> diagnosticsCreateMachineUserAction() {
        return new AbstractDiagnosticsCollectionActions<>(DiagnosticsCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiagnosticsCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into ENSURE_MACHINE_USER_EVENT. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.ENSURE_MACHINE_USER_EVENT.selector())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_COLLECTION_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.COLLECT_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_UPLOAD_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.UPLOAD_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_CLEANUP_STATE. resourceCrn: '{}'", resourceCrn);
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionHandlerSelectors.CLEANUP_DIAGNOSTICS_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_COLLECTION_FINISHED_STATE. resourceCrn: '{}'", resourceCrn);
                InMemoryStateStore.deleteStack(payload.getResourceId());
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionStateSelectors.FINALIZE_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
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
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into DIAGNOSTICS_COLLECTION_FAILED_STATE. resourceCrn: '{}'", resourceCrn);
                InMemoryStateStore.deleteStack(payload.getResourceId());
                DiagnosticParameters parameters = payload.getParameters();
                if (payload.getException() != null) {
                    parameters.setStatusReason(payload.getException().getMessage());
                }
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionStateSelectors.HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(parameters)
                        .build();
                diagnosticsOperationsService.vmDiagnosticsReport(resourceCrn, payload.getParameters(),
                        UsageProto.CDPVMDiagnosticsFailureType.Value.valueOf(payload.getFailureType()), payload.getException());
                sendEvent(context, event);
            }
        };
    }

}
