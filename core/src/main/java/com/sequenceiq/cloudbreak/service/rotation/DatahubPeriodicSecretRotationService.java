package com.sequenceiq.cloudbreak.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretTypeResponse;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class DatahubPeriodicSecretRotationService implements PeriodicSecretRotationService {

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private SecretTypeListService secretTypeListService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    @Inject
    private StackRotationService stackRotationService;

    @Override
    public List<JobResource> listJobResources() {
        return stackService.getAllAliveDatahubs(Status.getUnschedulableStatuses());
    }

    @Override
    public Optional<MdcContextInfoProvider> getMdcContextInfoProvider(String resourceCrn) {
        return Optional.ofNullable(getResourceByCrn(resourceCrn));
    }

    @Override
    public boolean isSchedulable(String resourceCrn) {
        StackDto stackDto = getResourceByCrn(resourceCrn);
        return !flowLogService.isOtherFlowRunning(stackDto.getId())
            && stackDto.getStatus().isAvailable();
    }

    @Override
    public List<String> listRotatableSecretNames(String resourceCrn) {
        List<DistroXSecretTypeResponse> list = secretTypeListService
            .listRotatableSecretType(resourceCrn, DistroXSecretTypeResponse.converter());
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
        stackRotationService.rotateSecrets(resourceCrn, dueSecretNames, null,
            Map.of(IGNORE_PREVALIDATE_ERRORS, "true"));
    }

    public StackDto getResourceByCrn(String resourceCrn) {
        return stackDtoService.getByCrn(resourceCrn);
    }

    @Override
    public Instant getResourceCreationDate(String resourceCrn) {
        return stackService.getCreatedByResourceCrn(resourceCrn);
    }
}
