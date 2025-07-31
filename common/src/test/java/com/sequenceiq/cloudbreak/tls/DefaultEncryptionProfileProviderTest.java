package com.sequenceiq.cloudbreak.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tls.DefaultEncryptionProfileProvider.CipherSuitesLimitType;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@ExtendWith(MockitoExtension.class)
public class DefaultEncryptionProfileProviderTest {

    private DefaultEncryptionProfileProvider underTest = new DefaultEncryptionProfileProvider();

    @Test
    public void testgetCipherSuiteString() {

        String assertValue = underTest.getCipherSuiteString(DefaultEncryptionProfileProvider.CipherSuitesLimitType.BLACKBOX_EXPORTER, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", assertValue);

        assertValue = underTest.getCipherSuiteString(DefaultEncryptionProfileProvider.CipherSuitesLimitType.REDHAT_VERSION8, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", assertValue);

        assertValue = underTest.getCipherSuiteString(DefaultEncryptionProfileProvider.CipherSuitesLimitType.MINIMAL, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", assertValue);

        assertValue = underTest.getCipherSuiteString(DefaultEncryptionProfileProvider.CipherSuitesLimitType.JAVA_INTERMEDIATE2018, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA," +
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA," +
                "TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA", assertValue);

        assertValue = underTest.getCipherSuiteString(DefaultEncryptionProfileProvider.CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018, ",");
        assertEquals("ECDHE-ECDSA-AES128-GCM-SHA256,ECDHE-RSA-AES128-GCM-SHA256,ECDHE-ECDSA-AES256-GCM-SHA384,ECDHE-RSA-AES256-GCM-SHA384," +
                "DHE-RSA-AES128-GCM-SHA256,DHE-RSA-AES256-GCM-SHA384,ECDHE-ECDSA-AES128-SHA256,ECDHE-ECDSA-AES128-SHA,ECDHE-RSA-AES128-SHA," +
                "ECDHE-ECDSA-AES256-SHA384,ECDHE-ECDSA-AES256-SHA,ECDHE-RSA-AES256-SHA,DHE-RSA-AES128-SHA256,DHE-RSA-AES128-SHA,DHE-RSA-AES256-SHA256," +
                "DHE-RSA-AES256-SHA,AES128-SHA,AES256-SHA", assertValue);

        assertValue = underTest.getCipherSuiteString(DefaultEncryptionProfileProvider.CipherSuitesLimitType.DEFAULT, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,"  +
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", assertValue);
    }

    @Test
    public void testGetTlsCipherSuitesIanaList() {
        String[] suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256").toArray(new String[9]);

        List<String> cipherSuites = underTest.getTlsCipherSuitesIanaList(suites, CipherSuitesLimitType.BLACKBOX_EXPORTER);
        assertTrue(cipherSuites.containsAll(List.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384")));
    }

    @Test
    public void testGetTlsCipherSuitesBlackboxExporter() {
        String[] suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256").toArray(new String[9]);

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.BLACKBOX_EXPORTER, ":");
        List<String> result = Arrays.asList(assertValue.split(":"));
        assertTrue(result.containsAll(List.of("ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384")));
    }

    @Test
    public void testGetTlsCipherSuitesDefault() {
        String[] suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256").toArray(new String[10]);

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.DEFAULT, ":");

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertTrue(result.containsAll(List.of("ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384")));
    }

    @Test
    public void testGetTlsCipherSuitesDefaultInvalidIncluded() {
        String[] suites = List.of(
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
                "TLS Invalid Suite").toArray(new String[10]);

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.DEFAULT, ":");

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertTrue(result.containsAll(List.of("DHE-RSA-AES128-GCM-SHA256", "DHE-RSA-AES256-GCM-SHA384", "DHE-RSA-AES256-SHA256",
                "ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-ECDSA-AES128-SHA256", "ECDHE-ECDSA-AES256-SHA384", "ECDHE-RSA-AES128-GCM-SHA256",
                "ECDHE-RSA-AES256-GCM-SHA384")));
    }

    @Test
    public void testGetTlsCipherSuitesDefaultRepeatedInput() {
        String[] suites = List.of(
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
                "TLS Invalid Suite").toArray(new String[11]);

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.DEFAULT, ":");

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertTrue(result.containsAll(List.of("DHE-RSA-AES128-GCM-SHA256", "DHE-RSA-AES128-SHA256", "DHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES256-SHA256", "ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-ECDSA-AES128-SHA256", "ECDHE-ECDSA-AES256-SHA384",
                "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384")));
    }

    @Test
    public void testGetTlsCipherSuitesMinimal() {
        String[] suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256").toArray(new String[10]);

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.MINIMAL, ":");

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertTrue(result.containsAll(List.of("DHE-RSA-AES128-GCM-SHA256", "DHE-RSA-AES128-SHA256", "DHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES256-SHA256", "ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-ECDSA-AES128-SHA256", "ECDHE-ECDSA-AES256-SHA384",
                "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384")));
    }

    @Test
    public void testGetTlsCipherSuitesRedhat8() {
        String[] suites = List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256").toArray(new String[9]);

        String assertValue = underTest.getTlsCipherSuites(suites, CipherSuitesLimitType.REDHAT_VERSION8, ":");

        List<String> result = Arrays.asList(assertValue.split(":"));
        assertTrue(result.containsAll(List.of("DHE-RSA-AES128-GCM-SHA256", "DHE-RSA-AES128-SHA256", "DHE-RSA-AES256-GCM-SHA384",
                "DHE-RSA-AES256-SHA256", "ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-ECDSA-AES128-SHA256", "ECDHE-ECDSA-AES256-SHA384",
                "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384")));
    }

    @Test
    public void testGetAllCipherSuitesAvailableByTlsVersion() {
        Map<String, Set<String>> result = underTest.getAllCipherSuitesAvailableByTlsVersion();

        assertTrue(result.containsKey(TlsVersion.TLS_1_2.getVersion()));
        assertTrue(result.containsKey(TlsVersion.TLS_1_3.getVersion()));
        assertTrue(result.get(TlsVersion.TLS_1_2.getVersion()).containsAll(List.of("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384")));
        assertTrue(result.get(TlsVersion.TLS_1_3.getVersion()).containsAll(List.of("TLS_AES_128_CCM_SHA256", "TLS_AES_128_CCM_8_SHA256")));
    }

    @Test
    public void testGetRecommendedCipherSuites() {
        Map<String, Set<String>> result = underTest.getRecommendedCipherSuites();

        assertTrue(result.containsKey(TlsVersion.TLS_1_2.getVersion()));
        assertTrue(result.containsKey(TlsVersion.TLS_1_3.getVersion()));
        assertTrue(result.get(TlsVersion.TLS_1_2.getVersion()).containsAll(List.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384",
                "TLS_ECCPWD_WITH_AES_128_GCM_SHA256", "TLS_ECCPWD_WITH_AES_256_GCM_SHA384")));
        assertTrue(result.get(TlsVersion.TLS_1_3.getVersion()).containsAll(List.of("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", "TLS_ECCPWD_WITH_AES_128_GCM_SHA256", "TLS_ECCPWD_WITH_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256")));
    }

    @Test
    public void testConvertCipherSuitesToIana() {
        Set<String> result = underTest.convertCipherSuitesToIana(
                Set.of(
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

        assertTrue(result.containsAll(
                Set.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
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
                        "TLS_RSA_WITH_AES_256_CBC_SHA")));
    }

    @Test
    public void testConvertCipherSuitesToIanaDoesNotIgnoreNotAllowed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> underTest.convertCipherSuitesToIana(
                Set.of(
                        "ECDHE-ECDSA-AES128-GCM-SHA256",
                        "NOT_ALLOWED"
                )));

        assertEquals("The following cipher(s) are not allowed: [NOT_ALLOWED]", ex.getMessage());
    }
}
