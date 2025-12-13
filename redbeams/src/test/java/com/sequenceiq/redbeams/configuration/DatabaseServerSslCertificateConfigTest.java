package com.sequenceiq.redbeams.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class DatabaseServerSslCertificateConfigTest {

    private static final String CLOUD_PROVIDER_IDENTIFIER_1 = "SSL.com-Root-Certification-Authority-ECC";

    private static final String CLOUD_PROVIDER_IDENTIFIER_2 = "SwissSign-Platinum-CA-G2";

    private static final String CLOUD_PROVIDER_IDENTIFIER_3 = "OISTE-WISeKey-Global-Root-GC-CA";

    private static final X509Certificate X_509_CERT = mock(X509Certificate.class);

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final int VERSION_2 = 2;

    private static final int NO_CERTS = 0;

    private static final int DUMMY_VERSION = Integer.MIN_VALUE;

    private static final String RDS_CA_2019 = "rds-ca-2019";

    private static final String DIGI_CERT_GLOBAL_ROOT_G_2 = "DigiCertGlobalRootG2";

    private static final String REGION_1 = "myRegion1";

    private static final String REGION_2 = "myRegion2";

    private DatabaseServerSslCertificateConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseServerSslCertificateConfig();
    }

    @Test
    void configReadTestWhenEmptyConfigShouldReturnZeroCerts() {
        initCerts("/test_certs/configReadTestWhenEmptyConfigShouldReturnZeroCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(true);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of());
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(0);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).isEmpty();

        verifyCertStatistics(CloudPlatform.AWS.name(), null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).isEmpty();

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isFalse();
    }

    @Test
    void configReadTestWhenAzureHasOneCertShouldReturnOneCert() {
        initCerts("/test_certs/configReadTestWhenAzureHasOneCertShouldReturnOneCert");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(1);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).isEmpty();

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isFalse();
    }

    private void initCerts(String certPath) {
        ReflectionTestUtils.setField(underTest, "certPath", certPath);

        underTest.setupCertsCache();
    }

    private void verifyCertStatistics(String cloudPlatform, String region, int numCerts, int minVersion, int maxVersion) {
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(numCerts);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(minVersion);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(maxVersion);
    }

    @Test
    void configReadTestWhenAwsIsDummyAzureHasOneCertShouldReturnOneCert() {
        initCerts("/test_certs/configReadTestWhenAwsIsDummyAzureHasOneCertShouldReturnOneCert");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(1);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).isEmpty();

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isFalse();
    }

    @Test
    void configReadTestWhenAwsHasTwoCertsShouldReturnTwoCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasTwoCertsShouldReturnTwoCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAwsGlobal).hasSize(2);
        assertThat(certsAwsGlobal).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_1, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsRegion1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_1);
        assertThat(certsAwsRegion1).hasSize(2);
        assertThat(certsAwsRegion1).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", REGION_1)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", REGION_1)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_2, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsRegion2 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_2);
        assertThat(certsAwsRegion2).hasSize(2);
        assertThat(certsAwsRegion2).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", REGION_2)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", REGION_2)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).isEmpty();

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void configReadTestWhenAwsHasTwoGlobalCertsAndOneRegionalCertShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasTwoGlobalCertsAndOneRegionalCertShouldReturnThreeCerts");

        configReadTestWhenAwsHasTwoGlobalCertsAndOneRegionalCertShouldReturnThreeCertsVerifyInternal();
    }

    private void configReadTestWhenAwsHasTwoGlobalCertsAndOneRegionalCertShouldReturnThreeCertsVerifyInternal() {
        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "aws." + REGION_1.toLowerCase()));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(3);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAwsGlobal).hasSize(2);
        assertThat(certsAwsGlobal).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_1, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAwsRegion1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_1);
        assertThat(certsAwsRegion1).hasSize(1);
        assertThat(certsAwsRegion1).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", REGION_1)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", REGION_1)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_2, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsRegion2 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_2);
        assertThat(certsAwsRegion2).hasSize(2);
        assertThat(certsAwsRegion2).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", REGION_2)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", REGION_2)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).isEmpty();

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isFalse();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void configReadTestWhenAwsHasTwoGlobalCertsAndThreeRegionalCertButTwoDeprecatedShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasTwoGlobalCertsAndThreeRegionalCertButTwoDeprecatedShouldReturnThreeCerts");

        configReadTestWhenAwsHasTwoGlobalCertsAndOneRegionalCertShouldReturnThreeCertsVerifyInternal();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsShouldReturnThreeCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(3);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsAndUppercaseIdentifiersShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsAndUppercaseIdentifiersShouldReturnThreeCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(3);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedCloudProviderIdentifier() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> initCerts("/test_certs/setupCertsCacheTestWhenErrorMalformedCloudProviderIdentifier"));
        assertThat(illegalArgumentException)
                .hasMessageStartingWith("Malformed SSL certificate CloudProviderIdentifier for cloud platform \"aws (global)\": \"");
    }

    @Test
    void configReadTestWhenAwsHasOneCertCommonWithAzureShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasOneCertCommonWithAzureShouldReturnThreeCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(3);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsWithOneCommonVersionShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsWithOneCommonVersionShouldReturnThreeCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(3);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_1, VERSION_1);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsWithOneCommonCloudProviderIdentifierShouldReturnThreeCerts() {
        initCerts("/test_certs/configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsWithOneCommonCloudProviderIdentifierShouldReturnThreeCerts");

        assertThat(underTest.getCerts().isEmpty()).isEqualTo(false);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(3);

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();

        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AZURE.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), null)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_1)).isTrue();
        assertThat(underTest.isCloudPlatformAndRegionSupportedForCerts(CloudPlatform.AWS.name(), REGION_2)).isTrue();
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedPem() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> initCerts("/test_certs/setupCertsCacheTestWhenErrorMalformedPem"));
        assertThat(illegalArgumentException).hasMessageStartingWith("Error parsing SSL certificate PEM for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorDuplicatedCloudProviderIdentifier() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> initCerts("/test_certs/setupCertsCacheTestWhenErrorDuplicatedCloudProviderIdentifier"));
        assertThat(illegalArgumentException)
                .hasMessage("Duplicated SSL certificate CloudProviderIdentifier for cloud platform \"aws (global)\": \"SwissSign-Platinum-CA-G2\"");
    }

    @Test
    void setupCertsCacheTestWhenErrorDuplicatedPem() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> initCerts("/test_certs/setupCertsCacheTestWhenErrorDuplicatedPem"));
        assertThat(illegalArgumentException).hasMessageStartingWith("Duplicated SSL certificate PEM for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorBadVersionRange() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> initCerts("/test_certs/setupCertsCacheTestWhenErrorBadVersionRange"));
        assertThat(illegalArgumentException).hasMessage("SSL certificate versions are not contiguous for cloud platform \"aws (global)\"");
    }

    @Test
    void setupCertsCacheTestWhenErrorBadVersionRangeAndDeprecated() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> initCerts("/test_certs/setupCertsCacheTestWhenErrorBadVersionRangeAndDeprecated"));
        assertThat(illegalArgumentException).hasMessage("SSL certificate versions are not contiguous for cloud platform \"aws (global)\"");
    }

    static Object[][] getLegacyMaxVersionByCloudPlatformAndRegionDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region legacyMaxVersionExpected
                {"null, null", null, null, DUMMY_VERSION},
                {"empty, null", "", null, DUMMY_VERSION},
                {"AWS, null", CloudPlatform.AWS.name(), null, 0},
                {"AWS, eu-south-1", CloudPlatform.AWS.name(), "eu-south-1", 0},
                {"AWS, af-south-1", CloudPlatform.AWS.name(), "af-south-1", 0},
                {"AWS, me-south-1", CloudPlatform.AWS.name(), "me-south-1", 0},
                {"AWS, ap-east-1", CloudPlatform.AWS.name(), "ap-east-1", 0},
                {"AWS, ap-southeast-3", CloudPlatform.AWS.name(), "ap-southeast-3", 0},
                {"AWS, us-gov-west-1", CloudPlatform.AWS.name(), "us-gov-west-1", 0},
                {"AWS, us-gov-east-1", CloudPlatform.AWS.name(), "us-gov-east-1", 0},
                {"aws, null", "aws", null, 0},
                {"Aws, null", "Aws", null, 0},
                {"AZURE, null", CloudPlatform.AZURE.name(), null, 1},
                {"Azure, null", "Azure", null, 1},
                {"aZure, null", "aZure", null, 1},
                {"GCP, null", CloudPlatform.GCP.name(), null, DUMMY_VERSION},
                {"MOCK, null", CloudPlatform.MOCK.name(), null, DUMMY_VERSION},
                {"YARN, null", CloudPlatform.YARN.name(), null, DUMMY_VERSION},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getLegacyMaxVersionByCloudPlatformAndRegionDataProvider")
    void getLegacyMaxVersionByCloudPlatformAndRegionTest(String testCaseName, String cloudPlatform, String region, int legacyMaxVersionExpected) {
        assertThat(underTest.getLegacyMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(legacyMaxVersionExpected);
    }

    static Object[][] getLegacyCloudProviderIdentifierByCloudPlatformAndRegionDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region legacyCloudProviderIdentifierExpected
                {"null, null", null, null, null},
                {"empty, null", "", null, null},
                {"AWS, null", CloudPlatform.AWS.name(), null, RDS_CA_2019},
                {"AWS, eu-south-1", CloudPlatform.AWS.name(), "eu-south-1", "rds-ca-2019-eu-south-1"},
                {"AWS, af-south-1", CloudPlatform.AWS.name(), "af-south-1", "rds-ca-2019-af-south-1"},
                {"AWS, me-south-1", CloudPlatform.AWS.name(), "me-south-1", "rds-ca-2019-me-south-1"},
                {"AWS, ap-east-1", CloudPlatform.AWS.name(), "ap-east-1", "rds-ca-rsa2048-g1"},
                {"AWS, ap-southeast-3", CloudPlatform.AWS.name(), "ap-southeast-3", "rds-ca-rsa2048-g1"},
                {"AWS, us-gov-west-1", CloudPlatform.AWS.name(), "us-gov-west-1", RDS_CA_2019},
                {"AWS, us-gov-east-1", CloudPlatform.AWS.name(), "us-gov-east-1", RDS_CA_2019},
                {"aws, null", "aws", null, RDS_CA_2019},
                {"Aws, null", "Aws", null, RDS_CA_2019},
                {"AZURE, null", CloudPlatform.AZURE.name(), null, DIGI_CERT_GLOBAL_ROOT_G_2},
                {"Azure, null", "Azure", null, DIGI_CERT_GLOBAL_ROOT_G_2},
                {"aZure, null", "aZure", null, DIGI_CERT_GLOBAL_ROOT_G_2},
                {"GCP, null", CloudPlatform.GCP.name(), null, null},
                {"MOCK, null", CloudPlatform.MOCK.name(), null, null},
                {"YARN, null", CloudPlatform.YARN.name(), null, null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getLegacyCloudProviderIdentifierByCloudPlatformAndRegionDataProvider")
    void getLegacyCloudProviderIdentifierByCloudPlatformAndRegionTest(String testCaseName, String cloudPlatform, String region,
            String legacyCloudProviderIdentifierExpected) {
        assertThat(underTest.getLegacyCloudProviderIdentifierByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(legacyCloudProviderIdentifierExpected);
    }

    @Test
    void getSupportedPlatformsForLegacyMaxVersionTest() {
        assertThat(underTest.getSupportedPlatformsForLegacyMaxVersion()).isEqualTo(Set.of("aws", "azure"));
    }

    @Test
    void testSslCertificatesOutdatedWhenNotEqualsTwoListMustReturnOUTDATED() {
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(1, "", "aws", "aws", "pem1", X_509_CERT);
        ReflectionTestUtils.setField(underTest, "certsByCloudPlatformCache", Map.of("aws", Set.of(sslCertificateEntry)));

        assertThat(underTest.getSslCertificatesOutdated("aws", "eu-west-1", Set.of("pem2"))).isEqualTo(SslCertStatus.OUTDATED);
    }

    @Test
    void testSslCertificatesOutdatedWhenEqualsTwoListMustReturnUPTODATE() {
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(1, "", "aws", "aws", "pem1", X_509_CERT);
        ReflectionTestUtils.setField(underTest, "certsByCloudPlatformCache", Map.of("aws", Set.of(sslCertificateEntry)));

        assertThat(underTest.getSslCertificatesOutdated("aws", "eu-west-1", Set.of("pem1"))).isEqualTo(SslCertStatus.UP_TO_DATE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formatCheckInput")
    void formatCheckForCertificates(String filePath, String cloudProviderIdentifierExpected, String issuerExpected, String cloudKeyExpected,
            String cloudPlatformExpected, int versionExpected) throws IOException {
        String fileContent = FileReaderUtils.readFileFromClasspath(filePath);

        if (issuerExpected == null) {
            IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                    () ->  underTest.parseCertEntry(filePath, fileContent));
            assertThat(illegalArgumentException.getMessage().contains("Error parsing SSL certificate PEM for cloud platform"))
                    .isEqualTo(true);
        } else {
            SslCertificateEntry sslCertificateEntry = underTest.parseCertEntry(filePath, fileContent);
            assertThat(sslCertificateEntry.cloudProviderIdentifier()).isEqualTo(cloudProviderIdentifierExpected);
            assertThat(sslCertificateEntry.x509Cert().getIssuerDN().getName()).isEqualTo(issuerExpected);
            assertThat(sslCertificateEntry.certPem()).isNotBlank();
            assertThat(sslCertificateEntry.cloudKey()).isEqualTo(cloudKeyExpected);
            assertThat(sslCertificateEntry.cloudPlatform()).isEqualTo(cloudPlatformExpected);
            assertThat(sslCertificateEntry.version()).isEqualTo(versionExpected);
            assertThat(sslCertificateEntry.fingerprint()).isNull();
            assertThat(sslCertificateEntry.deprecated()).isFalse();
        }
    }

    static Object[][] formatCheckInput() {
        return new Object[][]{
                // filePath, cloudProviderIdentifierExpected, issuerExpected, cloudKeyExpected, cloudPlatformExpected, versionExpected
                {
                    "/test_certs/formatCheck/aws/af-south-1/0.yml",
                    "rds-ca-2019-af-south-1",
                    "CN=Amazon RDS af-south-1 Root CA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", ST=Washington, L=Seattle, C=US",
                    "aws.af-south-1",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/ap-east-1/0.yml",
                    "rds-ca-rsa2048-g1",
                    "L=Seattle, CN=Amazon RDS ap-east-1 Root CA RSA2048 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.ap-east-1",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/ap-southeast-3/0.yml",
                    "rds-ca-rsa2048-g1",
                    "L=Seattle, CN=Amazon RDS ap-southeast-3 Root CA RSA2048 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.ap-southeast-3",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/default/0.yml",
                    "rds-ca-2019",
                    "CN=Amazon RDS Root 2019 CA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", ST=Washington, L=Seattle, C=US",
                    "aws",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/eu-south-1/0.yml",
                    "rds-ca-2019-eu-south-1",
                    "CN=Amazon RDS eu-south-1 Root CA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", ST=Washington, L=Seattle, C=US",
                    "aws.eu-south-1",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/eu-south-2/0.yml",
                    "rds-ca-rsa2048-g1",
                    "L=Seattle, CN=Amazon RDS eu-south-2 Root CA RSA2048 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.eu-south-2",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/me-south-1/0.yml",
                    "rds-ca-2019-me-south-1",
                    "CN=Amazon RDS me-south-1 Root CA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", ST=Washington, L=Seattle, C=US",
                    "aws.me-south-1",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-east-1/0.yml",
                    "rds-ca-2019-us-gov-east-1",
                    "CN=Amazon RDS CN Root CA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", ST=Washington, L=Seattle, C=US",
                    "aws.us-gov-east-1",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-east-1/1.yml",
                    "rds-ca-ecc384-g1",
                    "L=Seattle, CN=Amazon RDS us-gov-east-1 Root CA ECC384 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.us-gov-east-1",
                    "aws",
                    1
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-east-1/2.yml",
                    "rds-ca-rsa2048-g1",
                    "L=Seattle, CN=Amazon RDS us-gov-east-1 Root CA RSA2048 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.us-gov-east-1",
                    "aws",
                    2
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-east-1/3.yml",
                    "rds-ca-rsa4096-g1",
                    "L=Seattle, CN=Amazon RDS us-gov-east-1 Root CA RSA4096 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.us-gov-east-1",
                    "aws",
                    3
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-west-1/0.yml",
                    "rds-ca-2019-us-gov-west-1",
                    "CN=Amazon RDS GovCloud Root CA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", ST=Washington, L=Seattle, C=US",
                    "aws.us-gov-west-1",
                    "aws",
                    0
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-west-1/1.yml",
                    "rds-ca-ecc384-g1",
                    "L=Seattle, CN=Amazon RDS us-gov-west-1 Root CA ECC384 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.us-gov-west-1",
                    "aws",
                    1
                },
                {
                    "/test_certs/formatCheck/aws/us-gov-west-1/2.yml",
                    "rds-ca-rsa2048-g1",
                    "L=Seattle, CN=Amazon RDS us-gov-west-1 Root CA RSA2048 G1, ST=WA, OU=Amazon RDS, O=\"Amazon Web Services, Inc.\", C=US",
                    "aws.us-gov-west-1",
                    "aws",
                    2
                },
                {
                    "/test_certs/formatCheck/azure/default/0.yml",
                    "BaltimoreCyberTrustRoot",
                    "CN=Baltimore CyberTrust Root, OU=CyberTrust, O=Baltimore, C=IE",
                    "azure",
                    "azure",
                    0
                },
                {
                    "/test_certs/formatCheck/azure/default/1.yml",
                    "DigiCertGlobalRootG2",
                    "CN=DigiCert Global Root G2, OU=www.digicert.com, O=DigiCert Inc, C=US",
                    "azure",
                    "azure",
                    1
                },
                {
                    "/test_certs/formatCheck/aws/error/0.yml",
                    "DigiCertGlobalRootG2",
                    null,
                    "aws",
                    "aws",
                    0
                },
        };
    }

    @Test
    void getSupportedPlatformsForLegacyCloudProviderIdentifierTest() {
        assertThat(underTest.getSupportedPlatformsForLegacyCloudProviderIdentifier())
                .isEqualTo(Set.of("aws", "aws.eu-south-1", "aws.af-south-1", "aws.me-south-1", "aws.ap-east-1", "aws.ap-southeast-3", "azure"));
    }

    static Object[][] getCertByCloudPlatformAndRegionAndVersionDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region version certFoundExpected
                {"AWS, null, vDummy", CloudPlatform.AWS.name(), null, DUMMY_VERSION, false},
                {"AWS, null, v0", CloudPlatform.AWS.name(), null, VERSION_0, false},
                {"AWS, null, v1", CloudPlatform.AWS.name(), null, VERSION_1, true},
                {"AWS, null, v2", CloudPlatform.AWS.name(), null, VERSION_2, true},
                {"AWS, REGION_1, v0", CloudPlatform.AWS.name(), REGION_1, VERSION_0, true},
                {"AWS, REGION_1, v1", CloudPlatform.AWS.name(), REGION_1, VERSION_1, false},
                {"AWS, REGION_1, v2", CloudPlatform.AWS.name(), REGION_1, VERSION_2, false},
                {"AWS, REGION_2, v0", CloudPlatform.AWS.name(), REGION_2, VERSION_0, false},
                {"AWS, REGION_2, v1", CloudPlatform.AWS.name(), REGION_2, VERSION_1, true},
                {"AWS, REGION_2, v2", CloudPlatform.AWS.name(), REGION_2, VERSION_2, true},
                {"AZURE, null, v0", CloudPlatform.AZURE.name(), null, VERSION_0, false},
                {"AZURE, null, v1", CloudPlatform.AZURE.name(), null, VERSION_1, false},
                {"AZURE, null, v2", CloudPlatform.AZURE.name(), null, VERSION_2, false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getCertByCloudPlatformAndRegionAndVersionDataProvider")
    void getCertByCloudPlatformAndRegionAndVersionTest(String testCaseName, String cloudPlatform, String region, int version, boolean certFoundExpected) {
        initCerts("/test_certs/getCertByCloudPlatformAndRegionAndVersionTest");

        SslCertificateEntry result = underTest.getCertByCloudPlatformAndRegionAndVersion(cloudPlatform, region, version);
        if (certFoundExpected) {
            assertThat(result).isNotNull();
            assertThat(result.version()).isEqualTo(version);
        } else {
            assertThat(result).isNull();
        }
    }

    static Object[][] getCertsByCloudPlatformAndRegionAndVersionsDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region versions versionsExpected
                {"AWS, null, null", CloudPlatform.AWS.name(), null, null, Set.of()},
                {"AWS, null, []", CloudPlatform.AWS.name(), null, new int[]{}, Set.of()},
                {"AWS, null, [vDummy]", CloudPlatform.AWS.name(), null, new int[]{DUMMY_VERSION}, Set.of()},
                {"AWS, null, [v0]", CloudPlatform.AWS.name(), null, new int[]{VERSION_0}, Set.of()},
                {"AWS, null, [v2]", CloudPlatform.AWS.name(), null, new int[]{VERSION_2}, Set.of(VERSION_2)},
                {"AWS, null, [v2, v0]", CloudPlatform.AWS.name(), null, new int[]{VERSION_2, VERSION_0}, Set.of(VERSION_2)},
                {"AWS, null, [v2, v1]", CloudPlatform.AWS.name(), null, new int[]{VERSION_2, VERSION_1}, Set.of(VERSION_1, VERSION_2)},
                {"AWS, null, [v2, v0, v1]", CloudPlatform.AWS.name(), null, new int[]{VERSION_2, VERSION_0, VERSION_1}, Set.of(VERSION_1, VERSION_2)},
                {"AWS, null, [v2, v2, v1]", CloudPlatform.AWS.name(), null, new int[]{VERSION_2, VERSION_2, VERSION_1}, Set.of(VERSION_1, VERSION_2)},
                {"AWS, null, [v2, v0, v2, v1]", CloudPlatform.AWS.name(), null, new int[]{VERSION_2, VERSION_0, VERSION_2, VERSION_1},
                        Set.of(VERSION_1, VERSION_2)},
                {"AWS, REGION_1, null", CloudPlatform.AWS.name(), REGION_1, null, Set.of()},
                {"AWS, REGION_1, []", CloudPlatform.AWS.name(), REGION_1, new int[]{}, Set.of()},
                {"AWS, REGION_1, [vDummy]", CloudPlatform.AWS.name(), REGION_1, new int[]{DUMMY_VERSION}, Set.of()},
                {"AWS, REGION_1, [v0]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_0}, Set.of(VERSION_0)},
                {"AWS, REGION_1, [v2]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_2}, Set.of()},
                {"AWS, REGION_1, [v2, v0]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_2, VERSION_0}, Set.of(VERSION_0)},
                {"AWS, REGION_1, [v2, v1]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_2, VERSION_1}, Set.of()},
                {"AWS, REGION_1, [v2, v0, v1]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_2, VERSION_0, VERSION_1}, Set.of(VERSION_0)},
                {"AWS, REGION_1, [v2, v2, v1]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_2, VERSION_2, VERSION_1}, Set.of()},
                {"AWS, REGION_1, [v2, v0, v2, v1]", CloudPlatform.AWS.name(), REGION_1, new int[]{VERSION_2, VERSION_0, VERSION_2, VERSION_1},
                        Set.of(VERSION_0)},
                {"AWS, REGION_2, null", CloudPlatform.AWS.name(), REGION_2, null, Set.of()},
                {"AWS, REGION_2, []", CloudPlatform.AWS.name(), REGION_2, new int[]{}, Set.of()},
                {"AWS, REGION_2, [vDummy]", CloudPlatform.AWS.name(), REGION_2, new int[]{DUMMY_VERSION}, Set.of()},
                {"AWS, REGION_2, [v0]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_0}, Set.of()},
                {"AWS, REGION_2, [v2]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_2}, Set.of(VERSION_2)},
                {"AWS, REGION_2, [v2, v0]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_2, VERSION_0}, Set.of(VERSION_2)},
                {"AWS, REGION_2, [v2, v1]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_2, VERSION_1}, Set.of(VERSION_1, VERSION_2)},
                {"AWS, REGION_2, [v2, v0, v1]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_2, VERSION_0, VERSION_1}, Set.of(VERSION_1, VERSION_2)},
                {"AWS, REGION_2, [v2, v2, v1]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_2, VERSION_2, VERSION_1}, Set.of(VERSION_1, VERSION_2)},
                {"AWS, REGION_2, [v2, v0, v2, v1]", CloudPlatform.AWS.name(), REGION_2, new int[]{VERSION_2, VERSION_0, VERSION_2, VERSION_1},
                        Set.of(VERSION_1, VERSION_2)},
                {"AZURE, null, [v0, v2, v1]", CloudPlatform.AZURE.name(), null, new int[]{VERSION_0, VERSION_2, VERSION_1}, Set.of()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getCertsByCloudPlatformAndRegionAndVersionsDataProvider")
    void getCertsByCloudPlatformAndRegionAndVersionsTest(String testCaseName, String cloudPlatform, String region, int[] versions,
            Set<Integer> versionsExpected) {
        initCerts("/test_certs/getCertsByCloudPlatformAndRegionAndVersionsTest");

        Set<SslCertificateEntry> result = underTest.getCertsByCloudPlatformAndRegionAndVersions(cloudPlatform, region, versions);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContainNull();
        Set<Integer> versionsFound = result.stream()
                .map(SslCertificateEntry::version)
                .collect(Collectors.toSet());
        assertThat(versionsFound).containsExactlyInAnyOrderElementsOf(versionsExpected);
    }

    static Object[][] getCertByCloudPlatformAndRegionAndCloudProviderIdentifierDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region cloudProviderIdentifier certFoundExpected
                {"AWS, null, null", CloudPlatform.AWS.name(), null, null, false},
                {"AWS, null, empty", CloudPlatform.AWS.name(), null, "", false},
                {"AWS, null, CLOUD_PROVIDER_IDENTIFIER_1", CloudPlatform.AWS.name(), null, CLOUD_PROVIDER_IDENTIFIER_1, false},
                {"AWS, null, CLOUD_PROVIDER_IDENTIFIER_2", CloudPlatform.AWS.name(), null, CLOUD_PROVIDER_IDENTIFIER_2, true},
                {"AWS, null, CLOUD_PROVIDER_IDENTIFIER_3", CloudPlatform.AWS.name(), null, CLOUD_PROVIDER_IDENTIFIER_3, true},
                {"AWS, REGION_1, null", CloudPlatform.AWS.name(), REGION_1, null, false},
                {"AWS, REGION_1, empty", CloudPlatform.AWS.name(), REGION_1, "", false},
                {"AWS, REGION_1, CLOUD_PROVIDER_IDENTIFIER_1", CloudPlatform.AWS.name(), REGION_1, CLOUD_PROVIDER_IDENTIFIER_1, true},
                {"AWS, REGION_1, CLOUD_PROVIDER_IDENTIFIER_2", CloudPlatform.AWS.name(), REGION_1, CLOUD_PROVIDER_IDENTIFIER_2, false},
                {"AWS, REGION_1, CLOUD_PROVIDER_IDENTIFIER_3", CloudPlatform.AWS.name(), REGION_1, CLOUD_PROVIDER_IDENTIFIER_3, false},
                {"AWS, REGION_2, null", CloudPlatform.AWS.name(), REGION_2, null, false},
                {"AWS, REGION_2, empty", CloudPlatform.AWS.name(), REGION_2, "", false},
                {"AWS, REGION_2, CLOUD_PROVIDER_IDENTIFIER_1", CloudPlatform.AWS.name(), REGION_2, CLOUD_PROVIDER_IDENTIFIER_1, false},
                {"AWS, REGION_2, CLOUD_PROVIDER_IDENTIFIER_2", CloudPlatform.AWS.name(), REGION_2, CLOUD_PROVIDER_IDENTIFIER_2, true},
                {"AWS, REGION_2, CLOUD_PROVIDER_IDENTIFIER_3", CloudPlatform.AWS.name(), REGION_2, CLOUD_PROVIDER_IDENTIFIER_3, true},
                {"AZURE, null, CLOUD_PROVIDER_IDENTIFIER_1", CloudPlatform.AZURE.name(), null, CLOUD_PROVIDER_IDENTIFIER_1, false},
                {"AZURE, null, CLOUD_PROVIDER_IDENTIFIER_2", CloudPlatform.AZURE.name(), null, CLOUD_PROVIDER_IDENTIFIER_2, false},
                {"AZURE, null, CLOUD_PROVIDER_IDENTIFIER_3", CloudPlatform.AZURE.name(), null, CLOUD_PROVIDER_IDENTIFIER_3, false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getCertByCloudPlatformAndRegionAndCloudProviderIdentifierDataProvider")
    void getCertByCloudplatformAndRegionAndCloudProviderIdentifierTest(String testCaseName, String cloudPlatform, String region, String cloudProviderIdentifier,
            boolean certFoundExpected) {
        initCerts("/test_certs/getCertByCloudPlatformAndRegionAndCloudProviderIdentifierTest");

        SslCertificateEntry result = underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(cloudPlatform, region, cloudProviderIdentifier);
        if (certFoundExpected) {
            assertThat(result).isNotNull();
            assertThat(result.cloudProviderIdentifier()).isEqualTo(cloudProviderIdentifier);
        } else {
            assertThat(result).isNull();
        }
    }

}