package com.sequenceiq.freeipa.service.freeipa.config;

import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.DEFAULT;
import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.REDHAT_VERSION8;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileConverter;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

public class FreeIpaEncryptionConfigView {

    private final String tlsVersionsSpaceSeparated;

    private final String tlsCipherSuites;

    private final String tlsCipherSuitesRedHat8;

    private final String tls12CipherSuites;

    private final String tls13CipherSuites;

    private final boolean hardenDirectoryServer;

    private final String dirsrvTlsMinVersion;

    private final String dirsrvTlsMaxVersion;

    private final String dirsrvCipherSuites;

    public FreeIpaEncryptionConfigView(EncryptionProfileProvider encryptionProfileProvider, EncryptionProfileResponse encryptionProfileResponse,
            boolean hardenDirectoryServer) {
        Set<String> userTlsVersions = encryptionProfileResponse.getTlsVersions();
        Map<String, List<String>> userEncryptionProfileMap = encryptionProfileResponse.getCipherSuites();
        boolean legacyEncryptionProfile = encryptionProfileResponse.isLegacy();
        this.hardenDirectoryServer = hardenDirectoryServer;
        tlsVersionsSpaceSeparated = EncryptionProfileConverter.getTlsVersionsSeparatedBySpace(userTlsVersions);
        tlsCipherSuites = encryptionProfileProvider.getOpenSslCipherSuites(userEncryptionProfileMap, DEFAULT, legacyEncryptionProfile);
        tlsCipherSuitesRedHat8 = encryptionProfileProvider.getOpenSslCipherSuites(userEncryptionProfileMap, REDHAT_VERSION8, legacyEncryptionProfile);
        tls12CipherSuites = encryptionProfileProvider.getDefaultTls12CipherSuites(false);
        tls13CipherSuites = encryptionProfileProvider.getTls13CipherSuites(userEncryptionProfileMap);
        dirsrvTlsMinVersion = resolveDirsrvMinTlsVersion(userTlsVersions);
        dirsrvTlsMaxVersion = resolveDirsrvMaxTlsVersion(userTlsVersions);
        dirsrvCipherSuites = encryptionProfileProvider.getDirectoryServerCipherSuites(userEncryptionProfileMap);
    }

    private String resolveDirsrvMinTlsVersion(Set<String> userTlsVersions) {
        if (userTlsVersions.contains(TlsVersion.TLS_1_2.getVersion())) {
            return DirectoryServerTlsVersion.TLS_1_2.getVersion();
        }
        return userTlsVersions.contains(TlsVersion.TLS_1_3.getVersion()) ? DirectoryServerTlsVersion.TLS_1_3.getVersion() : "";
    }

    private String resolveDirsrvMaxTlsVersion(Set<String> userTlsVersions) {
        if (userTlsVersions.contains(TlsVersion.TLS_1_3.getVersion())) {
            return DirectoryServerTlsVersion.TLS_1_3.getVersion();
        }
        return userTlsVersions.contains(TlsVersion.TLS_1_2.getVersion()) ? DirectoryServerTlsVersion.TLS_1_2.getVersion() : "";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("tlsVersionsSpaceSeparated", tlsVersionsSpaceSeparated);
        result.put("tlsCipherSuites", tlsCipherSuites);
        result.put("tlsCipherSuitesRedHat8", tlsCipherSuitesRedHat8);
        result.put("tls12CipherSuites", tls12CipherSuites);
        result.put("tls13CipherSuites", tls13CipherSuites);
        if (hardenDirectoryServer) {
            result.put("dirsrvTlsMinVersion", dirsrvTlsMinVersion);
            result.put("dirsrvTlsMaxVersion", dirsrvTlsMaxVersion);
            result.put("dirsrvCipherSuites", dirsrvCipherSuites);
        }
        return result;
    }
}
