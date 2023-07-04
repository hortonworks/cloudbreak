package com.sequenceiq.cloudbreak.rotation.service;

import org.springframework.stereotype.Component;

@Component
public class DefaultSecretRotationStatusService implements SecretRotationStatusService {

    @Override
    public void rotationStarted(String resourceCrn) {
    }

    @Override
    public void rotationFinished(String resourceCrn) {
    }

    @Override
    public void rotationFailed(String resourceCrn, String statusReason) {
    }
}
