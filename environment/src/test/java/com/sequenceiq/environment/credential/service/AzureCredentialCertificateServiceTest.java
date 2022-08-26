package com.sequenceiq.environment.credential.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.credential.AppCertificateStatus;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialCertificate;

public class AzureCredentialCertificateServiceTest {

    private AzureCredentialCertificateService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureCredentialCertificateService();
    }

    @Test
    void testGenerate() {
        AzureCredentialCertificate certificate = underTest.generate();

        assertThat(certificate.getCertificate(), CoreMatchers.containsString("-----BEGIN CERTIFICATE-----"));
        assertThat(certificate.getCertificate(), CoreMatchers.containsString("-----END CERTIFICATE-----"));

        assertThat(certificate.getPrivateKey(), CoreMatchers.containsString("-----BEGIN PRIVATE KEY-----"));
        assertThat(certificate.getPrivateKey(), CoreMatchers.containsString("-----END PRIVATE KEY-----"));

        //we should not leak PK
        assertThat(certificate.toString(), not(CoreMatchers.containsString("PRIVATE")));

        assertEquals(AppCertificateStatus.KEY_GENERATED, certificate.getStatus());

    }
}
