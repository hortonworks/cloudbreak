package com.sequenceiq.redbeams.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.security.cert.X509Certificate;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SslCertificateEntryTest {

    private static final String CLOUD_PROVIDER_IDENTIFIER = "certID";

    private static final String CLOUD_PROVIDER_IDENTIFIER_DEFAULT = "certID.default";

    private static final String CLOUD_PROVIDER_IDENTIFIER_EUWEST1 = "certID.euwest1";

    private static final String CERT_PEM = "foo";

    private static final String CERT_PEM_2 = "bar";

    private static final String AWS = "aws";

    private static final X509Certificate X_509_CERT = mock(X509Certificate.class);

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final int VERSION_123 = 123;

    private static final String FINGERPRINT = "fingerprint";

    static Object[][] constructorTestWhenNPEDataProvider() {
        return new Object[][]{
                // testCaseName version cloudProviderIdentifier certPem x509Cert
                {"0, null, certPem, x509Cert", VERSION_0, null, CERT_PEM, X_509_CERT},
                {"0, cloudProviderIdentifier, null, x509Cert", VERSION_0, CLOUD_PROVIDER_IDENTIFIER, null, X_509_CERT},
                {"0, cloudProviderIdentifier, certPem, null", VERSION_0, CLOUD_PROVIDER_IDENTIFIER, CERT_PEM, null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNPEDataProvider")
    void constructorTestWhenNPE(String testCaseName, int version, String cloudProviderIdentifier, String certPem, X509Certificate x509Cert) {
        assertThrows(NullPointerException.class, () -> new SslCertificateEntry(version, cloudProviderIdentifier,
                cloudProviderIdentifier, AWS, certPem, x509Cert));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNPEDataProvider")
    void constructorTestWhenNPEAndFingerprintAndDeprecated(String testCaseName, int version, String cloudProviderIdentifier, String certPem,
            X509Certificate x509Cert) {
        assertThrows(NullPointerException.class, () -> new SslCertificateEntry(version, cloudProviderIdentifier,
                cloudProviderIdentifier, AWS, certPem, x509Cert, null, new Date().getTime(), false));
    }

    static Object[][] constructorTestWhenNONNPEDataProvider() {
        return new Object[][]{
                // testCaseName version cloudProviderIdentifier cloudKey certPem x509Cert
                {"0, cloudProviderIdentifier, null, x509Cert", VERSION_0, CLOUD_PROVIDER_IDENTIFIER,
                        CLOUD_PROVIDER_IDENTIFIER, CERT_PEM, X_509_CERT},
                {"0, cloudProviderIdentifier, null, x509Cert", VERSION_0, CLOUD_PROVIDER_IDENTIFIER_DEFAULT,
                        CLOUD_PROVIDER_IDENTIFIER_DEFAULT, CERT_PEM, X_509_CERT},
                {"0, cloudProviderIdentifier, null, x509Cert", VERSION_0, CLOUD_PROVIDER_IDENTIFIER_EUWEST1,
                        CLOUD_PROVIDER_IDENTIFIER_EUWEST1, CERT_PEM, X_509_CERT},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNONNPEDataProvider")
    void constructorTestWhenNONNPE(String testCaseName, int version, String cloudProviderIdentifier, String cloudKey,
            String certPem, X509Certificate x509Cert) {
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(version, cloudProviderIdentifier,
                cloudProviderIdentifier, AWS, certPem, x509Cert, null, new Date().getTime(), false);

        assertThat(sslCertificateEntry.certPem()).isEqualTo(certPem);
        assertThat(sslCertificateEntry.cloudProviderIdentifier()).isEqualTo(cloudProviderIdentifier);
        assertThat(sslCertificateEntry.cloudKey()).isEqualTo(cloudKey);
        assertThat(sslCertificateEntry.x509Cert()).isEqualTo(x509Cert);
        assertThat(sslCertificateEntry.fingerprint()).isNull();
        assertThat(sslCertificateEntry.deprecated()).isFalse();
    }

    @Test
    void constructorTestWhenSuccess() {
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(VERSION_123, CLOUD_PROVIDER_IDENTIFIER,
                CLOUD_PROVIDER_IDENTIFIER, AWS, CERT_PEM, X_509_CERT, FINGERPRINT, new Date().getTime(), true);

        assertThat(sslCertificateEntry.version()).isEqualTo(VERSION_123);
        assertThat(sslCertificateEntry.certPem()).isEqualTo(CERT_PEM);
        assertThat(sslCertificateEntry.x509Cert()).isSameAs(X_509_CERT);
        assertThat(sslCertificateEntry.fingerprint()).isEqualTo(FINGERPRINT);
        assertThat(sslCertificateEntry.deprecated()).isTrue();
    }

    private static SslCertificateEntry createSslCertificateEntry(int version) {
        return createSslCertificateEntry(version, CERT_PEM);
    }

    private static SslCertificateEntry createSslCertificateEntry(int version, String certPem) {
        return new SslCertificateEntry(version, CLOUD_PROVIDER_IDENTIFIER, CLOUD_PROVIDER_IDENTIFIER, AWS, certPem,
                X_509_CERT, null, new Date().getTime(), false);
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