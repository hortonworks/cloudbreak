package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.flow.rotation.status.service.SecretRotationStatusService;

@Primary
@Component
public class StackSecretRotationStatusService implements SecretRotationStatusService {

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public void rotationStarted(String resourceCrn) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_STARTED, null);
    }

    @Override
    public void rotationFinished(String resourceCrn) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINISHED, null);
    }

    @Override
    public void rotationFailed(String resourceCrn, String statusReason) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FAILED, statusReason);
    }
}
