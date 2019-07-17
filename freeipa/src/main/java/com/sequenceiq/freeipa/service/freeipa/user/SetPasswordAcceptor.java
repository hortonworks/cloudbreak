package com.sequenceiq.freeipa.service.freeipa.user;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.repository.SyncOperationRepository;

@Component
public class SetPasswordAcceptor extends SyncOperationAcceptor {
    @Inject
    public SetPasswordAcceptor(SyncOperationRepository syncOperationRepository) {
        super(syncOperationRepository);
    }

    @Override
    SyncOperationType selector() {
        return SyncOperationType.SET_PASSWORD;
    }
}
