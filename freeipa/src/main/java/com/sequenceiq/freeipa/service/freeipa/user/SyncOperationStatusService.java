package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.freeipa.user.SyncOperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.repository.SyncOperationRepository;

@Service
public class SyncOperationStatusService {

    @Inject
    private SyncOperationRepository syncOperationRepository;

    @Inject
    private SyncOperationToSyncOperationStatus syncOperationToSyncOperationStatus;

    public SyncOperation startOperation(String accountId, SyncOperationType syncOperationType) {
        SyncOperation syncOperation = new SyncOperation();
        syncOperation.setOperationId(UUID.randomUUID().toString());
        syncOperation.setStatus(SynchronizationStatus.RUNNING);
        syncOperation.setAccountId(accountId);
        syncOperation.setSyncOperationType(syncOperationType);
        return syncOperationRepository.save(syncOperation);
    }

    public SyncOperation completeOperation(String operationId, List<SuccessDetails> success, List<FailureDetails> failure) {
        SyncOperation syncOperation = getSyncOperationForOperationId(operationId);
        syncOperation.setStatus(SynchronizationStatus.COMPLETED);
        syncOperation.setSuccessList(new Json(success));
        syncOperation.setFailureList(new Json(failure));
        syncOperation.setEndTime(System.currentTimeMillis());
        return syncOperationRepository.save(syncOperation);
    }

    public SyncOperation failOperation(String operationId, String failureMessage) {
        SyncOperation syncOperation = getSyncOperationForOperationId(operationId);
        syncOperation.setStatus(SynchronizationStatus.FAILED);
        syncOperation.setError(failureMessage);
        syncOperation.setEndTime(System.currentTimeMillis());
        return syncOperationRepository.save(syncOperation);
    }

    public SyncOperationStatus getStatus(String operationId) {
        return syncOperationToSyncOperationStatus.convert(getSyncOperationForOperationId(operationId));
    }

    private SyncOperation getSyncOperationForOperationId(String operationId) {
        Optional<SyncOperation> syncOperationOptional = syncOperationRepository.findByOperationId(operationId);
        if (!syncOperationOptional.isPresent()) {
            throw NotFoundException.notFound("SyncOperation", "operationId").get();
        }
        return syncOperationOptional.get();
    }
}