package com.sequenceiq.datalake.service.sdx.status;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETED;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxStatusRepository;

@Service
public class SdxStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStatusService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusRepository sdxStatusRepository;

    public void setStatusForDatalake(DatalakeStatusEnum status, String statusReason, Long datalakeId) {
        sdxClusterRepository.findById(datalakeId).ifPresent(datalakeCluster -> setStatusForDatalake(status, statusReason, datalakeCluster));
    }

    public void setStatusForDatalake(DatalakeStatusEnum status, String statusReason, SdxCluster sdxCluster) {
        LOGGER.info("Updating status of Datalake cluster {} to {}", sdxCluster.getClusterName(), status.name());
        if (statusChangeIsValid(sdxCluster)) {
            SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
            sdxStatusEntity.setCreated(new Date().getTime());
            sdxStatusEntity.setStatus(status);
            sdxStatusEntity.setStatusReason(statusReason);
            sdxStatusEntity.setDatalake(sdxCluster);
            sdxStatusRepository.save(sdxStatusEntity);
            updateInMemoryStateStore(sdxCluster);
        }
    }

    private boolean statusChangeIsValid(SdxCluster sdxCluster) {
        SdxStatusEntity previousStatusForSdx = getActualStatusForSdx(sdxCluster);
        if (previousStatusForSdx != null && DELETED.equals(previousStatusForSdx.getStatus())) {
            LOGGER.info("Can not modify deleted datalake's ({}) status", sdxCluster.getClusterName());
            return false;
        } else {
            return true;
        }
    }

    public List<SdxStatusEntity> findDistinctFirstByStatusInAndDatalakeIdOrderByIdDesc(Collection<DatalakeStatusEnum> datalakeStatusEnums,
            Collection<Long> datalakeIds) {
        return sdxStatusRepository.findDistinctFirstByStatusInAndDatalakeIdOrderByIdDesc(datalakeStatusEnums, datalakeIds);
    }

    public SdxStatusEntity getActualStatusForSdx(SdxCluster sdxCluster) {
        return sdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(sdxCluster);
    }

    public void updateInMemoryStateStore(SdxCluster sdxCluster) {
        SdxStatusEntity actualStatusForSdx = getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx != null &&   actualStatusForSdx.getStatus().isDeleteInProgressOrCompleted()) {
            DatalakeInMemoryStateStore.put(sdxCluster.getId(), PollGroup.CANCELLED);
            LOGGER.info("Update {} datalake status in inmemory store to {}", sdxCluster.getClusterName(), PollGroup.CANCELLED.name());
        } else {
            DatalakeInMemoryStateStore.put(sdxCluster.getId(), PollGroup.POLLABLE);
            LOGGER.info("Update {} datalake status in inmemory store to {}", sdxCluster.getClusterName(), PollGroup.POLLABLE.name());
        }
    }
}
