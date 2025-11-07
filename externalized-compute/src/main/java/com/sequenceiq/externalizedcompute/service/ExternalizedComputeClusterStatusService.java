package com.sequenceiq.externalizedcompute.service;

import static com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum.DELETED;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.statestore.ExternalizedComputeInMemoryStateStore;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterStatusRepository;
import com.sequenceiq.externalizedcompute.service.exception.ExternalizedComputeClusterStatusUpdateException;

@Service
public class ExternalizedComputeClusterStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterStatusService.class);

    @Inject
    private ExternalizedComputeClusterStatusRepository externalizedComputeClusterStatusRepository;

    @Inject
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    public void setStatus(Long id, ExternalizedComputeClusterStatusEnum newStatus, String statusReason) {
        ExternalizedComputeCluster externalizedComputeCluster = externalizedComputeClusterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Externalized compute cluster not found with id " + id));
        setStatus(externalizedComputeCluster, newStatus, statusReason);
    }

    public void setStatus(ExternalizedComputeCluster externalizedComputeCluster, ExternalizedComputeClusterStatusEnum newStatus, String statusReason) {
        try {
            transactionService.required(() -> {
                ExternalizedComputeClusterStatus previous = getActualStatus(externalizedComputeCluster);
                LOGGER.info("Trying to set externalized compute cluster {} status from: {} to {} reason {}", externalizedComputeCluster.getName(),
                        getStatusText(previous), newStatus, statusReason);
                if (statusChangeIsValid(previous, newStatus)) {
                    ExternalizedComputeClusterStatus statusEntity = createStatusEntity(newStatus, statusReason, externalizedComputeCluster);
                    if (DELETED.equals(newStatus)) {
                        setDeletedTime(externalizedComputeCluster);
                    }
                    externalizedComputeClusterStatusRepository.save(statusEntity);
                    updateInMemoryStateStore(externalizedComputeCluster);
                    LOGGER.info("Updated status of externalized compute cluster with name: {} from {} to {} with statusReason {}",
                            externalizedComputeCluster.getName(), getStatusText(previous), getStatusText(statusEntity), statusReason);
                } else if (previous != null && DELETED.equals(previous.getStatus()) && DELETED.equals(newStatus)) {
                    if (externalizedComputeCluster.getDeleted() == null) {
                        setDeletedTime(externalizedComputeCluster);
                    }
                } else {
                    throw new ExternalizedComputeClusterStatusUpdateException(getStatusText(previous), newStatus);
                }
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Exception happened while set status transaction was executed", e);
            throw e.getCause();
        }
    }

    public ExternalizedComputeClusterStatus getActualStatus(ExternalizedComputeCluster externalizedComputeCluster) {
        return externalizedComputeClusterStatusRepository.findFirstByExternalizedComputeClusterIsOrderByIdDesc(externalizedComputeCluster);
    }

    public void updateInMemoryStateStore(ExternalizedComputeCluster externalizedComputeCluster) {
        ExternalizedComputeClusterStatus actualStatus = getActualStatus(externalizedComputeCluster);
        if (actualStatus != null &&
                (actualStatus.getStatus().isDeleteInProgressOrCompleted())) {
            ExternalizedComputeInMemoryStateStore.put(externalizedComputeCluster.getId(), PollGroup.CANCELLED);
            LOGGER.info("Update '{}' status in inmemory store to {}", externalizedComputeCluster.getName(), PollGroup.CANCELLED.name());
        } else {
            ExternalizedComputeInMemoryStateStore.put(externalizedComputeCluster.getId(), PollGroup.POLLABLE);
            LOGGER.info("Update '{}' status in inmemory store to {}", externalizedComputeCluster.getName(), PollGroup.POLLABLE.name());
        }
    }

    public List<ExternalizedComputeClusterStatus> findLatestClusterStatusesFilteredByStatusesAndClusterIds(
            Collection<ExternalizedComputeClusterStatusEnum> statusEnums, Collection<Long> ids) {
        return externalizedComputeClusterStatusRepository.findLatestStatusesFilteredByStatusesAndClusterIds(statusEnums, ids);
    }

    private boolean statusChangeIsValid(ExternalizedComputeClusterStatus currentStatus, ExternalizedComputeClusterStatusEnum newStatus) {
        if (currentStatus != null) {
            if (DELETED.equals(currentStatus.getStatus())) {
                return false;
            }
            if (currentStatus.getStatus() != null && currentStatus.getStatus().isDeleteInProgressOrCompleted() &&
                    !newStatus.isDeleteInProgressOrCompletedOrFailedOrReinitializeInProgress()) {
                return false;
            }
        }
        return true;
    }

    private ExternalizedComputeClusterStatus createStatusEntity(ExternalizedComputeClusterStatusEnum newStatus, String statusReason,
            ExternalizedComputeCluster externalizedComputeCluster) {
        ExternalizedComputeClusterStatus status = new ExternalizedComputeClusterStatus();
        status.setCreated(new Date().getTime());
        status.setStatus(newStatus);
        status.setStatusReason(statusReason);
        status.setExternalizedComputeCluster(externalizedComputeCluster);
        return status;
    }

    private void setDeletedTime(ExternalizedComputeCluster externalizedComputeCluster) {
        externalizedComputeClusterRepository.findById(externalizedComputeCluster.getId()).ifPresent(cluster -> {
            cluster.setDeleted(clock.getCurrentTimeMillis());
            externalizedComputeClusterRepository.save(cluster);
        });
    }

    private String getStatusText(ExternalizedComputeClusterStatus previous) {
        return previous == null ? "null" : previous.getStatus().name();
    }

}
