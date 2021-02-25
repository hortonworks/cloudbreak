package com.sequenceiq.cloudbreak.cloud.model.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CloudDatabaseServerSslCertificatesTest {

    @Test
    void constructorTestWhenNPE() {
        assertThrows(NullPointerException.class, () -> new CloudDatabaseServerSslCertificates(null));
    }

    @Test
    void constructorTestWhenSuccess() {
        Set<CloudDatabaseServerSslCertificate> sslCertificates =
                Set.of(mock(CloudDatabaseServerSslCertificate.class), mock(CloudDatabaseServerSslCertificate.class));

        CloudDatabaseServerSslCertificates underTest = new CloudDatabaseServerSslCertificates(sslCertificates);

        assertThat(underTest.getSslCertificates()).isSameAs(sslCertificates);
    }

}