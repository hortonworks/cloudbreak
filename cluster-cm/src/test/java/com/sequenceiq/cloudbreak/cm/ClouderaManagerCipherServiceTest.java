package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.JAVA_EXCLUDE_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.POLICY_SEPARATOR;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHERS_LIST_OPENSSL;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHER_SUITE;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHER_SUITE_JAVA;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHER_SUITE_JAVA_EXCLUDE;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.sequenceiq.cloudbreak.tls.CipherSuite;
import com.sequenceiq.cloudbreak.tls.CipherSuiteProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerCipherServiceTest {

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @InjectMocks
    private ClouderaManagerCipherService underTest;

    private final CipherSuiteProvider cipherSuiteProvider = new CipherSuiteProvider();

    private final EncryptionProfileProvider testData = new EncryptionProfileProvider(cipherSuiteProvider);

    @Test
    void testGetApiConfigEnforcements() {
        when(encryptionProfileProvider.getAllowedCipherSuites())
                .thenReturn(testData.getAllowedCipherSuites());

        List<ApiConfigEnforcement> result = underTest.getApiConfigEnforcements();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(ApiConfigEnforcement::getLabel)
                .hasSameElementsAs(List.of(TLS_CIPHER_SUITE, TLS_CIPHER_SUITE_JAVA, TLS_CIPHER_SUITE_JAVA_EXCLUDE, TLS_CIPHERS_LIST_OPENSSL));
        assertThat(result).filteredOn(enforcement -> TLS_CIPHER_SUITE.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(INTERMEDIATE2018);
        assertThat(result).filteredOn(enforcement -> TLS_CIPHER_SUITE_JAVA.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(getExpectedCiphers(true));
        assertThat(result).filteredOn(enforcement -> TLS_CIPHER_SUITE_JAVA_EXCLUDE.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(JAVA_EXCLUDE_INTERMEDIATE2018);
        assertThat(result).filteredOn(enforcement -> TLS_CIPHERS_LIST_OPENSSL.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(getExpectedCiphers(false));
        assertThat(result).filteredOn(enforcement ->
                List.of(TLS_CIPHER_SUITE_JAVA, TLS_CIPHER_SUITE_JAVA_EXCLUDE, TLS_CIPHERS_LIST_OPENSSL).contains(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getSeparator)
                .allMatch(POLICY_SEPARATOR::equals);
    }

    private String getExpectedCiphers(boolean useIanaName) {
        List<CipherSuite> cipherSuites =
                List.of(
                        TLS_AES_256_GCM_SHA384,
                        TLS_CHACHA20_POLY1305_SHA256,
                        TLS_AES_128_GCM_SHA256,
                        TLS_AES_128_CCM_8_SHA256,
                        TLS_AES_128_CCM_SHA256,
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
                        TLS_RSA_WITH_AES_256_CBC_SHA);

        return cipherSuites.stream()
                .map(c -> useIanaName ? c.getIanaName() : c.getOpenSslName())
                .collect(Collectors.joining(","));
    }
}
