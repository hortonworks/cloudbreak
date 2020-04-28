package com.sequenceiq.datalake.service.sdx.status;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFoundException;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETED;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;

@Service
public class SdxStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStatusService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusRepository sdxStatusRepository;

    @Inject
    private SdxNotificationService sdxNotificationService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    public void setStatusForDatalakeAndNotify(DatalakeStatusEnum status, String statusReason, SdxCluster sdxCluster) {
        setStatusForDatalake(status, statusReason, sdxCluster);
        sdxNotificationService.send(status.getDefaultResourceEvent(), sdxCluster);
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, String statusReason, Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(status.getDefaultResourceEvent(), cluster);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    public void setStatusForDatalakeAndNotify(DatalakeStatusEnum status, ResourceEvent event, String statusReason, SdxCluster sdxCluster) {
        setStatusForDatalake(status, statusReason, sdxCluster);
        sdxNotificationService.send(event, sdxCluster);
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, ResourceEvent event, String statusReason, Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(event, cluster);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    private void setStatusForDatalake(DatalakeStatusEnum status, String statusReason, SdxCluster sdxCluster) {
        try {
            transactionService.required(() -> {
                SdxStatusEntity previous = getActualStatusForSdx(sdxCluster);
                if (statusChangeIsValid(previous)) {
                    SdxStatusEntity sdxStatusEntity = createStatusEntity(status, statusReason, sdxCluster);
                    if (DELETED.equals(status)) {
                        setDeletedTime(sdxCluster);
                    }
                    sdxStatusRepository.save(sdxStatusEntity);
                    updateInMemoryStateStore(sdxCluster);
                    LOGGER.info("Updated status of Datalake with name: {} from {} to {} with statusReason {}",
                            sdxCluster.getClusterName(), getPreviousStatusText(previous), status, statusReason);
                } else if (DELETED.equals(previous.getStatus())) {
                    if (sdxCluster.getDeleted() == null) {
                        setDeletedTime(sdxCluster);
                    } else {
                        throw notFoundException("SDX cluster", sdxCluster.getClusterName());
                    }
                } else {
                    throw new DatalakeStatusUpdateException(getActualStatusForSdx(sdxCluster).getStatus(), status);
                }
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Exception happened while transaction was executed", e);
            throw e.getCause();
        }
    }

    private void setDeletedTime(SdxCluster sdxCluster) {
        sdxClusterRepository.findById(sdxCluster.getId()).ifPresent(sdx -> {
            sdx.setDeleted(clock.getCurrentTimeMillis());
            sdxClusterRepository.save(sdx);
        });
    }

    private String getPreviousStatusText(SdxStatusEntity previous) {
        return previous == null ? "null" : previous.getStatus().name();
    }

    private SdxStatusEntity createStatusEntity(DatalakeStatusEnum status, String statusReason, SdxCluster sdxCluster) {
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setCreated(new Date().getTime());
        sdxStatusEntity.setStatus(status);
        sdxStatusEntity.setStatusReason(statusReason);
        sdxStatusEntity.setDatalake(sdxCluster);
        return sdxStatusEntity;
    }

    private boolean statusChangeIsValid(SdxStatusEntity currentStatus) {
        return !(currentStatus != null && DELETED.equals(currentStatus.getStatus()));
    }

    public List<SdxStatusEntity> findDistinctFirstByStatusInAndDatalakeIdOrderByIdDesc(Collection<DatalakeStatusEnum> datalakeStatusEnums,
            Collection<Long> datalakeIds) {
        return sdxStatusRepository.findDistinctFirstByStatusInAndDatalakeIdInOrderByIdDesc(datalakeStatusEnums, datalakeIds);
    }

    public SdxStatusEntity getActualStatusForSdx(SdxCluster sdxCluster) {
        return sdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(sdxCluster);
    }

    public void updateInMemoryStateStore(SdxCluster sdxCluster) {
        SdxStatusEntity actualStatusForSdx = getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx != null && actualStatusForSdx.getStatus().isDeleteInProgressOrCompleted()) {
            DatalakeInMemoryStateStore.put(sdxCluster.getId(), PollGroup.CANCELLED);
            LOGGER.info("Update {} datalake status in inmemory store to {}", sdxCluster.getClusterName(), PollGroup.CANCELLED.name());
        } else {
            DatalakeInMemoryStateStore.put(sdxCluster.getId(), PollGroup.POLLABLE);
            LOGGER.info("Update {} datalake status in inmemory store to {}", sdxCluster.getClusterName(), PollGroup.POLLABLE.name());
        }
    }
}
