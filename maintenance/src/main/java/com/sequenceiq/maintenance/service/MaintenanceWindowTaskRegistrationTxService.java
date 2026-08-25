package com.sequenceiq.maintenance.service;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Service;

import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;

/**
 * Runs task registration writes and post-race reads in independent transactions so a failed
 * {@code save} does not mark the caller's persistence context rollback-only.
 */
@Service
public class MaintenanceWindowTaskRegistrationTxService {

    private final MaintenanceWindowTaskRepository taskRepository;

    @Inject
    public MaintenanceWindowTaskRegistrationTxService(MaintenanceWindowTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(TxType.REQUIRES_NEW)
    public MaintenanceWindowTask save(MaintenanceWindowTask task) {
        return taskRepository.save(task);
    }

    @Transactional(TxType.REQUIRES_NEW)
    public Optional<MaintenanceWindowTask> findActive(
            String accountId, String resourceCrn, String taskType, String workItemId) {
        return taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                accountId, resourceCrn, taskType, workItemId);
    }
}
