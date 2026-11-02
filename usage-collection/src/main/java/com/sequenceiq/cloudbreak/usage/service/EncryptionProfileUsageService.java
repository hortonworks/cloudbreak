package com.sequenceiq.cloudbreak.usage.service;

import java.util.Collection;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class EncryptionProfileUsageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileUsageService.class);

    private static final String SEPARATOR = ",";

    @Inject
    private UsageReporter usageReporter;

    public void sendCreatedUsageReport(String encryptionProfileCrn, String accountId, Collection<String> tlsVersions, Collection<String> cipherSuites) {
        sendUsageReport(UsageProto.CDPEncryptionProfileStatus.Value.CREATED, encryptionProfileCrn, accountId, tlsVersions, cipherSuites);
    }

    public void sendDeletedUsageReport(String encryptionProfileCrn, String accountId, Collection<String> tlsVersions, Collection<String> cipherSuites) {
        sendUsageReport(UsageProto.CDPEncryptionProfileStatus.Value.DELETED, encryptionProfileCrn, accountId, tlsVersions, cipherSuites);
    }

    private void sendUsageReport(UsageProto.CDPEncryptionProfileStatus.Value status, String encryptionProfileCrn, String accountId,
            Collection<String> tlsVersions, Collection<String> cipherSuites) {
        try {
            usageReporter.cdpEncryptionProfileEvent(UsageProto.CDPEncryptionProfileEvent.newBuilder()
                    .setStatus(status)
                    .setEncryptionProfileCrn(Objects.requireNonNullElse(encryptionProfileCrn, ""))
                    .setAccountId(Objects.requireNonNullElse(accountId, ""))
                    .setTlsVersions(joinSorted(tlsVersions))
                    .setCipherSuites(joinInOrder(cipherSuites))
                    .build());
        } catch (Exception e) {
            LOGGER.error("Couldn't send usage report about encryption profile [crn: {}] {} because: ", encryptionProfileCrn, status, e);
        }
    }

    private static String joinSorted(Collection<String> values) {
        return values == null ? "" : String.join(SEPARATOR, values.stream().filter(Objects::nonNull).sorted().toList());
    }

    private static String joinInOrder(Collection<String> values) {
        return values == null ? "" : String.join(SEPARATOR, values.stream().filter(Objects::nonNull).toList());
    }
}
