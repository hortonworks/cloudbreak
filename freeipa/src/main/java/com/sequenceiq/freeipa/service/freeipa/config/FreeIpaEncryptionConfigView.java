package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.tls.TlsSpecificationsHelper;
import com.sequenceiq.cloudbreak.tls.TlsSpecificationsHelper.CipherSuitesLimitType;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

public class FreeIpaEncryptionConfigView {
    private final String tlsVersionsSpaceSeparated;

    private final String tlsVersionsCommaSeparated;

    private final String tlsCipherSuites;

    private final String tlsCipherSuitesRedHat8;

    public FreeIpaEncryptionConfigView(EncryptionProfileResponse encryptionProfileResponse) {
        Set<String> usetTlsVersions =
                Optional.ofNullable(encryptionProfileResponse)
                .map(EncryptionProfileResponse::getTlsVersions)
                .orElse(null);
        Map<String, Set<String>> userCipherSuits =
                Optional.ofNullable(encryptionProfileResponse)
                .map(EncryptionProfileResponse::getCipherSuites)
                .orElse(null);
        tlsVersionsSpaceSeparated = TlsSpecificationsHelper.getTlsVersions(usetTlsVersions, " ");
        tlsVersionsCommaSeparated = TlsSpecificationsHelper.getTlsVersions(usetTlsVersions, ",");
        tlsCipherSuites = TlsSpecificationsHelper.getTlsCipherSuites(userCipherSuits, CipherSuitesLimitType.DEFAULT, ":", false);
        tlsCipherSuitesRedHat8 = TlsSpecificationsHelper.getTlsCipherSuites(userCipherSuits, CipherSuitesLimitType.REDHAT_VERSION8, ":", false);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("tlsVersionsSpaceSeparated", tlsVersionsSpaceSeparated);
        result.put("tlsVersionsCommaSeparated", tlsVersionsCommaSeparated);
        result.put("tlsCipherSuites", tlsCipherSuites);
        result.put("tlsCipherSuitesRedHat8", tlsCipherSuitesRedHat8);
        return result;
    }
}
