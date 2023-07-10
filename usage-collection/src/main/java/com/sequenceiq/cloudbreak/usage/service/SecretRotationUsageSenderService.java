package com.sequenceiq.cloudbreak.usage.service;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPSecretRotationEvent;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPSecretRotationStatus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class SecretRotationUsageSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationUsageSenderService.class);

    @Inject
    private UsageReporter usageReporter;

    public void rotationStarted(String secretType, String resourceCrn) {
        sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.STARTED);
    }

    public void rotationFinished(String secretType, String resourceCrn) {
        sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.FINISHED);
    }

    public void rotationFailed(String secretType, String resourceCrn, String reason) {
        sendUsageReport(secretType, resourceCrn, reason, CDPSecretRotationStatus.Value.FAILED);
    }

    public void rollbackStarted(String secretType, String resourceCrn) {
        sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.ROLLBACK_STARTED);
    }

    public void rollbackFinished(String secretType, String resourceCrn) {
        sendUsageReport(secretType, resourceCrn, null, CDPSecretRotationStatus.Value.ROLLBACK_FINISHED);
    }

    public void rollbackFailed(String secretType, String resourceCrn, String reason) {
        sendUsageReport(secretType, resourceCrn, reason, CDPSecretRotationStatus.Value.ROLLBACK_FAILED);
    }

    private void sendUsageReport(String secretType, String resourceCrn, String reason, CDPSecretRotationStatus.Value status) {
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
