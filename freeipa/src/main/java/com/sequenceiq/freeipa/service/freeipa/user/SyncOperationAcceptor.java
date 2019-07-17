package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.repository.SyncOperationRepository;

public abstract class SyncOperationAcceptor {
    private static final String REJECTION_MESSAGE = "%s operation %s rejected due to conflicts: %s";

    private static final String CONFLICT_REASON = "operation %s running for users %s in environments %s";

    private final SyncOperationRepository syncOperationRepository;

    protected SyncOperationAcceptor(SyncOperationRepository syncOperationRepository) {
        this.syncOperationRepository = syncOperationRepository;
    }

    abstract SyncOperationType selector();

    public AcceptResult accept(SyncOperation syncOperation) {
        List<SyncOperation> runningOperations = syncOperationRepository
                .findRunningByAccountIdAndType(syncOperation.getAccountId(), selector());
        List<String> rejectionReasons = new ArrayList<>(runningOperations.size());

        for (SyncOperation o : runningOperations) {
            if (o.getId() < syncOperation.getId() && doOperationsConflict(syncOperation, o)) {
                rejectionReasons.add(String.format(CONFLICT_REASON, o.getOperationId(), o.getUserList(), o.getEnvironmentList()));
            }
        }

        if (rejectionReasons.isEmpty()) {
            return AcceptResult.accept();
        } else {
            return AcceptResult.reject(String.format(REJECTION_MESSAGE, selector(), syncOperation.getOperationId(), rejectionReasons));
        }
    }

    /**
     * Returns whether two sync operations conflict. Operations are considered to conflict if they overlap
     * in both environment and user.
     *
     * @param syncOperation the sync operation
     * @param other another sync operation
     * @return true if the sync operations conflict
     */
    protected boolean doOperationsConflict(SyncOperation syncOperation, SyncOperation other) {
        return doUsersConflict(syncOperation, other) && doEnvironmentsConflict(syncOperation, other);
    }

    protected boolean doEnvironmentsConflict(SyncOperation syncOperation, SyncOperation other) {
        return doListsConflict(syncOperation.getEnvironmentList(), other.getEnvironmentList());
    }

    protected boolean doUsersConflict(SyncOperation syncOperation, SyncOperation other) {
        return doListsConflict(syncOperation.getUserList(), other.getUserList());
    }

    protected boolean doListsConflict(List<String> list1, List<String> list2) {
        return list1.isEmpty() || list2.isEmpty() || !Sets.intersection(Set.copyOf(list1), Set.copyOf(list2)).isEmpty();
    }
}