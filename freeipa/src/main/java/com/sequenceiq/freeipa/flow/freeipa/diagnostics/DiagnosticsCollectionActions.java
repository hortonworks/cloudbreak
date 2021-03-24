package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;

@Configuration
public class DiagnosticsCollectionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsCollectionActions.class);

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
                DiagnosticsCollectionEvent event = DiagnosticsCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(payload.getResourceCrn())
                        .withSelector(DiagnosticsCollectionStateSelectors.HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector())
                        .withParameters(payload.getParameters())
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
