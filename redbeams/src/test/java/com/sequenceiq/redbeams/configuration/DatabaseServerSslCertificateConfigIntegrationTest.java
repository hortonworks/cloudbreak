package com.sequenceiq.redbeams.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAvailabilityZoneProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsDefaultZoneProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResourcesTestSupport;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSubnetIgwExplorer;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AmazonKmsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DatabaseServerSslCertificateConfigIntegrationTest.TestAppContext.class)
class DatabaseServerSslCertificateConfigIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateConfigIntegrationTest.class);

    private static final int SINGLE_CERT = 1;

    private static final int FOUR_CERTS = 4;

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final int VERSION_2 = 2;

    private static final int VERSION_3 = 3;

    private static final String CERT_ISSUER_AWS_0 = "CN=Amazon RDS Root 2019 CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AZURE_0 = "CN=Baltimore CyberTrust Root,OU=CyberTrust,O=Baltimore,C=IE";

    private static final String CERT_ISSUER_AZURE_1 = "CN=DigiCert Global Root G2,OU=www.digicert.com,O=DigiCert Inc,C=US";

    private static final String CERT_ISSUER_AZURE_2 = "CN=DigiCert Global Root CA,OU=www.digicert.com,O=DigiCert Inc,C=US";

    private static final String CERT_ISSUER_AZURE_3 = "CN=Microsoft RSA Root Certificate Authority 2017,O=Microsoft Corporation,C=US";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_0 = "rds-ca-2019";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_0 = "BaltimoreCyberTrustRoot";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_1 = "DigiCertGlobalRootG2";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_2 = "DigiCertGlobalRootCA";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_3 = "MicrosoftRSARootCertificateAuthority2017";

    private static final String REGION_DUMMY = "dummy";

    private static final String MOCK_REGION_LONDON = "london";

    @Inject
    private AwsPlatformResources awsPlatformResources;

    @Inject
    private DatabaseServerSslCertificateConfig underTest;

    @Test
    void enabledAwsRegionsTest() {
        Set<String> enabledAwsRegions = getEnabledAwsRegions();
        LOGGER.info("Supported AWS regions: {}", enabledAwsRegions);
        assertThat(enabledAwsRegions).isNotEmpty();
    }

    private Set<String> getEnabledAwsRegions() {
        AwsPlatformResourcesTestSupport testSupport = new AwsPlatformResourcesTestSupport(awsPlatformResources);
        Set<String> enabledAwsRegions = testSupport.getEnabledRegions().stream()
                .map(Region::getRegionName)
                .collect(Collectors.toSet());
        enabledAwsRegions.add(REGION_DUMMY);
        enabledAwsRegions.add(null);
        return enabledAwsRegions;
    }

    private String getDisplayName(String cloudPlatform, String region) {
        return region == null ? cloudPlatform : String.format("%s %s", cloudPlatform, region);
    }

    @TestFactory
    List<DynamicTest> getNumberOfCertsByPlatformTest() {
        Set<String> enabledAwsRegions = getEnabledAwsRegions();
        List<DynamicTest> tests = new ArrayList<>(enabledAwsRegions.size() + 1);
        for (String region : enabledAwsRegions) {
            DynamicTest awsTest = DynamicTest.dynamicTest(getDisplayName(CloudPlatform.AWS.name(), region),
                    () -> assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), region)).isGreaterThanOrEqualTo(SINGLE_CERT));
            tests.add(awsTest);
        }
        DynamicTest azureTest = DynamicTest.dynamicTest(CloudPlatform.AZURE.name(),
                () -> assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEqualTo(FOUR_CERTS));
        tests.add(azureTest);
        return tests;
    }

    @TestFactory
    List<DynamicTest> getMinMaxVersionByPlatformTest() {
        Set<String> enabledAwsRegions = getEnabledAwsRegions();
        List<DynamicTest> tests = new ArrayList<>(enabledAwsRegions.size() + 1);
        for (String region : enabledAwsRegions) {
            DynamicTest awsTest = DynamicTest.dynamicTest(getDisplayName(CloudPlatform.AWS.name(), region), () -> {
                int minVersion = underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), region);
                int maxVersion = underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), region);
                assertThat(minVersion <= maxVersion).isTrue();
            });
            tests.add(awsTest);
        }
        DynamicTest azureTest = DynamicTest.dynamicTest(CloudPlatform.AZURE.name(), () -> {
            assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEqualTo(VERSION_0);
            assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEqualTo(VERSION_3);
        });
        tests.add(azureTest);
        return tests;
    }

    @Test
    void getSupportedPlatformsForCertsTest() {
        // It is not safe to test for all the regions in getEnabledAwsRegions(). Doing so would fail for any newly introduced region until the certs are added.
        Set<String> supportedPlatformsForCerts = underTest.getSupportedPlatformsForCerts();
        Set<String> awsRegions = supportedPlatformsForCerts.stream()
                .filter(p -> p.startsWith("aws"))
                .collect(Collectors.toSet());
        assertThat(awsRegions).isNotEmpty();
        assertThat(supportedPlatformsForCerts).containsAll(
                Set.of(
                    "azure",
                    "mock.london"
                )
        );
        assertThat(supportedPlatformsForCerts).doesNotContain("gcp");
    }

    @TestFactory
    List<DynamicTest> isCloudPlatformAndRegionSupportedForCertsTest() {
        Set<String> enabledAwsRegions = getEnabledAwsRegions();
        List<DynamicTest> tests = new ArrayList<>(enabledAwsRegions.size() + 1);
        for (String region : enabledAwsRegions) {
            DynamicTest awsTest = DynamicTest.dynamicTest(getDisplayName(CloudPlatform.AWS.name(), region),
                    () -> assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), region)).isTrue());
            tests.add(awsTest);
        }
        DynamicTest azureTest = DynamicTest.dynamicTest(CloudPlatform.AZURE.name(),
                () -> assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue());
        tests.add(azureTest);
        DynamicTest mockTest = DynamicTest.dynamicTest(CloudPlatform.MOCK.name(),
                () -> assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.MOCK.name(), MOCK_REGION_LONDON)).isTrue());
        tests.add(mockTest);
        DynamicTest gcpTest = DynamicTest.dynamicTest(CloudPlatform.GCP.name(),
                () -> assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.GCP.name(), null)).isFalse());
        tests.add(gcpTest);
        return tests;
    }

    @Test
    void getCertsTest() {
        Map<String, String> certs = underTest.getCerts();

        assertThat(certs).isNotNull();
        assertThat(certs.values()).doesNotContainNull();
        certs.values().forEach(c -> assertThat(c).isNotBlank());
    }

    @Test
    void getNumberOfCertsTotalTest() {
        assertThat(underTest.getNumberOfCertsTotal()).isPositive();
    }

    @TestFactory
    List<DynamicTest> getCertsByPlatformTest() {
        Set<String> enabledAwsRegions = getEnabledAwsRegions();
        List<DynamicTest> tests = new ArrayList<>(enabledAwsRegions.size() + 1);
        for (String region : enabledAwsRegions) {
            DynamicTest awsTest = DynamicTest.dynamicTest(getDisplayName(CloudPlatform.AWS.name(), region), () -> {
                Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), region);

                assertThat(certsAws).isNotNull();
                assertThat(certsAws).isNotEmpty();
                assertThat(certsAws).doesNotContainNull();
            });
            tests.add(awsTest);
        }
        DynamicTest azureTest = DynamicTest.dynamicTest(CloudPlatform.AZURE.name(), () -> {
            Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);

            assertThat(certsAzure).isNotNull();
            assertThat(certsAzure).hasSize(FOUR_CERTS);
            assertThat(certsAzure).doesNotContainNull();
        });
        tests.add(azureTest);
        return tests;
    }

    @Test
    void getCertsByPlatformAndVersionsTest() {
        // This cannot be tested for AWS regions in a general fashion. Verifying only the global fallback cert.
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), null, VERSION_0);

        assertThat(certsAwsGlobal).isNotNull();
        assertThat(certsAwsGlobal).hasSize(SINGLE_CERT);
        assertThat(certsAwsGlobal).doesNotContainNull();
        verifyCertEntry(certsAwsGlobal, VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        Set<SslCertificateEntry> certsAwsDummy = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_DUMMY, VERSION_0);

        assertThat(certsAwsDummy).isNotNull();
        assertThat(certsAwsDummy).hasSize(SINGLE_CERT);
        assertThat(certsAwsDummy).doesNotContainNull();
        verifyCertEntry(certsAwsDummy, VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AZURE.name(), null, VERSION_0, VERSION_1,
                VERSION_2, VERSION_3);

        assertThat(certsAzure).isNotNull();
        assertThat(certsAzure).hasSize(FOUR_CERTS);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_0, CERT_ISSUER_AZURE_0, CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(certsAzure, VERSION_1, CERT_ISSUER_AZURE_1, CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
        verifyCertEntry(certsAzure, VERSION_2, CERT_ISSUER_AZURE_2, CLOUD_PROVIDER_IDENTIFIER_AZURE_2);
        verifyCertEntry(certsAzure, VERSION_3, CERT_ISSUER_AZURE_3, CLOUD_PROVIDER_IDENTIFIER_AZURE_3);
    }

    @Test
    void getCertByPlatformAndVersionTest() {
        // This cannot be tested for AWS regions in a general fashion. Verifying only the global fallback cert.
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), null, VERSION_0), VERSION_0, CERT_ISSUER_AWS_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_DUMMY, VERSION_0), VERSION_0, CERT_ISSUER_AWS_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AZURE.name(), null, VERSION_0), VERSION_0, CERT_ISSUER_AZURE_0,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AZURE.name(), null, VERSION_1), VERSION_1, CERT_ISSUER_AZURE_1,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AZURE.name(), null, VERSION_2), VERSION_2, CERT_ISSUER_AZURE_2,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_2);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AZURE.name(), null, VERSION_3), VERSION_3, CERT_ISSUER_AZURE_3,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_3);
    }

    private void verifyCertEntry(Set<SslCertificateEntry> certs, int version, String certIssuerExpected, String cloudProviderIdentifierExpected) {
        Optional<SslCertificateEntry> cert = certs.stream()
                .filter(e -> e.version() == version)
                .findFirst();
        assertThat(cert).isPresent();
        verifyCertEntry(cert.get(), version, certIssuerExpected, cloudProviderIdentifierExpected);
    }

    private void verifyCertEntry(SslCertificateEntry cert, int version, String certIssuerExpected, String cloudProviderIdentifierExpected) {
        assertThat(cert).isNotNull();
        assertThat(cert.version()).isEqualTo(version);
        assertThat(cert.certPem()).isNotBlank();
        assertThat(cert.cloudProviderIdentifier()).isEqualTo(cloudProviderIdentifierExpected);

        X509Certificate x509Cert = cert.x509Cert();
        assertThat(x509Cert).isNotNull();

        X500Principal issuerX500Principal = x509Cert.getIssuerX500Principal();
        assertThat(issuerX500Principal).isNotNull();
        assertThat(issuerX500Principal.getName()).isEqualTo(certIssuerExpected);
    }

    @Test
    void getCertByPlatformAndCloudProviderIdentifierTest() {
        // This cannot be tested for AWS regions in a general fashion. Verifying only the global fallback cert.
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), null, CLOUD_PROVIDER_IDENTIFIER_AWS_0),
                VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_DUMMY,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_0),
                VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), null,
                        CLOUD_PROVIDER_IDENTIFIER_AZURE_0),
                VERSION_0, CERT_ISSUER_AZURE_0, CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), null,
                        CLOUD_PROVIDER_IDENTIFIER_AZURE_1),
                VERSION_1, CERT_ISSUER_AZURE_1, CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), null,
                        CLOUD_PROVIDER_IDENTIFIER_AZURE_2),
                VERSION_2, CERT_ISSUER_AZURE_2, CLOUD_PROVIDER_IDENTIFIER_AZURE_2);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), null,
                        CLOUD_PROVIDER_IDENTIFIER_AZURE_3),
                VERSION_3, CERT_ISSUER_AZURE_3, CLOUD_PROVIDER_IDENTIFIER_AZURE_3);
    }

    @Configuration
    @EnableConfigurationProperties
    @Import({
            DatabaseServerSslCertificateConfig.class,
            CloudbreakResourceReaderService.class,
            AwsPlatformResources.class
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private CommonAwsClient awsClient;

        @MockBean
        private AwsDefaultZoneProvider awsDefaultZoneProvider;

        @MockBean
        private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

        @MockBean
        private AwsAvailabilityZoneProvider awsAvailabilityZoneProvider;

        @MockBean
        private MinimalHardwareFilter minimalHardwareFilter;

        @MockBean
        private AwsPageCollector awsPageCollector;

        @MockBean
        private EntitlementService entitlementService;

        @MockBean
        private AmazonKmsUtil amazonKmsUtil;

        @MockBean
        private DbOverrideConfig dbOverrideConfig;

    }

}
