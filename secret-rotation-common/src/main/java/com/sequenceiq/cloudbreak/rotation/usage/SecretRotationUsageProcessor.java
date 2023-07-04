package com.sequenceiq.cloudbreak.rotation.usage;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface SecretRotationUsageProcessor {

    void rotationStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rotationFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rotationFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType);

    void rollbackStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rollbackFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType);

    void rollbackFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType);
}
