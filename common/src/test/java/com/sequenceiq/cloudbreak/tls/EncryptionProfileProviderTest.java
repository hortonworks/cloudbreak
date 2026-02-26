package com.sequenceiq.cloudbreak.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileProviderTest {

    @Spy
    private CipherSuiteProvider cipherSuiteProvider = new CipherSuiteProvider();

    @InjectMocks
    private EncryptionProfileProvider underTest;

    @Test
    public void testGetCipherSuiteString() {

        String assertValue = underTest.getCipherSuiteString(CipherSuitesLimitType.BLACKBOX_EXPORTER, ",", true);
        List<String> response = Arrays.stream(assertValue.split(",")).toList();
        assertThat(response).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");

        assertValue = underTest.getCipherSuiteString(CipherSuitesLimitType.REDHAT_VERSION8, ",", true);
        response = Arrays.stream(assertValue.split(",")).toList();
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

        assertValue = underTest.getCipherSuiteString(CipherSuitesLimitType.MINIMAL, ",", true);
        response = Arrays.stream(assertValue.split(",")).toList();
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

        assertValue = underTest.getCipherSuiteString(CipherSuitesLimitType.JAVA_INTERMEDIATE2018, ",", true);
        response = Arrays.stream(assertValue.split(",")).toList();
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

        assertValue = underTest.getCipherSuiteString(CipherSuitesLimitType.JAVA_INTERMEDIATE2018, ",", false);
        response = Arrays.stream(assertValue.split(",")).toList();
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

        assertValue = underTest.getCipherSuiteString(CipherSuitesLimitType.DEFAULT, ",", true);
        response = Arrays.stream(assertValue.split(",")).toList();
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
    public void testGetTlsCipherSuitesIanaList() {
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

        List<String> cipherSuites = underTest.getTlsCipherSuitesIanaList(suites, CipherSuitesLimitType.BLACKBOX_EXPORTER);
        assertThat(cipherSuites).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    }

    @Test
    public void testGetTlsCipherSuitesBlackboxExporter() {
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

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.BLACKBOX_EXPORTER, ":", false);
        List<String> result = Arrays.asList(assertValue.split(":"));
        assertThat(result).containsExactly("ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384");
    }

    @Test
    public void testGetTlsCipherSuitesDefault() {
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
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256"));

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.DEFAULT, ":", false);

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
    public void testGetTlsCipherSuitesDefaultInvalidIncluded() {
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

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.DEFAULT, ":", false);

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
    public void testGetTlsCipherSuitesRepeatedCipherSuitesShouldBeRemoved() {
        Map<String, List<String>> suites = Map.of(TlsVersion.TLS_1_2.getVersion(),
                List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
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

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.DEFAULT, ":", false);

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
    public void testGetTlsCipherSuitesMinimal() {
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
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256"));

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.MINIMAL, ":", false);

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
    public void testGetTlsCipherSuitesRedhat8() {
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

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.REDHAT_VERSION8, ":", false);

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
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256",
                "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384",
                "TLS_ECCPWD_WITH_AES_128_GCM_SHA256",
                "TLS_ECCPWD_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256",
                "TLS_ECCPWD_WITH_AES_256_CCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CCM",
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
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
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
                "TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256");
        assertThat(result.get(TlsVersion.TLS_1_3.getVersion())).containsExactly(
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256",
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_128_CCM_8_SHA256",
                "TLS_AES_128_CCM_SHA256");
    }

    @Test
    public void testConvertCipherSuitesToIana() {
        List<String> result = underTest.convertCipherSuitesToIana(
                List.of(
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
                        "AES256-SHA"
                ));

        assertThat(result).containsExactly(
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
    }

    @Test
    public void testConvertCipherSuitesToIanaDoesNotIgnoreNotAllowed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                underTest.convertCipherSuitesToIana(List.of(
                        "ECDHE-ECDSA-AES128-GCM-SHA256",
                        "NOT_ALLOWED"
                )));

        assertEquals("The following cipher(s) are not allowed: [NOT_ALLOWED]", ex.getMessage());
    }

    @Test
    public void testGetTlsCipherSuitesIanaListEnsureOrdering() {
        List<String> assertValue = underTest.getTlsCipherSuitesIanaList(List.of(),
                CipherSuitesLimitType.BLACKBOX_EXPORTER);

        assertThat(assertValue).containsExactly(
                cipherSuiteProvider.getCipherSuitesByLimitType(CipherSuitesLimitType.BLACKBOX_EXPORTER)
                        .stream()
                        .map(CipherSuite::getIanaName)
                        .toArray(String[]::new));
    }

    @Test
    public void testGetTlsCipherSuitesIanaRemoveDuplicates() {
        List<String> suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");

        List<String> assertValue = underTest.getTlsCipherSuitesIanaList(suites,
                CipherSuitesLimitType.JAVA_INTERMEDIATE2018);

        assertThat(assertValue).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
    }

    @Test
    public void testGetDefaultRecommendedTl12CipherSuitesWithIanaNames() {
        String assertValue = underTest.getDefaultRecommendedTls12CipherSuites(true);

        assertThat(assertValue).isEqualTo(
                """
                            TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256:
                            TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:
                            TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
                            TLS_ECCPWD_WITH_AES_128_GCM_SHA256:
                            TLS_ECCPWD_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256:
                            TLS_ECCPWD_WITH_AES_256_CCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8:
                            TLS_ECDHE_ECDSA_WITH_AES_256_CCM:
                            TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8:
                            TLS_ECDHE_ECDSA_WITH_AES_128_CCM:
                            TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
                            TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                            TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384:
                            TLS_ECCPWD_WITH_AES_128_CCM_SHA256:
                            TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256:
                            TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetDefaultRecommendedTl12CipherSuitesWithOpenSslNames() {
        String assertValue = underTest.getDefaultRecommendedTls12CipherSuites(false);

        assertThat(assertValue).isEqualTo(
                """
                            ECDHE-ECDSA-AES128-GCM-SHA256:
                            ECDHE-PSK-CHACHA20-POLY1305:
                            ECDHE-PSK-AES256-GCM-SHA384:
                            ECDHE-ECDSA-CAMELLIA128-GCM-SHA256:
                            ECDHE-ECDSA-ARIA256-GCM-SHA384:
                            ECDHE-ECDSA-ARIA128-GCM-SHA256:
                            ECDHE-ECDSA-AES256-GCM-SHA384:
                            ECDHE-PSK-AES128-GCM-SHA256:
                            ECDHE-ECDSA-CHACHA20-POLY1305:
                            ECDHE-ECDSA-CAMELLIA256-GCM-SHA384:
                            ECCPWD-AES128-GCM-SHA256:
                            ECCPWD-AES256-GCM-SHA384:
                            ECDHE-RSA-ARIA128-GCM-SHA256:
                            ECCPWD-AES256-CCM-SHA384:
                            ECDHE-ECDSA-AES256-CCM-8:
                            ECDHE-ECDSA-AES256-CCM:
                            ECDHE-RSA-AES256-GCM-SHA384:
                            ECDHE-RSA-AES128-GCM-SHA256:
                            ECDHE-ECDSA-AES128-CCM-8:
                            ECDHE-ECDSA-AES128-CCM:
                            ECDHE-RSA-CHACHA20-POLY1305:
                            ECDHE-RSA-CAMELLIA256-GCM-SHA384:
                            ECDHE-RSA-CAMELLIA128-GCM-SHA256:
                            ECDHE-RSA-ARIA256-GCM-SHA384:
                            ECCPWD-AES128-CCM-SHA256:
                            ECDHE-PSK-AES128-CCM-SHA256:
                            ECDHE-PSK-AES128-CCM-8-SHA256
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetTls13CipherSuitesWithEmptyMapShouldReturnAllTls13CipherSuites() {
        String assertValue = underTest.getTls13CipherSuites(null, Set.of("TLSv1.3"));

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
        String assertValue = underTest.getTls13CipherSuites(userEncryptionProfileMap, Set.of("TLSv1.3"));

        assertThat(assertValue).isEqualTo("TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256");
    }

    @Test
    public void testGetIanaCipherSuitesShouldReturnAllRecommendedCipherSuitesWhenCustomMapIsNullAndAddTls13IsUsed() {
        String assertValue = underTest.getIanaCipherSuites(null, CipherSuitesLimitType.MINIMAL, true,
                Set.of("TLSv1.2", "TLSv1.3"), false);

        assertThat(assertValue).isEqualTo(
                """
                            TLS_AES_256_GCM_SHA384:
                            TLS_CHACHA20_POLY1305_SHA256:
                            TLS_AES_128_GCM_SHA256:
                            TLS_AES_128_CCM_8_SHA256:
                            TLS_AES_128_CCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256:
                            TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:
                            TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
                            TLS_ECCPWD_WITH_AES_128_GCM_SHA256:
                            TLS_ECCPWD_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256:
                            TLS_ECCPWD_WITH_AES_256_CCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8:
                            TLS_ECDHE_ECDSA_WITH_AES_256_CCM:
                            TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8:
                            TLS_ECDHE_ECDSA_WITH_AES_128_CCM:
                            TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
                            TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                            TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384:
                            TLS_ECCPWD_WITH_AES_128_CCM_SHA256:
                            TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256:
                            TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetOpenSslCipherSuitesShouldReturnAllRecommendedCipherSuitesWhenCustomMapIsNullAndAddTls13IsUsed() {
        String assertValue = underTest.getOpenSslCipherSuites(null, CipherSuitesLimitType.MINIMAL, true,
                Set.of("TLSv1.2", "TLSv1.3"), false);

        assertThat(assertValue).isEqualTo(
                """
                            TLS_AES_256_GCM_SHA384:
                            TLS_CHACHA20_POLY1305_SHA256:
                            TLS_AES_128_GCM_SHA256:
                            TLS_AES_128_CCM_8_SHA256:
                            TLS_AES_128_CCM_SHA256:
                            ECDHE-ECDSA-AES128-GCM-SHA256:
                            ECDHE-PSK-CHACHA20-POLY1305:
                            ECDHE-PSK-AES256-GCM-SHA384:
                            ECDHE-ECDSA-CAMELLIA128-GCM-SHA256:
                            ECDHE-ECDSA-ARIA256-GCM-SHA384:
                            ECDHE-ECDSA-ARIA128-GCM-SHA256:
                            ECDHE-ECDSA-AES256-GCM-SHA384:
                            ECDHE-PSK-AES128-GCM-SHA256:
                            ECDHE-ECDSA-CHACHA20-POLY1305:
                            ECDHE-ECDSA-CAMELLIA256-GCM-SHA384:
                            ECCPWD-AES128-GCM-SHA256:
                            ECCPWD-AES256-GCM-SHA384:
                            ECDHE-RSA-ARIA128-GCM-SHA256:
                            ECCPWD-AES256-CCM-SHA384:
                            ECDHE-ECDSA-AES256-CCM-8:
                            ECDHE-ECDSA-AES256-CCM:
                            ECDHE-RSA-AES256-GCM-SHA384:
                            ECDHE-RSA-AES128-GCM-SHA256:
                            ECDHE-ECDSA-AES128-CCM-8:
                            ECDHE-ECDSA-AES128-CCM:
                            ECDHE-RSA-CHACHA20-POLY1305:
                            ECDHE-RSA-CAMELLIA256-GCM-SHA384:
                            ECDHE-RSA-CAMELLIA128-GCM-SHA256:
                            ECDHE-RSA-ARIA256-GCM-SHA384:
                            ECCPWD-AES128-CCM-SHA256:
                            ECDHE-PSK-AES128-CCM-SHA256:
                            ECDHE-PSK-AES128-CCM-8-SHA256
                        """.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetIanaCipherSuitesWithCustomCipherMapsAndAddingTls13toTheResult() {
        Map<String, List<String>> userEncryptionProfileMap = Map.of("TLSv1.2", List.of("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                "TLSv1.3", List.of("TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));

        String assertValue = underTest.getIanaCipherSuites(userEncryptionProfileMap, CipherSuitesLimitType.MINIMAL, true,
                Set.of("TLSv1.2", "TLSv1.3"), false);

        assertThat(assertValue).isEqualTo("TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    }

    @Test
    public void testGetOpenSslCipherSuitesWithCustomCipherMapsAndWithoutAddingTls13toTheResult() {
        Map<String, List<String>> userEncryptionProfileMap = Map.of("TLSv1.2", List.of("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                "TLSv1.3", List.of("TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));

        String assertValue = underTest.getOpenSslCipherSuites(userEncryptionProfileMap, CipherSuitesLimitType.MINIMAL, false,
                Set.of("TLSv1.2", "TLSv1.3"), false);

        assertThat(assertValue).isEqualTo("ECDHE-RSA-AES256-GCM-SHA384");
    }

    @Test
    public void testGetIanaCipherSuitesWithDefaultEncryptionProfile() {
        String assertValue = underTest.getIanaCipherSuites(null, CipherSuitesLimitType.MINIMAL, true,
                Set.of("TLSv1.2"), true);

        assertThat(assertValue).isEqualTo(
                """
                            TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
                            TLS_DHE_RSA_WITH_AES_128_GCM_SHA256:
                            TLS_DHE_RSA_WITH_AES_256_GCM_SHA384:
                            TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:
                            TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:
                            TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:
                            TLS_DHE_RSA_WITH_AES_256_CBC_SHA256
                        """.replaceAll("\\s+", ""));
    }
}
