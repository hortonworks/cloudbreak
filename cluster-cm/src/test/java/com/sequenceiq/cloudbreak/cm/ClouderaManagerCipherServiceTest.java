package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.JAVA_EXCLUDE_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.POLICY_SEPARATOR;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHERS_LIST_OPENSSL;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHER_SUITE;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHER_SUITE_JAVA;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerCipherService.TLS_CIPHER_SUITE_JAVA_EXCLUDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.sequenceiq.cloudbreak.tls.TlsSpecificationsHelper;

class ClouderaManagerCipherServiceTest {

    private final ClouderaManagerCipherService underTest = new ClouderaManagerCipherService();

    @Test
    void testGetApiConfigEnforcements() {
        List<ApiConfigEnforcement> result = underTest.getApiConfigEnforcements();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(ApiConfigEnforcement::getLabel)
                .hasSameElementsAs(List.of(TLS_CIPHER_SUITE, TLS_CIPHER_SUITE_JAVA, TLS_CIPHER_SUITE_JAVA_EXCLUDE, TLS_CIPHERS_LIST_OPENSSL));
        assertThat(result).filteredOn(enforcement -> TLS_CIPHER_SUITE.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(INTERMEDIATE2018);
        assertThat(result).filteredOn(enforcement -> TLS_CIPHER_SUITE_JAVA.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(TlsSpecificationsHelper.getCipherSuiteString(
                        TlsSpecificationsHelper.CipherSuitesLimitType.JAVA_INTERMEDIATE2018, POLICY_SEPARATOR));
        assertThat(result).filteredOn(enforcement -> TLS_CIPHER_SUITE_JAVA_EXCLUDE.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(JAVA_EXCLUDE_INTERMEDIATE2018);
        assertThat(result).filteredOn(enforcement -> TLS_CIPHERS_LIST_OPENSSL.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly(
                        TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018, POLICY_SEPARATOR));
        assertThat(result).filteredOn(enforcement ->
                List.of(TLS_CIPHER_SUITE_JAVA, TLS_CIPHER_SUITE_JAVA_EXCLUDE, TLS_CIPHERS_LIST_OPENSSL).contains(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getSeparator)
                .allMatch(POLICY_SEPARATOR::equals);
    }
}
