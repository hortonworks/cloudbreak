package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.action;

import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_STARTING_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.FAIL_HANDLED_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayContext;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayService;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.failure.ClusterProxyUpdateRegistrationFailedToChangePrimaryGatewayFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.failure.HealthCheckFailedToChangePrimaryGatewayFailureEventConverter;
import com.sequenceiq.freeipa.flow.stack.HealthCheckRequest;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class ChangePrimaryGatewayActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangePrimaryGatewayActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_STATE_STARTING")
    public Action<?, ?> startingAction() {
        return new AbstractChangePrimaryGatewayAction<>(ChangePrimaryGatewayEvent.class) {
            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, ChangePrimaryGatewayEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String operationId = payload.getOperationId();
                setOperationId(variables, operationId);
                setInstanceIds(variables, payload.getRepairInstanceIds());
                setFinalChain(variables, payload.getFinalChain());
                LOGGER.info("Starting to change the primary gateway {}", payload);
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS, "Starting to change the primary gateway");
                sendEvent(context, CHANGE_PRIMARY_GATEWAY_STARTING_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_SELECTION")
    public Action<?, ?> selectionAction() {
        return new AbstractChangePrimaryGatewayAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS, "Selecting the primary gateway");

                List<String> repairInstanceIds = getInstanceIds(variables);
                ChangePrimaryGatewaySelectionRequest request = new ChangePrimaryGatewaySelectionRequest(stack.getId(), repairInstanceIds);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_METADATA_STATE")
    public Action<?, ?> orchestrationAction() {
        return new AbstractChangePrimaryGatewayAction<>(ChangePrimaryGatewaySelectionSuccess.class) {
            @Inject
            private ChangePrimaryGatewayService changePrimaryGatewayService;

            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, ChangePrimaryGatewaySelectionSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS, "Changing the primary gateway metadata");

                try {
                    changePrimaryGatewayService.changePrimaryGatewayMetadata(stack, payload.getFormerPrimaryGatewayInstanceId(),
                            payload.getNewPrimaryGatewayInstanceId());
                    sendEvent(context, CHANGE_PRIMARY_GATEWAY_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    LOGGER.error("Failed to update the primary gateway metadata", e);
                    sendEvent(context, CHANGE_PRIMARY_GATEWAY_METADATA_FAILED_EVENT.selector(),
                            new ChangePrimaryGatewayFailureEvent(stack.getId(), "Updating metadata", Set.of(), Map.of(), e));

                }
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyRegistrationAction() {
        return new AbstractChangePrimaryGatewayAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS,
                        "Changing the primary gateway cluster proxy registration");

                List<String> repairInstanceIds = getInstanceIds(variables);
                ClusterProxyUpdateRegistrationRequest request;
                if (Objects.nonNull(repairInstanceIds)) {
                    List<String> instanceIdsToRegister = stack.getNotDeletedInstanceMetaDataSet().stream()
                            .map(InstanceMetaData::getInstanceId)
                            .filter(instanceId -> !repairInstanceIds.contains(instanceId))
                            .collect(Collectors.toList());
                    LOGGER.debug("When changing the primary gateway cluster proxy in stack {}, [{}] will be reregistered", stack.getId(),
                            instanceIdsToRegister);
                    request = new ClusterProxyUpdateRegistrationRequest(stack.getId(), instanceIdsToRegister);
                } else {
                    LOGGER.debug("When changing the primary gateway cluster proxy in stack {}, the whole stack will be reregistered", stack.getId());
                    request = new ClusterProxyUpdateRegistrationRequest(stack.getId());
                }
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_STATE")
    public Action<?, ?> healthCheckAction() {
        return new AbstractChangePrimaryGatewayAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();

                Selectable request;
                if (isFinalChain(variables)) {
                    stackUpdater.updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS, "Checking the health");
                    request = new HealthCheckRequest(stack.getId(), false);
                } else {
                    LOGGER.debug("Repair in progress, skipping the health check");
                    request = new HealthCheckSuccess(stack.getId(), null);
                }

                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_FINISHED_STATE")
    public Action<?, ?> finsihedAction() {
        return new AbstractChangePrimaryGatewayAction<>(StackEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());

                if (isFinalChain(variables)) {
                    successDetails.getAdditionalDetails().put("DownscaleHosts", getDownscaleHosts(variables));
                    successDetails.getAdditionalDetails().put("UpscaleHosts", getUpscaleHosts(variables));
                    operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(successDetails), Collections.emptyList());
                } else {
                    stackUpdater.updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS, "Finished changing the primary gateway");
                }

                sendEvent(context, CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_FAIL_STATE")
    public Action<?, ?> failureAction() {
        return new AbstractChangePrimaryGatewayAction<>(ChangePrimaryGatewayFailureEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected ChangePrimaryGatewayContext createFlowContext(FlowParameters flowParameters,
                    StateContext<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> stateContext, ChangePrimaryGatewayFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(ChangePrimaryGatewayContext context, ChangePrimaryGatewayFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Change primary gateway failed with payload: " + payload);
                Stack stack = context.getStack();
                String environmentCrn = stack.getEnvironmentCrn();
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Change primary gateway failed during " + payload.getFailedPhase();
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.REPAIR_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                LOGGER.info("Enabling the status checker for stack ID {} after failing repairing", stack.getId());
                enableStatusChecker(stack, "Failed to repair FreeIPA");
                sendEvent(context, FAIL_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<ChangePrimaryGatewayFailureEvent>> payloadConverters) {
                payloadConverters.add(new ClusterProxyUpdateRegistrationFailedToChangePrimaryGatewayFailureEventConverter());
                payloadConverters.add(new HealthCheckFailedToChangePrimaryGatewayFailureEventConverter());
            }
        };
    }
}