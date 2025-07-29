package com.sequenceiq.cloudbreak.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TlsSpecificationsHelperTest {

    @Test
    public void testgetCipherSuiteString() {

        String assertValue = TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.BLACKBOX_EXPORTER, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", assertValue);

        assertValue = TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.REDHAT_VERSION8, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", assertValue);

        assertValue = TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.MINIMAL, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", assertValue);

        assertValue = TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.JAVA_INTERMEDIATE2018, ",");
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA," +
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA," +
                "TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA", assertValue);

        assertValue = TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018, ",");
        assertEquals("ECDHE-ECDSA-AES128-GCM-SHA256,ECDHE-RSA-AES128-GCM-SHA256,ECDHE-ECDSA-AES256-GCM-SHA384,ECDHE-RSA-AES256-GCM-SHA384," +
                "DHE-RSA-AES128-GCM-SHA256,DHE-RSA-AES256-GCM-SHA384,ECDHE-ECDSA-AES128-SHA256,ECDHE-ECDSA-AES128-SHA,ECDHE-RSA-AES128-SHA," +
                "ECDHE-ECDSA-AES256-SHA384,ECDHE-ECDSA-AES256-SHA,ECDHE-RSA-AES256-SHA,DHE-RSA-AES128-SHA256,DHE-RSA-AES128-SHA,DHE-RSA-AES256-SHA256," +
                "DHE-RSA-AES256-SHA,AES128-SHA,AES256-SHA", assertValue);

        assertValue = TlsSpecificationsHelper.getCipherSuiteString(TlsSpecificationsHelper.CipherSuitesLimitType.DEFAULT, ",");
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
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256").toArray(new String[0]);

        List<String> cipherSuites = TlsSpecificationsHelper.getTlsCipherSuitesIanaList(suites, TlsSpecificationsHelper.CipherSuitesLimitType.BLACKBOX_EXPORTER);
        String assertValue = String.join(", ", cipherSuites);
        assertEquals("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256," +
                " TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
                " TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", assertValue);
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
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256").toArray(new String[0]);

        String assertValue = TlsSpecificationsHelper.getTlsCipherSuites(suites, TlsSpecificationsHelper.CipherSuitesLimitType.BLACKBOX_EXPORTER, ":");
        assertEquals("ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384", assertValue);
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
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256").toArray(new String[0]);

        String assertValue = TlsSpecificationsHelper.getTlsCipherSuites(suites, TlsSpecificationsHelper.CipherSuitesLimitType.DEFAULT, ":");
        assertEquals("DH-DSS-AES128-GCM-SHA256:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:" +
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:" +
                "ECDHE-RSA-AES256-GCM-SHA384", assertValue);
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
                "TLS Invalid Suite").toArray(new String[0]);

        String assertValue = TlsSpecificationsHelper.getTlsCipherSuites(suites, TlsSpecificationsHelper.CipherSuitesLimitType.DEFAULT, ":");
        assertEquals("DH-DSS-AES128-GCM-SHA256:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:" +
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:" +
                "ECDHE-RSA-AES256-GCM-SHA384", assertValue);
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
                "TLS Invalid Suite").toArray(new String[0]);

        String assertValue = TlsSpecificationsHelper.getTlsCipherSuites(suites, TlsSpecificationsHelper.CipherSuitesLimitType.DEFAULT, ":");
        assertEquals("DH-DSS-AES128-GCM-SHA256:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:" +
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:" +
                "ECDHE-RSA-AES256-GCM-SHA384", assertValue);
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
                "TLS_DH_DSS_WITH_AES_128_GCM_SHA256").toArray(new String[0]);

        String assertValue = TlsSpecificationsHelper.getTlsCipherSuites(suites, TlsSpecificationsHelper.CipherSuitesLimitType.MINIMAL, ":");
        assertEquals("DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:" +
                "ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384", assertValue);
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
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256").toArray(new String[0]);

        String assertValue = TlsSpecificationsHelper.getTlsCipherSuites(suites, TlsSpecificationsHelper.CipherSuitesLimitType.REDHAT_VERSION8, ":");
        assertEquals("DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:" +
                "ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384", assertValue);
    }
}
