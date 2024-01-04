package com.sequenceiq.cloudbreak.rotation.service.usage;


import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.usage.service.SecretRotationUsageSenderService;

@Service
public class SecretRotationUsageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationUsageService.class);

    @Inject
    private SecretRotationUsageSenderService secretRotationUsageSenderService;

    public void rotationStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            secretRotationUsageSenderService.rotationStarted(secretType.toString(), resourceCrn);
        }
    }

    public void rotationFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            secretRotationUsageSenderService.rotationFinished(secretType.toString(), resourceCrn);
        }
    }

    public void rotationFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            secretRotationUsageSenderService.rotationFailed(secretType.toString(), resourceCrn, reason);
        }
    }

    public void rollbackStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            secretRotationUsageSenderService.rollbackStarted(secretType.toString(), resourceCrn);
        }
    }

    public void rollbackFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            secretRotationUsageSenderService.rollbackFinished(secretType.toString(), resourceCrn);
        }
    }

    public void rollbackFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            secretRotationUsageSenderService.rollbackFailed(secretType.toString(), resourceCrn, reason);
        }
    }
}
