package com.sequenceiq.cloudbreak.rotation.secret.usage;

import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public interface SecretRotationUsageProcessor {

    void rotationStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rotationFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rotationFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType);

    void rollbackStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rollbackFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rollbackFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType);
}
