package com.sequenceiq.freeipa.service.freeipa.user;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class UserAcceptor extends OperationAcceptor {
    @Inject
    public UserAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.USER_SYNC;
    }

    /**
     * Returns whether two user sync operations conflict. User sync conflicts are asymmetrical. User-
     * filtered operations should be rejected if there is a full sync running for that environment because
     * it is redundant. Full sync is not rejected due to a user-filtered sync, but only for other full syncs.
     *
     * @param operation this sync operation
     * @param other another running sync operation
     * @return true if the operations conflict
     */
    @Override
    protected boolean doOperationsConflict(Operation operation, Operation other) {
        if (!operation.getUserList().isEmpty()) {
            return super.doOperationsConflict(operation, other);
        } else {
            return other.getUserList().isEmpty() && doEnvironmentsConflict(operation, other);
        }
    }
}
