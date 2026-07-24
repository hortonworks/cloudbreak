package com.sequenceiq.freeipa.service.freeipa.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.tls.CipherSuiteProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

class FreeIpaEncryptionConfigViewTest {

    private final EncryptionProfileProvider encryptionProfileProvider = new EncryptionProfileProvider(new CipherSuiteProvider());

    @Test
    void tls13OnlyProfilePinsDirectoryServerToTls13AndApprovedCiphers() {
        EncryptionProfileResponse profile = profile("cdp_default_tls13_fips_140_3",
                Set.of(TlsVersion.TLS_1_3.getVersion()),
                Map.of(TlsVersion.TLS_1_3.getVersion(), List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")));

        Map<String, Object> result = new FreeIpaEncryptionConfigView(encryptionProfileProvider, profile).toMap();

        assertThat(result.get("dirsrvTlsMinVersion")).isEqualTo("TLS1.3");
        assertThat(result.get("dirsrvTlsMaxVersion")).isEqualTo("TLS1.3");
        assertThat(result.get("dirsrvCipherSuites")).isEqualTo("TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384");
    }

    @Test
    void tls12AndTls13ProfileAllowsBothVersions() {
        EncryptionProfileResponse profile = profile("custom",
                Set.of(TlsVersion.TLS_1_2.getVersion(), TlsVersion.TLS_1_3.getVersion()),
                Map.of(
                        TlsVersion.TLS_1_2.getVersion(), List.of("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                        TlsVersion.TLS_1_3.getVersion(), List.of("TLS_AES_128_GCM_SHA256")));

        Map<String, Object> result = new FreeIpaEncryptionConfigView(encryptionProfileProvider, profile).toMap();

        assertThat(result.get("dirsrvTlsMinVersion")).isEqualTo("TLS1.2");
        assertThat(result.get("dirsrvTlsMaxVersion")).isEqualTo("TLS1.3");
        assertThat(result.get("dirsrvCipherSuites")).isEqualTo("TLS_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    }

    @Test
    void tls12OnlyProfileKeepsDirectoryServerOnTls12() {
        EncryptionProfileResponse profile = profile("custom-tls12",
                Set.of(TlsVersion.TLS_1_2.getVersion()),
                Map.of(TlsVersion.TLS_1_2.getVersion(), List.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")));

        Map<String, Object> result = new FreeIpaEncryptionConfigView(encryptionProfileProvider, profile).toMap();

        assertThat(result.get("dirsrvTlsMinVersion")).isEqualTo("TLS1.2");
        assertThat(result.get("dirsrvTlsMaxVersion")).isEqualTo("TLS1.2");
        assertThat(result.get("dirsrvCipherSuites")).isEqualTo("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
    }

    @Test
    void emptyTlsVersionsYieldEmptyDirsrvValuesSoTheSaltStateIsSkipped() {
        EncryptionProfileResponse profile = profile("empty", Set.of(), Map.of());

        Map<String, Object> result = new FreeIpaEncryptionConfigView(encryptionProfileProvider, profile).toMap();

        // With no TLS versions the min/max and cipher pillar values are empty, and ssl-ciphers.sls
        // guards (`{% if tlsMin and tlsMax %}` / `{% if ciphers %}`) skip the dirsrv hardening entirely.
        assertThat(result.get("dirsrvTlsMinVersion")).isEqualTo("");
        assertThat(result.get("dirsrvTlsMaxVersion")).isEqualTo("");
        assertThat(result.get("dirsrvCipherSuites")).isEqualTo("");
    }

    private EncryptionProfileResponse profile(String name, Set<String> tlsVersions, Map<String, List<String>> cipherSuites) {
        EncryptionProfileResponse response = new EncryptionProfileResponse();
        response.setName(name);
        response.setTlsVersions(tlsVersions);
        response.setCipherSuites(cipherSuites);
        return response;
    }
}
