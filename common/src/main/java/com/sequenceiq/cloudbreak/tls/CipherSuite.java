package com.sequenceiq.cloudbreak.tls;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

public enum CipherSuite {

    TLS_AES_256_GCM_SHA384(
            "TLS_AES_256_GCM_SHA384",
            EnumSet.of(TlsVersion.TLS_1_3)
    ),
    TLS_CHACHA20_POLY1305_SHA256(
            "TLS_CHACHA20_POLY1305_SHA256",
            EnumSet.of(TlsVersion.TLS_1_3)
    ),
    TLS_AES_128_GCM_SHA256(
            "TLS_AES_128_GCM_SHA256",
            EnumSet.of(TlsVersion.TLS_1_3)
    ),
    TLS_AES_128_CCM_8_SHA256(
            "TLS_AES_128_CCM_8_SHA256",
            EnumSet.of(TlsVersion.TLS_1_3)
    ),
    TLS_AES_128_CCM_SHA256(
            "TLS_AES_128_CCM_SHA256",
            EnumSet.of(TlsVersion.TLS_1_3)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256(
            "ECDHE-ECDSA-AES128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384(
            "ECDHE-ECDSA-AES256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256(
            "ECDHE-PSK-CHACHA20-POLY1305",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384(
            "ECDHE-PSK-AES256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384(
            "ECDHE-RSA-AES256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256(
            "ECDHE-RSA-AES128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256(
            "ECDHE-ECDSA-CAMELLIA128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384(
            "ECDHE-ECDSA-ARIA256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256(
            "ECDHE-ECDSA-ARIA128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256(
            "ECDHE-PSK-AES128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256(
            "ECDHE-ECDSA-CHACHA20-POLY1305",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384(
            "ECDHE-ECDSA-CAMELLIA256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECCPWD_WITH_AES_128_GCM_SHA256(
            "ECCPWD-AES128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECCPWD_WITH_AES_256_GCM_SHA384(
            "ECCPWD-AES256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256(
            "ECDHE-RSA-ARIA128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECCPWD_WITH_AES_256_CCM_SHA384(
            "ECCPWD-AES256-CCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8(
            "ECDHE-ECDSA-AES256-CCM-8",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_256_CCM(
            "ECDHE-ECDSA-AES256-CCM",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8(
            "ECDHE-ECDSA-AES128-CCM-8",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_128_CCM(
            "ECDHE-ECDSA-AES128-CCM",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256(
            "ECDHE-RSA-CHACHA20-POLY1305",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384(
            "ECDHE-RSA-CAMELLIA256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256(
            "ECDHE-RSA-CAMELLIA128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384(
            "ECDHE-RSA-ARIA256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECCPWD_WITH_AES_128_CCM_SHA256(
            "ECCPWD-AES128-CCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256(
            "ECDHE-PSK-AES128-CCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256(
            "ECDHE-PSK-AES128-CCM-8-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256(
            "DHE-RSA-AES128-GCM-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_DHE_RSA_WITH_AES_256_GCM_SHA384(
            "DHE-RSA-AES256-GCM-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA(
            "ECDHE-ECDSA-AES128-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA(
            "ECDHE-ECDSA-AES256-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256(
            "ECDHE-ECDSA-AES128-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384(
            "ECDHE-ECDSA-AES256-SHA384",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256(
            "DHE-RSA-AES128-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256(
            "DHE-RSA-AES256-SHA256",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA(
            "ECDHE-RSA-AES128-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA(
            "ECDHE-RSA-AES256-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA(
            "DHE-RSA-AES128-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA(
            "DHE-RSA-AES256-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_RSA_WITH_AES_128_CBC_SHA(
            "AES128-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    ),
    TLS_RSA_WITH_AES_256_CBC_SHA(
            "AES256-SHA",
            EnumSet.of(TlsVersion.TLS_1_2)
    );

    private final String opensslName;

    private final EnumSet<TlsVersion> tlsVersions;

    CipherSuite(String opensslName, EnumSet<TlsVersion> tlsVersions) {
        this.opensslName = Objects.requireNonNull(opensslName, "opensslName");
        this.tlsVersions = Objects.requireNonNull(tlsVersions, "tlsVersions");
    }

    public static Optional<CipherSuite> fromIanaName(String name) {
        try {
            return Optional.of(CipherSuite.valueOf(name));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public String getOpenSslName() {
        return opensslName;
    }

    public String getIanaName() {
        return name();
    }

    public Set<TlsVersion> getTlsVersions() {
        return tlsVersions;
    }

    public boolean supports(TlsVersion version) {
        return tlsVersions.contains(version);
    }
}
