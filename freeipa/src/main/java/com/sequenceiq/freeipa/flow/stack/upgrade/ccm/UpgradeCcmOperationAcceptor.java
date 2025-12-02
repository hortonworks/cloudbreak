package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class UpgradeCcmOperationAcceptor extends OperationAcceptor {

    @Override
    public AcceptResult accept(Operation operation) {
        Optional<AcceptResult> validationResult = validateOperation(operation);
        if (validationResult.isPresent()) {
            return validationResult.get();
        } else {
            boolean runningForSameStack = hasRunningOperationForSameStack(operation);
            if (runningForSameStack) {
                return AcceptResult.reject("There is already a running Cluster Connectivity Manager upgrade for FreeIPA stack");
            } else {
                return AcceptResult.accept();
            }
        }
    }

    private Optional<AcceptResult> validateOperation(Operation operation) {
        if (operation.getEnvironmentList() == null || operation.getEnvironmentList().size() != 1) {
            return Optional.of(AcceptResult.reject("Cluster Connectivity Manager upgrade must be invoked for a single environment!"));
        } else {
            return Optional.empty();
        }
    }

    private boolean hasRunningOperationForSameStack(Operation operation) {
        List<Operation> runningOperations = getOperationRepository().findRunningByAccountIdAndType(operation.getAccountId(), selector());
        return runningOperations.stream()
                .filter(op -> !op.getId().equals(operation.getId()))
                .anyMatch(op -> op.getEnvironmentList().contains(operation.getEnvironmentList().get(0)));
    }

    @Override
    protected OperationType selector() {
        return OperationType.UPGRADE_CCM;
    }

}
