package com.sequenceiq.redbeams.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class SslCertificateEntryTest {

    private static final String CERT_PEM = "foo";

    private static final String CERT_PEM_2 = "bar";

    private static final X509Certificate X_509_CERT = Mockito.mock(X509Certificate.class);

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final int VERSION_123 = 123;

    static Object[][] constructorTestWhenNPEDataProvider() {
        return new Object[][]{
                // testCaseName version certPem x509Cert
                {"0, null, x509Cert", VERSION_0, null, X_509_CERT},
                {"0, certPem, null", VERSION_0, CERT_PEM, null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNPEDataProvider")
    void constructorTestWhenNPE(String testCaseName, int version, String certPem, X509Certificate x509Cert) {
        assertThrows(NullPointerException.class, () -> new SslCertificateEntry(version, certPem, x509Cert));
    }

    @Test
    void constructorTestWhenSuccess() {
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(VERSION_123, CERT_PEM, X_509_CERT);

        assertThat(sslCertificateEntry.getVersion()).isEqualTo(VERSION_123);
        assertThat(sslCertificateEntry.getCertPem()).isEqualTo(CERT_PEM);
        assertThat(sslCertificateEntry.getX509Cert()).isSameAs(X_509_CERT);
    }

    private static SslCertificateEntry createSslCertificateEntry(int version) {
        return createSslCertificateEntry(version, CERT_PEM);
    }

    private static SslCertificateEntry createSslCertificateEntry(int version, String certPem) {
        return new SslCertificateEntry(version, certPem, X_509_CERT);
    }

    static Object[][] equalsDataProvider() {
        return new Object[][]{
                // testCaseName target ref equalityExpected
                {"v0, null", createSslCertificateEntry(VERSION_0), null, false},
                {"v0 with certCommon, v0 with certCommon", createSslCertificateEntry(VERSION_0), createSslCertificateEntry(VERSION_0), true},
                {"v0 with certA, v0 with certB", createSslCertificateEntry(VERSION_0), createSslCertificateEntry(VERSION_0, CERT_PEM_2), true},
                {"v0 with certCommon, v1 with certCommon", createSslCertificateEntry(VERSION_0), createSslCertificateEntry(VERSION_1), false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("equalsDataProvider")
    void equalsTest(String testCaseName, SslCertificateEntry target, SslCertificateEntry ref, boolean equalityExpected) {
        assertThat(target.equals(ref)).isEqualTo(equalityExpected);
    }

    static Object[][] hashCodeDataProvider() {
        return new Object[][]{
                // testCaseName entry1 entry2 equalityExpected
                {"v0 with certCommon, v0 with certCommon", createSslCertificateEntry(VERSION_0), createSslCertificateEntry(VERSION_0), true},
                {"v0 with certA, v0 with certB", createSslCertificateEntry(VERSION_0), createSslCertificateEntry(VERSION_0, CERT_PEM_2), true},
                {"v0 with certCommon, v1 with certCommon", createSslCertificateEntry(VERSION_0), createSslCertificateEntry(VERSION_1), false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hashCodeDataProvider")
    void hashCodeTest(String testCaseName, SslCertificateEntry entry1, SslCertificateEntry entry2, boolean equalityExpected) {
        assertThat(entry1.hashCode() == entry2.hashCode()).isEqualTo(equalityExpected);
    }

}