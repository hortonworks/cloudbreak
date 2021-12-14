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
public class OperationTimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationTimeoutService.class);

    @Value("${freeipa.operation.cleanup.timeout-millis}")
    private Long operationTimeout;

    @Value("${freeipa.operation.cleanup.upgrade.timeout-millis}")
    private Long upgradeOperationTimeout;

    @Inject
    private OperationRepository operationRepository;

    @Scheduled(fixedDelayString = "${freeipa.operation.cleanup.fixed-delay-millis}",
            initialDelayString = "${freeipa.operation.cleanup.initial-delay-millis}")
    public void triggerCleanup() {
        try {
            cleanupNonUpgradeStaleOperation(System.currentTimeMillis() - operationTimeout);
            cleanupUpgradeStaleOperation(System.currentTimeMillis() - upgradeOperationTimeout);
        } catch (Exception e) {
            LOGGER.error("Failed to clean up Operation table", e);
        }
    }

    private void cleanupNonUpgradeStaleOperation(Long startedBeforeTime) {
        List<Operation> staleOperations = operationRepository.findNonUpgradeStaleRunning(startedBeforeTime);
        LOGGER.debug("{} non upgrade operations have TIMEDOUT.", staleOperations.size());
        cleanupStaleOperation(staleOperations);
    }

    private void cleanupUpgradeStaleOperation(Long startedBeforeTime) {
        List<Operation> staleOperations = operationRepository.findUpgradeStaleRunning(startedBeforeTime);
        LOGGER.debug("{} upgrade operations have TIMEDOUT.", staleOperations.size());
        cleanupStaleOperation(staleOperations);
    }

    private void cleanupStaleOperation(List<Operation> staleOperations) {
        Long endTime = System.currentTimeMillis();

        staleOperations.forEach(o -> {
            LOGGER.debug("Recording that operation {} TIMEDOUT", o.getOperationId());
            o.setStatus(OperationState.TIMEDOUT);
            o.setEndTime(endTime);
        });
        operationRepository.saveAll(staleOperations);
    }
}
