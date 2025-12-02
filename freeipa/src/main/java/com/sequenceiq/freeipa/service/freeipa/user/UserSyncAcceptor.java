package com.sequenceiq.freeipa.service.freeipa.user;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class UserSyncAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.USER_SYNC;
    }

    /**
     * Returns whether two user sync operations conflict. User sync conflicts are asymmetrical and
     * defined as follows:
     * 1) A full sync is not rejected due to a user-filtered sync, but only for other full syncs.
     * 2) User-filtered operations in general should be rejected if there is a full sync running for
     * that environment because it is redundant. We make a special exception to this rule when there's
     * a single user.
     * 3) When there's request to sync a single user we always let it through. This lets us apply the
     * changes to a user right away without needing to wait for a potentially long full sync to
     * finish first.
     *
     * @param operation this sync operation
     * @param other another running sync operation
     * @return true if the operations conflict
     */
    @Override
    protected boolean doOperationsConflict(Operation operation, Operation other) {
        if (operation.getUserList().size() == 1) {
            return false;
        } else if (!operation.getUserList().isEmpty()) {
            return super.doOperationsConflict(operation, other);
        } else {
            return other.getUserList().isEmpty() && doEnvironmentsConflict(operation, other);
        }
    }
}
