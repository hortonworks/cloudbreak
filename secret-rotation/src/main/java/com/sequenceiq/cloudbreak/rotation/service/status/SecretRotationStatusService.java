package com.sequenceiq.cloudbreak.rotation.service.status;

import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface SecretRotationStatusService {

    void rotationStarted(String resourceCrn, SecretType secretType);

    void rotationFinished(String resourceCrn, SecretType secretType);

    void rotationFailed(String resourceCrn, SecretType secretType, String reason);

    void rollbackStarted(String resourceCrn, SecretType secretType, String reason);

    void rollbackFinished(String resourceCrn, SecretType secretType);

    void rollbackFailed(String resourceCrn, SecretType secretType, String reason);

    void finalizeStarted(String resourceCrn, SecretType secretType);

    void finalizeFinished(String resourceCrn, SecretType secretType);

    void finalizeFailed(String resourceCrn, SecretType secretType, String reason);

    void preVaildationFailed(String resourceCrn, SecretType secretType, String reason);
}
