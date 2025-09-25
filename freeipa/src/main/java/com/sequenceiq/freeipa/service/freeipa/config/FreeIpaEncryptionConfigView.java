package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

public class FreeIpaEncryptionConfigView {
    private final String tlsVersionsSpaceSeparated;

    private final String tlsCipherSuites;

    private final String tlsCipherSuitesRedHat8;

    public FreeIpaEncryptionConfigView(EncryptionProfileProvider encryptionProfileProvider, EncryptionProfileResponse encryptionProfileResponse) {
        Set<String> useTlsVersions =
                Optional.ofNullable(encryptionProfileResponse)
                .map(EncryptionProfileResponse::getTlsVersions)
                .orElse(null);
        Map<String, List<String>> userCipherSuits =
                Optional.ofNullable(encryptionProfileResponse)
                .map(EncryptionProfileResponse::getCipherSuites)
                .orElse(null);
        tlsVersionsSpaceSeparated = encryptionProfileProvider.getTlsVersions(useTlsVersions, " ");
        tlsCipherSuites = encryptionProfileProvider.getTlsCipherSuites(userCipherSuits, CipherSuitesLimitType.DEFAULT, ":", false);
        tlsCipherSuitesRedHat8 = encryptionProfileProvider.getTlsCipherSuites(userCipherSuits, CipherSuitesLimitType.REDHAT_VERSION8, ":", false);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("tlsVersionsSpaceSeparated", tlsVersionsSpaceSeparated);
        result.put("tlsCipherSuites", tlsCipherSuites);
        result.put("tlsCipherSuitesRedHat8", tlsCipherSuitesRedHat8);
        return result;
    }
}
