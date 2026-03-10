package com.sequenceiq.cloudbreak.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileProviderTest {

    @Spy
    private CipherSuiteProvider cipherSuiteProvider = new CipherSuiteProvider();

    @InjectMocks
    private EncryptionProfileProvider underTest;

    @Test
    public void testGetIanaCipherSuites() {
        Map<String, List<String>> cipherSuites = createLegacyCipherSuitesMap(CipherSuitesLimitType.BLACKBOX_EXPORTER);

        String assertValue = underTest.getIanaCipherSuites(cipherSuites, CipherSuitesLimitType.BLACKBOX_EXPORTER, true);

        List<String> response = Arrays.stream(assertValue.split(":")).toList();
        assertThat(response).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");

        cipherSuites = createLegacyCipherSuitesMap(CipherSuitesLimitType.REDHAT_VERSION8);

        assertValue = underTest.getIanaCipherSuites(cipherSuites, CipherSuitesLimitType.REDHAT_VERSION8, true);

        response = Arrays.stream(assertValue.split(":")).toList();
        assertThat(response).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");

        cipherSuites = createLegacyCipherSuitesMap(CipherSuitesLimitType.MINIMAL);

        assertValue = underTest.getIanaCipherSuites(cipherSuites, CipherSuitesLimitType.MINIMAL, true);

        response = Arrays.stream(assertValue.split(":")).toList();
        assertThat(response).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");

        cipherSuites = createLegacyCipherSuitesMap(CipherSuitesLimitType.JAVA_INTERMEDIATE2018);

        assertValue = underTest.getIanaCipherSuites(cipherSuites, CipherSuitesLimitType.JAVA_INTERMEDIATE2018, true);

        response = Arrays.stream(assertValue.split(":")).toList();
        assertThat(response).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA");

        cipherSuites = createLegacyCipherSuitesMap(CipherSuitesLimitType.JAVA_INTERMEDIATE2018);

        assertValue = underTest.getOpenSslCipherSuites(cipherSuites, CipherSuitesLimitType.JAVA_INTERMEDIATE2018, true);

        response = Arrays.stream(assertValue.split(":")).toList();
        assertThat(response).containsExactly(
                "ECDHE-ECDSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES128-GCM-SHA256",
                "ECDHE-ECDSA-AES256-GCM-SHA384",
                "ECDHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES128-GCM-SHA256",
                "DHE-RSA-AES256-GCM-SHA384",
                "ECDHE-ECDSA-AES128-SHA256",
                "ECDHE-ECDSA-AES128-SHA",
                "ECDHE-RSA-AES128-SHA",
                "ECDHE-ECDSA-AES256-SHA384",
                "ECDHE-ECDSA-AES256-SHA",
                "ECDHE-RSA-AES256-SHA",
                "DHE-RSA-AES128-SHA256",
                "DHE-RSA-AES128-SHA",
                "DHE-RSA-AES256-SHA256",
                "DHE-RSA-AES256-SHA",
                "AES128-SHA",
                "AES256-SHA");

        cipherSuites = createLegacyCipherSuitesMap(CipherSuitesLimitType.DEFAULT);

        assertValue = underTest.getIanaCipherSuites(cipherSuites, CipherSuitesLimitType.DEFAULT, true);

        response = Arrays.stream(assertValue.split(":")).toList();
        assertThat(response).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    @Test
    public void testGetOpenSslCipherSuitesBlackboxExporter() {
        List<String> suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
        Map<String, List<String>> cipherSuitesMap = Map.of(TlsVersion.TLS_1_2.getVersion(), suites);

        String assertValue = underTest.getOpenSslCipherSuites(cipherSuitesMap, CipherSuitesLimitType.BLACKBOX_EXPORTER, true);

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertThat(result).containsExactly("ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384");
    }

    @Test
    public void testGetOpenSslCipherSuitesDefault() {
        Map<String, List<String>> suites = Map.of(TlsVersion.TLS_1_2.getVersion(),
                List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"));

        String assertValue = underTest.getOpenSslCipherSuites(suites, CipherSuitesLimitType.DEFAULT, true);

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertThat(result).containsExactly(
                "ECDHE-ECDSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES128-GCM-SHA256",
                "DHE-RSA-AES256-GCM-SHA384",
                "ECDHE-ECDSA-AES128-SHA256",
                "ECDHE-ECDSA-AES256-SHA384",
                "DHE-RSA-AES128-SHA256",
                "DHE-RSA-AES256-SHA256");

    }

    @Test
    public void testGetOpenSslCipherSuitesWithInvalidCipherIncluded() {
        Map<String, List<String>> suites = Map.of(TlsVersion.TLS_1_2.getVersion(),
                List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256",
                "TLS Invalid Suite"));

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () ->
                underTest.getOpenSslCipherSuites(suites, CipherSuitesLimitType.DEFAULT, true));

        assertEquals("Failed to convert to CipherSuite.", ex.getMessage());
    }

    @Test
    public void testGetOpenSslCipherSuitesMinimal() {
        Map<String, List<String>> suites = Map.of(TlsVersion.TLS_1_2.getVersion(),
                List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"));

        String assertValue = underTest.getOpenSslCipherSuites(suites, CipherSuitesLimitType.DEFAULT, true);

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertThat(result).containsExactly(
                "ECDHE-ECDSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES128-GCM-SHA256",
                "DHE-RSA-AES256-GCM-SHA384",
                "ECDHE-ECDSA-AES128-SHA256",
                "ECDHE-ECDSA-AES256-SHA384",
                "DHE-RSA-AES128-SHA256",
                "DHE-RSA-AES256-SHA256");
    }

    @Test
    public void testGetOpenSslCipherSuitesRedhat8() {
        Map<String, List<String>> suites = Map.of(TlsVersion.TLS_1_2.getVersion(),
                List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"));

        String assertValue = underTest.getOpenSslCipherSuites(suites, CipherSuitesLimitType.DEFAULT, true);

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertThat(result).containsExactly(
                "ECDHE-ECDSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES128-GCM-SHA256",
                "DHE-RSA-AES256-GCM-SHA384",
                "ECDHE-ECDSA-AES128-SHA256",
                "ECDHE-ECDSA-AES256-SHA384",
                "DHE-RSA-AES128-SHA256",
                "DHE-RSA-AES256-SHA256"
        );
    }

    @Test
    public void testGetAllCipherSuitesAvailableByTlsVersion() {
        Map<String, List<String>> result = underTest.getAllCipherSuitesAvailableByTlsVersion();

        assertTrue(result.containsKey(TlsVersion.TLS_1_2.getVersion()));
        assertTrue(result.containsKey(TlsVersion.TLS_1_3.getVersion()));
        assertThat(result.get(TlsVersion.TLS_1_2.getVersion())).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384",
                "TLS_ECCPWD_WITH_AES_128_GCM_SHA256",
                "TLS_ECCPWD_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256",
                "TLS_ECCPWD_WITH_AES_256_CCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CCM",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CCM",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384",
                "TLS_ECCPWD_WITH_AES_128_CCM_SHA256",
                "TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256",
                "TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA");
        assertThat(result.get(TlsVersion.TLS_1_3.getVersion())).containsExactly(
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256",
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_128_CCM_8_SHA256",
                "TLS_AES_128_CCM_SHA256");
    }

    @Test
    public void testGetRecommendedCipherSuites() {
        Map<String, List<String>> result = underTest.getRecommendedCipherSuites();

        assertTrue(result.containsKey(TlsVersion.TLS_1_2.getVersion()));
        assertTrue(result.containsKey(TlsVersion.TLS_1_3.getVersion()));
        assertThat(result.get(TlsVersion.TLS_1_2.getVersion())).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        assertThat(result.get(TlsVersion.TLS_1_3.getVersion())).containsExactly(
                "TLS_AES_128_GCM_SHA256",
                        "TLS_AES_256_GCM_SHA384");
    }

    @Test
    public void testGetDefaultTl12CipherSuitesWithIanaNames() {
        String assertValue = underTest.getDefaultTls12CipherSuites(true);

        assertThat(assertValue).isEqualTo(
                """
                            TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetDefaultTl12CipherSuitesWithOpenSslNames() {
        String assertValue = underTest.getDefaultTls12CipherSuites(false);

        assertThat(assertValue).isEqualTo(
                """
                            ECDHE-ECDSA-AES128-GCM-SHA256:
                            ECDHE-RSA-AES128-GCM-SHA256:
                            ECDHE-ECDSA-AES256-GCM-SHA384:
                            ECDHE-RSA-AES256-GCM-SHA384
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetTls13CipherSuites() {
        String assertValue = underTest.getTls13CipherSuites(
                Map.of(TlsVersion.TLS_1_3.getVersion(), EncryptionProfileConverter.toListString(cipherSuiteProvider.getAllowedTls13CipherSuites()))
        );

        assertThat(assertValue).isEqualTo(
                """
                            TLS_AES_256_GCM_SHA384:
                            TLS_CHACHA20_POLY1305_SHA256:
                            TLS_AES_128_GCM_SHA256:
                            TLS_AES_128_CCM_8_SHA256:
                            TLS_AES_128_CCM_SHA256
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetTls13CipherSuitesWithCustomCipherSuitesConfigured() {
        Map<String, List<String>> userEncryptionProfileMap = Map.of("TLSv1.3", List.of("TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));
        String assertValue = underTest.getTls13CipherSuites(userEncryptionProfileMap);

        assertThat(assertValue).isEqualTo("TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256");
    }

    @Test
    public void testGetIanaCipherSuitesWithCustomCipherMapsAndAddingTls13toTheResult() {
        Map<String, List<String>> userEncryptionProfileMap = Map.of("TLSv1.2", List.of("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                "TLSv1.3", List.of("TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));

        String assertValue = underTest.getIanaCipherSuites(userEncryptionProfileMap, CipherSuitesLimitType.MINIMAL, false);

        assertThat(assertValue).isEqualTo("TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    }

    @Test
    public void testGetOpenSslCipherSuitesWithCustomCipherMapsAndWithoutAddingTls13toTheResult() {
        Map<String, List<String>> userEncryptionProfileMap = Map.of("TLSv1.2", List.of("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                "TLSv1.3", List.of("TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));

        String assertValue = underTest.getOpenSslCipherSuites(userEncryptionProfileMap, CipherSuitesLimitType.MINIMAL, false);

        assertThat(assertValue).isEqualTo("ECDHE-RSA-AES256-GCM-SHA384");
    }

    private Map<String, List<String>> createLegacyCipherSuitesMap(CipherSuitesLimitType cipherSuitesLimitType) {
        return Map.of(TlsVersion.TLS_1_2.getVersion(),
                EncryptionProfileConverter.toListString(cipherSuiteProvider.getLegacyCipherSuitesByLimitType(cipherSuitesLimitType)));
    }
}
