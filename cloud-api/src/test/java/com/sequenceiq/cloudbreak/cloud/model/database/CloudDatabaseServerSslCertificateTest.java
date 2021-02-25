package com.sequenceiq.cloudbreak.cloud.model.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CloudDatabaseServerSslCertificateTest {

    private static final String CERTIFICATE_IDENTIFIER = "mycert";

    static Object[][] constructorTestWhenNPEDataProvider() {
        return new Object[][]{
                // testCaseName certificateType certificateIdentifier
                {"null, certificateIdentifier", null, CERTIFICATE_IDENTIFIER},
                {"certificateType, null", CloudDatabaseServerSslCertificateType.ROOT, null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNPEDataProvider")
    void constructorTestWhenNPE(String testCaseName, CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier) {
        assertThrows(NullPointerException.class, () -> new CloudDatabaseServerSslCertificate(certificateType, certificateIdentifier));
    }

    @Test
    void constructorTestWhenSuccess() {
        CloudDatabaseServerSslCertificate sslCertificate =
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERTIFICATE_IDENTIFIER);

        assertThat(sslCertificate.getCertificateType()).isEqualTo(CloudDatabaseServerSslCertificateType.ROOT);
        assertThat(sslCertificate.getCertificateIdentifier()).isSameAs(CERTIFICATE_IDENTIFIER);
    }

}