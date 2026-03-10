package com.sequenceiq.cloudbreak.tls;

public record CipherSuiteOptions(CipherSuitesLimitType cipherSuitesLimitType,
    boolean legacyEncryptionProfile, boolean useIana, boolean addTls13) {
}
