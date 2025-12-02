package com.sequenceiq.freeipa.service.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;

public abstract class OperationAcceptor {
    private static final String REJECTION_MESSAGE = "%s operation %s rejected due to conflicts: %s";

    private static final String CONFLICT_REASON = "operation %s running for users %s in environments %s";

    @Inject
    private OperationRepository operationRepository;

    protected abstract OperationType selector();

    public AcceptResult accept(Operation operation) {
        List<Operation> runningOperations = getOperationRepository()
                .findRunningByAccountIdAndType(operation.getAccountId(), selector());
        List<String> rejectionReasons = new ArrayList<>(runningOperations.size());

        for (Operation o : runningOperations) {
            if (o.getId() < operation.getId() && doOperationsConflict(operation, o)) {
                rejectionReasons.add(String.format(CONFLICT_REASON, o.getOperationId(),
                        o.getUserList().isEmpty() ? "[all users]" : o.getUserList(),
                        o.getEnvironmentList().isEmpty() ? "[all environments]" : o.getEnvironmentList()));
            }
        }

        if (rejectionReasons.isEmpty()) {
            return AcceptResult.accept();
        } else {
            return AcceptResult.reject(String.format(REJECTION_MESSAGE, selector(), operation.getOperationId(), rejectionReasons));
        }
    }

    /**
     * Returns whether two sync operations conflict. Operations are considered to conflict if they overlap
     * in both environment and user.
     *
     * @param operation the sync operation
     * @param other another sync operation
     * @return true if the sync operations conflict
     */
    protected boolean doOperationsConflict(Operation operation, Operation other) {
        return doUsersConflict(operation, other) && doEnvironmentsConflict(operation, other);
    }

    protected boolean doEnvironmentsConflict(Operation operation, Operation other) {
        return doListsConflict(operation.getEnvironmentList(), other.getEnvironmentList());
    }

    protected boolean doUsersConflict(Operation operation, Operation other) {
        return doListsConflict(operation.getUserList(), other.getUserList());
    }

    protected boolean doListsConflict(List<String> list1, List<String> list2) {
        return list1.isEmpty() || list2.isEmpty() || !Sets.intersection(Set.copyOf(list1), Set.copyOf(list2)).isEmpty();
    }

    protected OperationRepository getOperationRepository() {
        return operationRepository;
    }
}