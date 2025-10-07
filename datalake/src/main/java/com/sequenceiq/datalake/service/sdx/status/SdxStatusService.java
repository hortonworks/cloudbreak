package com.sequenceiq.datalake.service.sdx.status;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFoundException;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;

@Service
public class SdxStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStatusService.class);

    private static final String NOT_AVAILABLE = "N/A";

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusRepository sdxStatusRepository;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    @Inject
    private SdxNotificationService sdxNotificationService;

    public void setStatusForDatalakeAndNotify(DatalakeStatusEnum status, String statusReason, SdxCluster sdxCluster) {
        setStatusForDatalake(status, statusReason, sdxCluster);
        sdxNotificationService.send(status.getDefaultResourceEvent(), sdxCluster);
        eventSenderService.sendEventAndNotification(sdxCluster, status.getDefaultResourceEvent());
    }

    public void setStatusForDatalakeAndNotify(DatalakeStatusEnum status, Collection<?> messageArgs, String statusReason, SdxCluster sdxCluster) {
        setStatusForDatalake(status, statusReason, sdxCluster);
        sdxNotificationService.send(status.getDefaultResourceEvent(), messageArgs, sdxCluster);
        eventSenderService.sendEventAndNotification(sdxCluster, status.getDefaultResourceEvent(), messageArgs);
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, String statusReason, Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(status.getDefaultResourceEvent(), cluster);
            eventSenderService.sendEventAndNotification(cluster, status.getDefaultResourceEvent());
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    public SdxCluster setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum status, String statusReason, Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(status.getDefaultResourceEvent(), cluster);
            eventSenderService.sendEventAndNotificationWithMessage(cluster, status.getDefaultResourceEvent(), statusReason);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, Collection<?> messageArgs, String statusReason, Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(status.getDefaultResourceEvent(), messageArgs, cluster);
            eventSenderService.sendEventAndNotification(cluster, status.getDefaultResourceEvent(), messageArgs);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, String statusReason, String eventMessage, String datalakeCrn) {
        return sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(status.getDefaultResourceEvent(), cluster);
            eventSenderService.sendEventAndNotificationWithMessage(cluster, status.getDefaultResourceEvent(), eventMessage);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with crn: " + datalakeCrn));
    }

    public void setStatusForDatalakeAndNotify(DatalakeStatusEnum status, ResourceEvent event, String statusReason,
            SdxCluster sdxCluster) {
        setStatusForDatalake(status, statusReason, sdxCluster);
        sdxNotificationService.send(event, sdxCluster);
        eventSenderService.sendEventAndNotification(sdxCluster, event);
    }

    public void setStatusForDatalakeAndNotify(DatalakeStatusEnum status, ResourceEvent event, Collection<?> messageArgs, String statusReason,
            SdxCluster sdxCluster) {
        setStatusForDatalake(status, statusReason, sdxCluster);
        sdxNotificationService.send(event, messageArgs, sdxCluster);
        eventSenderService.sendEventAndNotification(sdxCluster, event, messageArgs);
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, ResourceEvent event, String statusReason, Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalake(status, statusReason, cluster);
            sdxNotificationService.send(event, cluster);
            eventSenderService.sendEventAndNotification(cluster, event);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    public SdxCluster setStatusForDatalakeAndNotify(DatalakeStatusEnum status, ResourceEvent event,  Collection<?> messageArgs, String statusReason,
            Long datalakeId) {
        return sdxClusterRepository.findById(datalakeId).map(cluster -> {
            setStatusForDatalakeAndNotify(status, event, messageArgs, statusReason, cluster);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + datalakeId));
    }

    public void setStatusForDatalake(DatalakeStatusEnum status, String statusReason, SdxCluster sdxCluster) {
        try {
            transactionService.required(() -> {
                SdxStatusEntity previous = getActualStatusForSdx(sdxCluster);
                if (statusChangeIsValid(previous, status)) {
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

    private boolean statusChangeIsValid(SdxStatusEntity currentStatus, DatalakeStatusEnum newStatus) {
        if (currentStatus != null) {
            if (DELETED.equals(currentStatus.getStatus())) {
                return false;
            }
            if (currentStatus.getStatus() != null && currentStatus.getStatus().isDeleteInProgressOrCompleted() && !newStatus.isDeleteInProgressOrCompleted()) {
                return false;
            }
        }
        return true;
    }

    public List<SdxStatusEntity> findLatestSdxStatusesFilteredByStatusesAndDatalakeIds(Collection<DatalakeStatusEnum> datalakeStatusEnums,
            Collection<Long> datalakeIds) {
        return sdxStatusRepository.findLatestSdxStatusesFilteredByStatusesAndDatalakeIds(datalakeStatusEnums, datalakeIds);
    }

    public SdxStatusEntity getActualStatusForSdx(SdxCluster sdxCluster) {
        return sdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(sdxCluster);
    }

    public SdxStatusEntity getActualStatusForSdx(Long id) {
        return sdxStatusRepository.findFirstByDatalakeIdIsOrderByIdDesc(id);
    }

    public void updateInMemoryStateStore(SdxCluster sdxCluster) {
        SdxStatusEntity actualStatusForSdx = getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx != null &&
                (actualStatusForSdx.getStatus().isDeleteInProgressOrCompleted() || DELETED_ON_PROVIDER_SIDE.equals(actualStatusForSdx.getStatus()))) {
            DatalakeInMemoryStateStore.put(sdxCluster.getId(), PollGroup.CANCELLED);
            LOGGER.info("Update {} datalake status in inmemory store to {}", sdxCluster.getClusterName(), PollGroup.CANCELLED.name());
        } else {
            DatalakeInMemoryStateStore.put(sdxCluster.getId(), PollGroup.POLLABLE);
            LOGGER.info("Update {} datalake status in inmemory store to {}", sdxCluster.getClusterName(), PollGroup.POLLABLE.name());
        }
    }

    public String getShortStatusMessage(StackV4Response stack) {
        String stackStatus = stack.getStatus() == null ? "N/A" : stack.getStatus().name();
        String stackStatusReason = stack.getStatusReason() == null ? "N/A" : stack.getStatusReason();
        String clusterStatus;
        if (stack.getCluster() == null || stack.getCluster().getStatus() == null) {
            clusterStatus = "N/A";
        } else {
            clusterStatus = stack.getCluster().getStatus().name();
        }
        String clusterStatusReason;
        if (stack.getCluster() == null || stack.getCluster().getStatusReason() == null) {
            clusterStatusReason = "N/A";
        } else {
            clusterStatusReason = stack.getCluster().getStatusReason();
        }
        return String.format("Stack status: %s, reason: %s, cluster status: %s, reason: %s",
                stackStatus, stackStatusReason, clusterStatus, clusterStatusReason);
    }

    public String getShortStatusMessage(StackStatusV4Response statusResponse) {
        String stackStatus = statusResponse.getStatus() == null ? NOT_AVAILABLE : statusResponse.getStatus().name();
        String stackStatusReason = statusResponse.getStatusReason() == null ? NOT_AVAILABLE : statusResponse.getStatusReason();
        String clusterStatus = statusResponse.getClusterStatus() == null ? NOT_AVAILABLE : statusResponse.getClusterStatus().name();
        String clusterStatusReason = statusResponse.getClusterStatusReason() == null ? NOT_AVAILABLE : statusResponse.getClusterStatusReason();
        if (!stackStatusReason.equals(NOT_AVAILABLE) && stackStatusReason.equals(clusterStatusReason)) {
            return String.format("Stack status: %s, cluster status: %s, reason: %s",
                    stackStatus, clusterStatus, stackStatusReason);
        }
        return String.format("Stack status: %s, reason: %s, cluster status: %s, reason: %s",
                stackStatus, stackStatusReason, clusterStatus, clusterStatusReason);
    }
}
