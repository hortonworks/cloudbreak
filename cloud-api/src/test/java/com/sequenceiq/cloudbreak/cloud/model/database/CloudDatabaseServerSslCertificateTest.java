package com.sequenceiq.cloudbreak.cloud.model.database;

import static com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType.ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CloudDatabaseServerSslCertificateTest {

    private static final String CERTIFICATE_IDENTIFIER = "mycert";

    private static final String CERTIFICATE_PEM = "pem";

    private static Date date = new Date();

    static Object[][] constructorTestWhenNPEDataProvider() {
        return new Object[][]{
                // testCaseName certificateType certificateIdentifier
                {"null, certificateIdentifier", null, CERTIFICATE_IDENTIFIER},
                {"certificateType, null", ROOT, null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNPEDataProvider")
    void constructorTestWhenNPE(String testCaseName, CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier) {
        assertThrows(NullPointerException.class, () -> new CloudDatabaseServerSslCertificate(certificateType, certificateIdentifier));
        assertThrows(NullPointerException.class, () -> new CloudDatabaseServerSslCertificate(certificateType, certificateIdentifier,
                CERTIFICATE_PEM, date.getTime()));
        assertThrows(NullPointerException.class, () -> new CloudDatabaseServerSslCertificate(certificateType, certificateIdentifier, true));
        assertThrows(NullPointerException.class, () -> new CloudDatabaseServerSslCertificate(certificateType, certificateIdentifier,
                CERTIFICATE_PEM, date.getTime(), true));
    }

    @ParameterizedTest(name = "certificateIdentifier={0}")
    @ValueSource(strings = {"", " "})
    void constructorTestWhenBlankCertificateIdentifier(String certificateIdentifier) {
        assertThrows(IllegalArgumentException.class, () -> new CloudDatabaseServerSslCertificate(ROOT, certificateIdentifier));
        assertThrows(IllegalArgumentException.class, () -> new CloudDatabaseServerSslCertificate(ROOT, certificateIdentifier,
                CERTIFICATE_PEM, date.getTime()));
        assertThrows(IllegalArgumentException.class, () -> new CloudDatabaseServerSslCertificate(ROOT, certificateIdentifier, true));
        assertThrows(IllegalArgumentException.class, () -> new CloudDatabaseServerSslCertificate(ROOT, certificateIdentifier,
                CERTIFICATE_PEM, date.getTime(),  true));
    }

    @Test
    void constructorTestWhenSuccess() {
        CloudDatabaseServerSslCertificate sslCertificate =
                new CloudDatabaseServerSslCertificate(ROOT, CERTIFICATE_IDENTIFIER);

        assertThat(sslCertificate.certificateType()).isEqualTo(ROOT);
        assertThat(sslCertificate.certificateIdentifier()).isSameAs(CERTIFICATE_IDENTIFIER);
        assertThat(sslCertificate.certificate()).isNull();
        assertThat(sslCertificate.overridden()).isFalse();

        sslCertificate =
                new CloudDatabaseServerSslCertificate(ROOT, CERTIFICATE_IDENTIFIER, CERTIFICATE_PEM, date.getTime());

        assertThat(sslCertificate.certificateType()).isEqualTo(ROOT);
        assertThat(sslCertificate.certificateIdentifier()).isSameAs(CERTIFICATE_IDENTIFIER);
        assertThat(sslCertificate.certificate()).isSameAs(CERTIFICATE_PEM);
        assertThat(sslCertificate.overridden()).isFalse();

        sslCertificate =
                new CloudDatabaseServerSslCertificate(ROOT, CERTIFICATE_IDENTIFIER, true);

        assertThat(sslCertificate.certificateType()).isEqualTo(ROOT);
        assertThat(sslCertificate.certificateIdentifier()).isSameAs(CERTIFICATE_IDENTIFIER);
        assertThat(sslCertificate.certificate()).isNull();
        assertThat(sslCertificate.overridden()).isTrue();

        sslCertificate =
                new CloudDatabaseServerSslCertificate(ROOT, CERTIFICATE_IDENTIFIER, CERTIFICATE_PEM, date.getTime(), true);

        assertThat(sslCertificate.certificateType()).isEqualTo(ROOT);
        assertThat(sslCertificate.certificateIdentifier()).isSameAs(CERTIFICATE_IDENTIFIER);
        assertThat(sslCertificate.certificate()).isSameAs(CERTIFICATE_PEM);
        assertThat(sslCertificate.overridden()).isTrue();
    }

}