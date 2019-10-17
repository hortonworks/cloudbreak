package com.sequenceiq.freeipa.service.operation;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;

@Service
public class CleanupOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupOperation.class);

    @Value("${freeipa.operation.cleanup.timeout-millis:1800000}")
    private  Long operationTimeout;

    @Inject
    private OperationRepository operationRepository;

    @Scheduled(fixedDelayString = "${freeipa.operation.cleanup.fixed-delay-millis:60000}",
            initialDelayString = "${freeipa.operation.cleanup.initial-delay-millis:60000}")
    public void triggerCleanup() {
        try {
            cleanupStaleOperation(System.currentTimeMillis() - operationTimeout);
        } catch (Exception e) {
            LOGGER.error("Failed to clean up SyncOperation table", e);
        }
    }

    public void cleanupStaleOperation(Long startedBeforeTime) {
        List<Operation> staleOperations = operationRepository.findStaleRunning(startedBeforeTime);

        LOGGER.debug("{} operations have TIMEDOUT.", staleOperations.size());
        Long endTime = System.currentTimeMillis();

        staleOperations.stream().forEach(o -> {
            LOGGER.debug("Recording that operation {} TIMEDOUT", o.getOperationId());
            o.setStatus(OperationState.TIMEDOUT);
            o.setEndTime(endTime);
            operationRepository.save(o);
        });
    }
}
