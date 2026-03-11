package com.sequenceiq.freeipa.service.rotation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeipaSecretTypeResponse;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaPeriodicSecretRotationService implements PeriodicSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPeriodicSecretRotationService.class);

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackService stackService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private SecretTypeListService secretTypeListService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    @Inject
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @Override
    public List<JobResource> listJobResources() {
        return stackService.findAllForAutoSync();
    }

    @Override
    public boolean isSchedulable(String resourceCrn) {
        return getResourceByCrn(resourceCrn)
                .map(resource -> !flowLogService.isOtherFlowRunning(resource.stackStatus().getId())
                        && resource.stackStatus().getStatus().isAvailable())
                .orElse(false);
    }

    @Override
    public List<String> listRotatableSecretNames(String resourceCrn) {
        return getResourceByCrn(resourceCrn)
                .map(resource -> {
                    List<FreeipaSecretTypeResponse> list = secretTypeListService
                            .listRotatableSecretType(resource.environmentCrn(), FreeipaSecretTypeResponse.converter());
                    return list.stream()
                            .map(BaseSecretTypeResponse::getSecretType)
                            .toList();
                })
                .orElse(List.of());
    }

    @Override
    public List<SecretType> enabledSecretTypes() {
        return enabledSecretTypes;
    }

    @Override
    public void triggerRotation(String resourceCrn, List<String> dueSecretNames) {
        getResourceByCrn(resourceCrn).ifPresent(resource -> {
            String accountId = Crn.fromString(resource.resourceCrn()).getAccountId();
            FreeIpaSecretRotationRequest request = new FreeIpaSecretRotationRequest();
            request.setSecrets(dueSecretNames);
            freeIpaSecretRotationService.rotateSecretsByCrn(accountId, resource.environmentCrn(), request);
        });
    }

    public Optional<FreeIpaListView> getResourceByCrn(String resourceCrn) {
        return freeIpaService.getViewByResourceCrn(resourceCrn);
    }

    @Override
    public Optional<MdcContextInfoProvider> getMdcContextInfoProvider(String resourceCrn) {
        return getResourceByCrn(resourceCrn).map(FreeIpaMdcContextInfoProvider::new);
    }

    @Override
    public Instant getResourceCreationDate(String resourceCrn) {
        return stackService.getCreatedByResourceCrn(resourceCrn);
    }
}

