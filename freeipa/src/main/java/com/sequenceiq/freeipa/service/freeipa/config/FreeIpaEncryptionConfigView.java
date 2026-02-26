package com.sequenceiq.freeipa.service.freeipa.config;

import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.DEFAULT;
import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.REDHAT_VERSION8;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

public class FreeIpaEncryptionConfigView {
    private final String tlsVersionsSpaceSeparated;

    private final String tlsCipherSuites;

    private final String tlsCipherSuitesRedHat8;

    private final String tls12CipherSuites;

    private final String tls13CipherSuites;

    public FreeIpaEncryptionConfigView(EncryptionProfileProvider encryptionProfileProvider, EncryptionProfileResponse encryptionProfileResponse) {
        Set<String> userTlsVersions = encryptionProfileResponse.getTlsVersions();
        Map<String, List<String>> userEncryptionProfileMap = encryptionProfileResponse.getCipherSuites();
        boolean defaultEncryptionProfile = encryptionProfileResponse.isDefault();
        tlsVersionsSpaceSeparated = encryptionProfileProvider.getTlsVersions(userTlsVersions, " ");
        tlsCipherSuites = encryptionProfileProvider
                .getOpenSslCipherSuites(userEncryptionProfileMap, DEFAULT, false, userTlsVersions, defaultEncryptionProfile);
        tlsCipherSuitesRedHat8 = encryptionProfileProvider
                .getOpenSslCipherSuites(userEncryptionProfileMap, REDHAT_VERSION8, false, userTlsVersions, defaultEncryptionProfile);
        tls12CipherSuites = encryptionProfileProvider
                .getDefaultRecommendedTls12CipherSuites(false);
        tls13CipherSuites = encryptionProfileProvider
                .getTls13CipherSuites(userEncryptionProfileMap, userTlsVersions);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("tlsVersionsSpaceSeparated", tlsVersionsSpaceSeparated);
        result.put("tlsCipherSuites", tlsCipherSuites);
        result.put("tlsCipherSuitesRedHat8", tlsCipherSuitesRedHat8);
        result.put("tls12CipherSuites", tls12CipherSuites);
        result.put("tls13CipherSuites", tls13CipherSuites);
        return result;
    }
}
