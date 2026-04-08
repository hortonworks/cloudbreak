package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.sdx.api.model.SdxSecretTypeResponse;

@Service
public class DatalakePeriodicSecretRotationService implements PeriodicSecretRotationService {

    @Inject
    private SdxRotationService sdxRotationService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusRepository sdxStatusRepository;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private SecretTypeListService secretTypeListService;

    @Override
    public List<JobResource> listJobResources() {
        return sdxClusterRepository.findAllAliveView();
    }

    @Override
    public boolean isSchedulable(String resourceCrn) {
        SdxCluster sdxCluster = getResourceByCrn(resourceCrn);
        SdxStatusEntity sdxStatusEntity = sdxStatusRepository.findFirstByDatalakeIdIsOrderByIdDesc(sdxCluster.getId());
        if (sdxStatusEntity == null) {
            throw new NotFoundException(
                String.format("Datalake stack status with resource crn [%s] not found", resourceCrn));
        }
        return !flowLogService.isOtherFlowRunning(sdxCluster.getId())
            && DatalakeStatusEnum.RUNNING.equals(sdxStatusEntity.getStatus());
    }

    @Override
    public List<String> listRotatableSecretNames(String resourceCrn) {
        List<SdxSecretTypeResponse> list = secretTypeListService
            .listRotatableSecretType(resourceCrn, SdxSecretTypeResponse.converter());
        return list.stream()
            .map(BaseSecretTypeResponse::getSecretType)
            .toList();
    }

    @Override
    public List<SecretType> enabledSecretTypes() {
        return enabledSecretTypes;
    }

    @Override
    public void triggerRotation(String resourceCrn, List<String> dueSecretNames) {
        sdxRotationService.triggerSecretRotation(resourceCrn, dueSecretNames, null,
            Map.of(IGNORE_PREVALIDATE_ERRORS, "true"));
    }

    public SdxCluster getResourceByCrn(String resourceCrn) {
        return sdxClusterRepository.findByCrnAndDeletedIsNull(resourceCrn)
            .orElseThrow(() -> new NotFoundException(
                String.format("Datalake stack with resource crn [%s] not found or deleted", resourceCrn)));
    }

    @Override
    public Instant getResourceCreationDate(String resourceCrn) {
        Optional<Long> created = sdxClusterRepository.getCreatedByResourceCrn(resourceCrn);
        return Instant.ofEpochMilli(created.orElseThrow(() -> new NotFoundException(
            String.format("Datalake stack with resource crn [%s] not found or deleted", resourceCrn))));
    }
}
