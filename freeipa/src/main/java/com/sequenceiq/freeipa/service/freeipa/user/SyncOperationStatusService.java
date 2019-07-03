package com.sequenceiq.freeipa.service.freeipa.user;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;

@Service
public class SyncOperationStatusService {
    private static final long MAX_OPERATION_CACHE_SIZE = 1000;

    private static final Duration OPERATION_EXPIRATION = Duration.ofMinutes(20L);

    // TODO replace cache with database tables
    private Cache<String, SyncOperationStatus> syncOperationStatusCache = CacheBuilder.newBuilder()
            .maximumSize(MAX_OPERATION_CACHE_SIZE)
            .expireAfterAccess(OPERATION_EXPIRATION)
            .build();

    public SyncOperationStatus startOperation(SyncOperationType syncOperationType) {
        String operationId = UUID.randomUUID().toString();
        SyncOperationStatus response = SyncOperationStatus.running(operationId, syncOperationType);
        syncOperationStatusCache.put(operationId, response);
        return response;
    }

    public SyncOperationStatus completeOperation(String operationId, List<SuccessDetails> success, List<FailureDetails> failure) {
        SyncOperationStatus response = getStatus(operationId);
        response.completed(success, failure);
        syncOperationStatusCache.put(operationId, response);
        return response;
    }

    public SyncOperationStatus failOperation(String operationId, String failureMessage) {
        SyncOperationStatus response = getStatus(operationId);
        response.failed(failureMessage);
        syncOperationStatusCache.put(operationId, response);
        return response;
    }

    public SyncOperationStatus getStatus(String operationId) {
        SyncOperationStatus response = syncOperationStatusCache.getIfPresent(operationId);
        if (response == null) {
            throw NotFoundException.notFound("SyncOperationResponse", operationId).get();
        }
        return response;
    }
}
