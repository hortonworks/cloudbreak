package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.repository.SyncOperationRepository;

@Service
public class CleanupSyncOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupSyncOperation.class);

    @Value("${freeipa.syncoperation.cleanup.timeout-millis:1800000}")
    private  Long syncOperationTimeout;

    @Inject
    private SyncOperationRepository syncOperationRepository;

    @Scheduled(fixedDelayString = "${freeipa.syncoperation.cleanup.fixed-delay-millis:60000}",
            initialDelayString = "${freeipa.syncoperation.cleanup.initial-delay-millis:60000}")
    public void triggerCleanup() {
        try {
            cleanupStaleSyncOperation(System.currentTimeMillis() - syncOperationTimeout);
        } catch (Exception e) {
            LOGGER.error("Failed to clean up SyncOperation table", e);
        }
    }

    public void cleanupStaleSyncOperation(Long startedBeforeTime) {
        List<SyncOperation> staleOperations = syncOperationRepository.findStaleRunning(startedBeforeTime);

        LOGGER.debug("{} operations have TIMEDOUT.", staleOperations.size());
        Long endTime = System.currentTimeMillis();

        staleOperations.stream().forEach(o -> {
            LOGGER.debug("Recording that operation {} TIMEDOUT", o.getOperationId());
            o.setStatus(SynchronizationStatus.TIMEDOUT);
            o.setEndTime(endTime);
            syncOperationRepository.save(o);
        });
    }
}
