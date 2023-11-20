package com.sequenceiq.cloudbreak.rotation.service.status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class DefaultSecretRotationStatusService implements SecretRotationStatusService {

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType, String reason) {
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
    }
}
