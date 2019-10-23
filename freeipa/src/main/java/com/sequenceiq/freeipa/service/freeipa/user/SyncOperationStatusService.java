package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncOperationStatusService.class);

    @Inject
    private SyncOperationRepository syncOperationRepository;

    @Inject
    private SyncOperationToSyncOperationStatus syncOperationToSyncOperationStatus;

    @Inject
    private List<SyncOperationAcceptor> syncOperationAcceptorList;

    private Map<SyncOperationType, SyncOperationAcceptor> syncOperationAcceptorMap = new HashMap<>();

    @PostConstruct
    public void init() {
        syncOperationAcceptorList.stream()
                .peek(a -> LOGGER.info("Registering acceptor for {}", a.selector()))
                .forEach(a -> syncOperationAcceptorMap.put(a.selector(), a));

        for (SyncOperationType syncOperationType : SyncOperationType.values()) {
            if (!syncOperationAcceptorMap.containsKey(syncOperationType)) {
                throw new IllegalStateException("No Acceptor injected for SyncOperationType " + syncOperationType);
            }
        }
    }

    public SyncOperation startOperation(String accountId, SyncOperationType syncOperationType,
            Collection<String> environmentCrns, Collection<String> userCrns) {
        return startOperation(accountId, syncOperationType, environmentCrns, userCrns, false);
    }

    public SyncOperation startOperation(String accountId, SyncOperationType syncOperationType,
            Collection<String> environmentCrns, Collection<String> userCrns, boolean force) {
        SyncOperation syncOperation = requestOperation(accountId, syncOperationType, environmentCrns, userCrns);

        if (force) {
            syncOperation = acceptOperation(syncOperation);
        } else {
            SyncOperationAcceptor acceptor = syncOperationAcceptorMap.get(syncOperationType);
            AcceptResult acceptResult = acceptor.accept(syncOperation);
            if (acceptResult.isAccepted()) {
                syncOperation = acceptOperation(syncOperation);
            } else {
                syncOperation = rejectOperation(syncOperation, acceptResult.getRejectionMessage().orElse("Rejected for unspecified reason."));
            }
        }

        return syncOperationRepository.save(syncOperation);
    }

    public SyncOperation completeOperation(String operationId, List<SuccessDetails> success, List<FailureDetails> failure) {
        SyncOperation syncOperation = getSyncOperationForOperationId(operationId);
        syncOperation.setStatus(SynchronizationStatus.COMPLETED);
        syncOperation.setSuccessList(success);
        syncOperation.setFailureList(failure);
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

    private SyncOperation requestOperation(String accountId, SyncOperationType syncOperationType,
            Collection<String> environmentCrns, Collection<String> userCrns) {
        SyncOperation syncOperation = new SyncOperation();
        syncOperation.setOperationId(UUID.randomUUID().toString());
        syncOperation.setStatus(SynchronizationStatus.REQUESTED);
        syncOperation.setAccountId(accountId);
        syncOperation.setSyncOperationType(syncOperationType);
        syncOperation.setEnvironmentList(List.copyOf(environmentCrns));
        syncOperation.setUserList(List.copyOf(userCrns));
        return syncOperationRepository.save(syncOperation);
    }

    private SyncOperation acceptOperation(SyncOperation syncOperation) {
        syncOperation.setStatus(SynchronizationStatus.RUNNING);
        return syncOperation;
    }

    private SyncOperation rejectOperation(SyncOperation syncOperation, String reason) {
        syncOperation.setStatus(SynchronizationStatus.REJECTED);
        syncOperation.setEndTime(System.currentTimeMillis());
        syncOperation.setError(reason);
        return syncOperation;
    }

    private SyncOperation getSyncOperationForOperationId(String operationId) {
        Optional<SyncOperation> syncOperationOptional = syncOperationRepository.findByOperationId(operationId);
        if (!syncOperationOptional.isPresent()) {
            throw NotFoundException.notFound("SyncOperation", "operationId").get();
        }
        return syncOperationOptional.get();
    }
}