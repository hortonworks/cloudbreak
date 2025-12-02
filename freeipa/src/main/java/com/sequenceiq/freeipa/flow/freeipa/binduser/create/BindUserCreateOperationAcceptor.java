package com.sequenceiq.freeipa.flow.freeipa.binduser.create;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class BindUserCreateOperationAcceptor extends OperationAcceptor {

    @Override
    public AcceptResult accept(Operation operation) {
        Optional<AcceptResult> validationResult = validateOperation(operation);
        if (validationResult.isPresent()) {
            return validationResult.get();
        } else {
            boolean runningForSameCluster = isRunningOperationForSameCluster(operation);
            if (runningForSameCluster) {
                return AcceptResult.reject("There is already a running bind user creation for cluster");
            } else {
                return AcceptResult.accept();
            }
        }
    }

    private boolean isRunningOperationForSameCluster(Operation operation) {
        List<Operation> runningOperations = getOperationRepository().findRunningByAccountIdAndType(operation.getAccountId(), selector());
        return runningOperations.stream()
                .filter(op -> !op.getId().equals(operation.getId()))
                .filter(op -> op.getEnvironmentList().contains(operation.getEnvironmentList().get(0)))
                .flatMap(op -> op.getUserList().stream())
                .anyMatch(clusterName -> clusterName.equalsIgnoreCase(operation.getUserList().get(0)));
    }

    private Optional<AcceptResult> validateOperation(Operation operation) {
        if (operation.getEnvironmentList() == null || operation.getEnvironmentList().size() != 1) {
            return Optional.of(AcceptResult.reject("Bind user create must run only for one environment!"));
        } else if (operation.getUserList() == null || operation.getUserList().size() != 1) {
            return Optional.of(AcceptResult.reject("Bind user create must run only for one suffix!"));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected OperationType selector() {
        return OperationType.BIND_USER_CREATE;
    }
}
