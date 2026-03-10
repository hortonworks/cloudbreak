package com.sequenceiq.cloudbreak.tls;

import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_AES_128_CCM_8_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_AES_128_CCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_AES_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_CHACHA20_POLY1305_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECCPWD_WITH_AES_128_CCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECCPWD_WITH_AES_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECCPWD_WITH_AES_256_CCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECCPWD_WITH_AES_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA;
import static com.sequenceiq.cloudbreak.tls.CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CipherSuiteProvider {

    public List<CipherSuite> getLegacyCipherSuitesByLimitType(CipherSuitesLimitType cipherSuitesLimitType) {
        return switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL, CipherSuitesLimitType.REDHAT_VERSION8 ->
                    List.of(
                    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,
                    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,
                    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256);

            case CipherSuitesLimitType.BLACKBOX_EXPORTER ->
                    List.of(
                    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA);

            case CipherSuitesLimitType.JAVA_INTERMEDIATE2018 ->
                    List.of(
                    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,
                    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,
                    TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,
                    TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
                    TLS_RSA_WITH_AES_128_CBC_SHA,
                    TLS_RSA_WITH_AES_256_CBC_SHA);

            default -> List.of(
                    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                    TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,
                    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,
                    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA);
        };
    }

    public List<CipherSuite> getAllowedTls12CipherSuites() {
        return List.of(
                TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256,
                TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256,
                TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384,
                TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256,
                TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256,
                TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384,
                TLS_ECCPWD_WITH_AES_128_GCM_SHA256,
                TLS_ECCPWD_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256,
                TLS_ECCPWD_WITH_AES_256_CCM_SHA384,
                TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8,
                TLS_ECDHE_ECDSA_WITH_AES_256_CCM,
                TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8,
                TLS_ECDHE_ECDSA_WITH_AES_128_CCM,
                TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
                TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384,
                TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256,
                TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384,
                TLS_ECCPWD_WITH_AES_128_CCM_SHA256,
                TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256,
                TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256,
                TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,
                TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,
                TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,
                TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
                TLS_RSA_WITH_AES_128_CBC_SHA,
                TLS_RSA_WITH_AES_256_CBC_SHA
                );
    }

    public List<CipherSuite> getRecommendedTls12CipherSuites() {
        return List.of(
                TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384);
    }

    public List<CipherSuite> getAllowedTls13CipherSuites() {
        return List.of(
                TLS_AES_256_GCM_SHA384,
                TLS_CHACHA20_POLY1305_SHA256,
                TLS_AES_128_GCM_SHA256,
                TLS_AES_128_CCM_8_SHA256,
                TLS_AES_128_CCM_SHA256);
    }

    public List<CipherSuite> getRecommendedTls13CipherSuites() {
        return List.of(
                TLS_AES_128_GCM_SHA256,
                TLS_AES_256_GCM_SHA384);
    }
}