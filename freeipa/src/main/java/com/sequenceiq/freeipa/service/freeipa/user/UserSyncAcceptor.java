package com.sequenceiq.freeipa.service.freeipa.user;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.repository.SyncOperationRepository;

@Component
public class UserSyncAcceptor extends SyncOperationAcceptor {
    @Inject
    public UserSyncAcceptor(SyncOperationRepository syncOperationRepository) {
        super(syncOperationRepository);
    }

    @Override
    SyncOperationType selector() {
        return SyncOperationType.USER_SYNC;
    }

    /**
     * Returns whether two user sync operations conflict. User sync conflicts are asymmetrical. User-
     * filtered operations should be rejected if there is a full sync running for that environment because
     * it is redundant. Full sync is not rejected due to a user-filtered sync, but only for other full syncs.
     *
     * @param syncOperation this sync operation
     * @param other another running sync operation
     * @return true if the operations conflict
     */
    @Override
    protected boolean doOperationsConflict(SyncOperation syncOperation, SyncOperation other) {
        if (!syncOperation.getUserList().isEmpty()) {
            return super.doOperationsConflict(syncOperation, other);
        } else {
            return other.getUserList().isEmpty() && doEnvironmentsConflict(syncOperation, other);
        }
    }
}
