package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.FETCH_STACK_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_STACK_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FLOW_FINISHED_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

@Configuration
public class MaintenanceModeValidationActions {

    private static final String STACK_REPO = "STACK_REPO";

    private static final String WARNING_LIST = "WARNINGS";

    @Inject
    private MaintenanceModeValidationService maintenanceModeValidationService;

    @Bean(name = "FETCH_STACK_REPO_STATE")
    public AbstractMaintenanceModeValidationAction<?> fetchStackRepo() {
        return new AbstractMaintenanceModeValidationAction<>(MaintenanceModeValidationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, MaintenanceModeValidationTriggerEvent payload, Map<Object, Object> variables) {
                maintenanceModeValidationService.setUpValidationFlow(context.getStack().getId());
                String stackRepo = maintenanceModeValidationService.fetchStackRepository(context.getStack().getId());
                putWarnings(variables, new ArrayList<>());
                variables.put(STACK_REPO, stackRepo);
                sendEvent(context.getFlowId(), new StackEvent(FETCH_STACK_REPO_INFO_FINISHED_EVENT.event(),
                        context.getStack().getId()));
            }
        };
    }

    @Bean(name = "VALIDATE_STACK_REPO_INFO_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateStackRepo() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                List<Warning> warnings = getWarnings(variables);
                String stackRepo = (String) variables.get(STACK_REPO);
                warnings.addAll(maintenanceModeValidationService.validateStackRepository(
                        context.getStack().getCluster().getId(), stackRepo));
                sendEvent(context.getFlowId(), new StackEvent(VALIDATE_STACK_REPO_INFO_FINISHED_EVENT.event(),
                        context.getStack().getId()));
                putWarnings(variables, warnings);
            }
        };
    }

    @Bean(name = "VALIDATE_AMBARI_REPO_INFO_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateAmbariRepo() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                List<Warning> warnings = getWarnings(variables);
                warnings.addAll(maintenanceModeValidationService.validateAmbariRepository(context.getStack().getCluster().getId()));
                sendEvent(context.getFlowId(), new StackEvent(VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT.event(),
                        context.getStack().getId()));
                putWarnings(variables, warnings);
            }
        };
    }

    @Bean(name = "VALIDATE_IMAGE_COMPATIBILITY_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateImageCompatibility() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                List<Warning> warnings = getWarnings(variables);
                warnings.addAll(maintenanceModeValidationService.validateImageCatalog(context.getStack()));
                sendEvent(context.getFlowId(), new StackEvent(VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT.event(),
                        context.getStack().getId()));
                putWarnings(variables, warnings);
            }
        };
    }

    @Bean(name = "VALIDATION_FINISHED_STATE")
    public AbstractMaintenanceModeValidationAction<?> finishedAction() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                List<Warning> warnings = getWarnings(variables);
                maintenanceModeValidationService.handleValidationSuccess(context.getStack().getId(), warnings);
                sendEvent(context.getFlowId(), new StackEvent(VALIDATION_FLOW_FINISHED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "VALIDATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractStackFailureAction<MaintenanceModeValidationState, MaintenanceModeValidationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                maintenanceModeValidationService.handleValidationFailure(context.getStackView().getId(),
                        payload.getException());
                sendEvent(context.getFlowId(), VALIDATION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private List<Warning> getWarnings(Map<Object, Object> variables) {
        return (List<Warning>) variables.get(WARNING_LIST);
    }

    private void putWarnings(Map<Object, Object> variables, List<Warning> warnings) {
        variables.put(WARNING_LIST, warnings);
    }

}
