package com.sequenceiq.cloudbreak.usage.service;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPSecretRotationEvent;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPSecretRotationStatus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.usage.SecretRotationUsageProcessor;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class SecretRotationUsageService implements SecretRotationUsageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationUsageService.class);

    @Inject
    private UsageReporter usageReporter;

    @Override
    public void rotationStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.STARTED);
        }
    }

    @Override
    public void rotationFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.FINISHED);
        }
    }

    @Override
    public void rotationFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            sendUsageReport(secretType, resourceCrn, reason, CDPSecretRotationStatus.Value.FAILED);
        }
    }

    @Override
    public void rollbackStarted(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.ROLLBACK_STARTED);
        }
    }

    @Override
    public void rollbackFinished(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.ROLLBACK_FINISHED);
        }
    }

    @Override
    public void rollbackFailed(SecretType secretType, String resourceCrn, String reason, RotationFlowExecutionType executionType) {
        if (executionType == null) {
            sendUsageReport(secretType, resourceCrn, reason, CDPSecretRotationStatus.Value.ROLLBACK_FAILED);
        }
    }

    private void sendUsageReport(SecretType secretType, String resourceCrn, String reason, CDPSecretRotationStatus.Value status) {
        try {
            LOGGER.debug("Send secret rotation usage report for secretType: {}, status: {}, reason: {}", secretType, status, reason);
            usageReporter.cdpSecretRotationEvent(CDPSecretRotationEvent.newBuilder()
                    .setAccountId(ThreadBasedUserCrnProvider.getAccountId())
                    .setSecretType(secretType.toString())
                    .setResourceCrn(resourceCrn)
                    .setReason(reason == null ? "" : reason)
                    .setStatus(status)
                    .build());
        } catch (Exception e) {
            LOGGER.error("Couldn't send usage report about secret rotation with secret type: {}, status: {}", secretType, status, e);
        }
    }
}
