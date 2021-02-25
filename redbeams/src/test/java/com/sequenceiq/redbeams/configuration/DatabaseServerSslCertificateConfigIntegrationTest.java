package com.sequenceiq.redbeams.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DatabaseServerSslCertificateConfigIntegrationTest.TestAppContext.class)
class DatabaseServerSslCertificateConfigIntegrationTest {

    private static final int SINGLE_CERT = 1;

    private static final int TWO_CERTS = 2;

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final String CERT_ISSUER_AWS_0 = "CN=Amazon RDS Root 2019 CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AZURE_0 = "CN=Baltimore CyberTrust Root,OU=CyberTrust,O=Baltimore,C=IE";

    private static final String CERT_ISSUER_AZURE_1 = "CN=DigiCert Global Root G2,OU=www.digicert.com,O=DigiCert Inc,C=US";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_0 = "rds-ca-2019";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_0 = "BaltimoreCyberTrustRoot";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_1 = "DigiCertGlobalRootG2";

    @Inject
    private DatabaseServerSslCertificateConfig underTest;

    @Test
    void getNumberOfCertsByPlatformTest() {
        assertThat(underTest.getNumberOfCertsByPlatform(CloudPlatform.AWS.name())).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByPlatform(CloudPlatform.AZURE.name())).isEqualTo(TWO_CERTS);
    }

    @Test
    void getMinVersionByPlatformTest() {
        assertThat(underTest.getMinVersionByPlatform(CloudPlatform.AWS.name())).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByPlatform(CloudPlatform.AZURE.name())).isEqualTo(VERSION_0);
    }

    @Test
    void getMaxVersionByPlatformTest() {
        assertThat(underTest.getMaxVersionByPlatform(CloudPlatform.AWS.name())).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByPlatform(CloudPlatform.AZURE.name())).isEqualTo(VERSION_1);
    }

    @Test
    void getSupportedPlatformsForCertsTest() {
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));
    }

    @Test
    void getCertsTest() {
        Map<String, String> certs = underTest.getCerts();

        assertThat(certs).isNotNull();
        assertThat(certs).hasSize(TWO_CERTS);
        assertThat(certs.values()).doesNotContainNull();
        certs.values().forEach(c -> assertThat(c).isNotBlank());
    }

    @Test
    void getCertsByPlatformTest() {
        Set<SslCertificateEntry> certsAws = underTest.getCertsByPlatform(CloudPlatform.AWS.name());

        assertThat(certsAws).isNotNull();
        assertThat(certsAws).hasSize(SINGLE_CERT);
        assertThat(certsAws).doesNotContainNull();

        Set<SslCertificateEntry> certsAzure = underTest.getCertsByPlatform(CloudPlatform.AZURE.name());

        assertThat(certsAzure).isNotNull();
        assertThat(certsAzure).hasSize(TWO_CERTS);
        assertThat(certsAzure).doesNotContainNull();
    }

    @Test
    void getCertsByPlatformAndVersionsTest() {
        Set<SslCertificateEntry> certsAws = underTest.getCertsByPlatformAndVersions(CloudPlatform.AWS.name(), VERSION_0);

        assertThat(certsAws).isNotNull();
        assertThat(certsAws).hasSize(SINGLE_CERT);
        assertThat(certsAws).doesNotContainNull();
        verifyCertEntry(certsAws, VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        Set<SslCertificateEntry> certsAzure = underTest.getCertsByPlatformAndVersions(CloudPlatform.AZURE.name(), VERSION_0, VERSION_1);

        assertThat(certsAzure).isNotNull();
        assertThat(certsAzure).hasSize(TWO_CERTS);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_0, CERT_ISSUER_AZURE_0, CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(certsAzure, VERSION_1, CERT_ISSUER_AZURE_1, CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
    }

    @Test
    void getCertByPlatformAndVersionTest() {
        verifyCertEntry(underTest.getCertByPlatformAndVersion(CloudPlatform.AWS.name(), VERSION_0), VERSION_0, CERT_ISSUER_AWS_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByPlatformAndVersion(CloudPlatform.AZURE.name(), VERSION_0), VERSION_0, CERT_ISSUER_AZURE_0,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(underTest.getCertByPlatformAndVersion(CloudPlatform.AZURE.name(), VERSION_1), VERSION_1, CERT_ISSUER_AZURE_1,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
    }

    private void verifyCertEntry(Set<SslCertificateEntry> certs, int version, String certIssuerExpected, String cloudProviderIdentifierExpected) {
        Optional<SslCertificateEntry> cert = certs.stream()
                .filter(e -> e.getVersion() == version)
                .findFirst();
        assertThat(cert).isPresent();
        verifyCertEntry(cert.get(), version, certIssuerExpected, cloudProviderIdentifierExpected);
    }

    private void verifyCertEntry(SslCertificateEntry cert, int version, String certIssuerExpected, String cloudProviderIdentifierExpected) {
        assertThat(cert.getVersion()).isEqualTo(version);
        assertThat(cert.getCertPem()).isNotBlank();
        assertThat(cert.getCloudProviderIdentifier()).isEqualTo(cloudProviderIdentifierExpected);

        X509Certificate x509Cert = cert.getX509Cert();
        assertThat(x509Cert).isNotNull();

        X500Principal issuerX500Principal = x509Cert.getIssuerX500Principal();
        assertThat(issuerX500Principal).isNotNull();
        assertThat(issuerX500Principal.getName()).isEqualTo(certIssuerExpected);
    }

    @Test
    void getCertByPlatformAndCloudProviderIdentifierTest() {
        verifyCertEntry(underTest.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AWS.name(), CLOUD_PROVIDER_IDENTIFIER_AWS_0), VERSION_0,
                CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), CLOUD_PROVIDER_IDENTIFIER_AZURE_0), VERSION_0,
                CERT_ISSUER_AZURE_0, CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(underTest.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), CLOUD_PROVIDER_IDENTIFIER_AZURE_1), VERSION_1,
                CERT_ISSUER_AZURE_1, CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
    }

    @Configuration
    @EnableConfigurationProperties
    @Import({
            DatabaseServerSslCertificateConfig.class,
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {
    }

}
