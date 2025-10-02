package com.sequenceiq.freeipa.flow.freeipa.trust.base;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public abstract class FreeIpaBaseTrustAction<S extends FlowState, E extends FlowEvent, P extends Payload>
        extends AbstractStackAction<S, E, StackContext, P>
        implements OperationAwareAction {
    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private OperationService operationService;

    @Inject
    private TaskResultConverter taskResultConverter;

    protected FreeIpaBaseTrustAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        return new StackContext(
                flowParameters,
                stack,
                buildContext(stack),
                credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn())),
                cloudStackConverter.convert(stack)
        );
    }

    protected void updateStatuses(Stack stack, DetailedStackStatus detailedStackStatus, String statusReason, TrustStatus trustStatus) {
        stackUpdater.updateStackStatus(stack, detailedStackStatus, statusReason);
        crossRealmTrustService.updateTrustStateByStackId(stack.getId(), trustStatus);
    }

    protected void updateOperation(Stack stack, String operationId, TaskResults taskResults) {
        operationService.updateOperation(
                stack.getAccountId(),
                operationId,
                taskResultConverter.convertSuccessfulTasks(taskResults.getSuccessfulTasks(), stack.getEnvironmentCrn()),
                taskResultConverter.convertFailedTasks(taskResults.getErrors(), stack.getEnvironmentCrn())
        );
    }

    protected void updateOperation(Stack stack, String operationId, List<String> successfulTasks) {
        operationService.updateOperation(
                stack.getAccountId(),
                operationId,
                getSuccessfulTasks(stack, successfulTasks),
                List.of()
        );
    }

    private List<SuccessDetails> getSuccessfulTasks(Stack stack, List<String> successfulTasks) {
        return successfulTasks.stream()
                .map(task -> taskResultConverter.convertSuccessfulTaskResult(task, stack.getEnvironmentCrn()))
                .toList();
    }

    protected void setOperationId(Stack stack, Map<Object, Object> variables, String operationId) {
        setOperationId(variables, operationId);
        crossRealmTrustService.updateOperationIdByStackId(stack.getId(), operationId);
    }
}
