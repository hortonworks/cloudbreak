package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.repository.SyncOperationRepository;

@Service
public class CleanupSyncOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupSyncOperation.class);

    private static final Long SYNC_OPERATION_TIMEOUT = 30L * 60L * 1000L;

    @Inject
    private SyncOperationRepository syncOperationRepository;

    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000)
    public void triggerCleanup() {
        try {
            cleanupStaleSyncOperation(System.currentTimeMillis() - SYNC_OPERATION_TIMEOUT);
        } catch (Exception e) {
            LOGGER.error("Failed to clean up SyncOperation table", e);
        }
    }

    public void cleanupStaleSyncOperation(Long startedBeforeTime) {
        List<SyncOperation> staleOperations = syncOperationRepository.findStaleRunning(startedBeforeTime);

        if (!staleOperations.isEmpty()) {
            LOGGER.debug("Attempting to TIMEOUT {} operations", staleOperations.size());
            Long endTime = System.currentTimeMillis();

            staleOperations.stream().forEach(o -> {
                LOGGER.debug("TIMEOUT operation {}", o.getOperationId());
                o.setStatus(SynchronizationStatus.TIMEDOUT);
                o.setEndTime(endTime);
                syncOperationRepository.save(o);
            });
        }
    }
}
