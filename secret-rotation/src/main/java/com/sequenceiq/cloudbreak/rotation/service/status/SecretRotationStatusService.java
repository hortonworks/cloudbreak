package com.sequenceiq.cloudbreak.rotation.service.status;

public interface SecretRotationStatusService {

    void rotationStarted(String resourceCrn);

    void rotationFinished(String resourceCrn);

    void rotationFailed(String resourceCrn, String statusReason);
}
