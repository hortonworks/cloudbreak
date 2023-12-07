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

    private static final int THREE_CERTS = 3;

    private static final int FOUR_CERTS = 4;

    private static final int NUM_CERTS_TOTAL = 22;

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final int VERSION_2 = 2;

    private static final int VERSION_3 = 3;

    private static final String CERT_ISSUER_AWS_0 = "CN=Amazon RDS Root 2019 CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AWS_APSE3_0 =
            "L=Seattle,CN=Amazon RDS ap-southeast-3 Root CA RSA2048 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_AFS1_0 =
            "CN=Amazon RDS af-south-1 Root CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AWS_EUS1_0 =
            "CN=Amazon RDS eu-south-1 Root CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AWS_EUS2_0 =
            "L=Seattle,CN=Amazon RDS eu-south-2 Root CA RSA2048 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_MES1_0 =
            "CN=Amazon RDS me-south-1 Root CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AWS_APE1_0 =
            "L=Seattle,CN=Amazon RDS ap-east-1 Root CA RSA2048 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_USGW1_0 =
            "CN=Amazon RDS GovCloud Root CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AWS_USGW1_1 =
            "L=Seattle,CN=Amazon RDS us-gov-west-1 Root CA ECC384 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_USGW1_2 =
            "L=Seattle,CN=Amazon RDS us-gov-west-1 Root CA RSA2048 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_USGW1_3 =
            "L=Seattle,CN=Amazon RDS us-gov-west-1 Root CA RSA4096 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_USGE1_0 =
            "CN=Amazon RDS CN Root CA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,ST=Washington,L=Seattle,C=US";

    private static final String CERT_ISSUER_AWS_USGE1_1 =
            "L=Seattle,CN=Amazon RDS us-gov-east-1 Root CA ECC384 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_USGE1_2 =
            "L=Seattle,CN=Amazon RDS us-gov-east-1 Root CA RSA2048 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AWS_USGE1_3 =
            "L=Seattle,CN=Amazon RDS us-gov-east-1 Root CA RSA4096 G1,ST=WA,OU=Amazon RDS,O=Amazon Web Services\\, Inc.,C=US";

    private static final String CERT_ISSUER_AZURE_0 = "CN=Baltimore CyberTrust Root,OU=CyberTrust,O=Baltimore,C=IE";

    private static final String CERT_ISSUER_AZURE_1 = "CN=DigiCert Global Root G2,OU=www.digicert.com,O=DigiCert Inc,C=US";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_0 = "rds-ca-2019";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_AFS1_0 = "rds-ca-2019-af-south-1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_EUS1_0 = "rds-ca-2019-eu-south-1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_MES1_0 = "rds-ca-2019-me-south-1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_APE1_0 = "rds-ca-rsa2048-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_EUS2_0 = "rds-ca-rsa2048-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_0 = "rds-ca-2019-us-gov-west-1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_1 = "rds-ca-ecc384-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_2 = "rds-ca-rsa2048-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_3 = "rds-ca-rsa4096-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_0 = "rds-ca-2019-us-gov-east-1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_1 = "rds-ca-ecc384-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_2 = "rds-ca-rsa2048-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_3 = "rds-ca-rsa4096-g1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_0 = "BaltimoreCyberTrustRoot";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AZURE_1 = "DigiCertGlobalRootG2";

    private static final String CLOUD_PROVIDER_IDENTIFIER_AWS_APSE3_0 = "rds-ca-rsa2048-g1";

    private static final String REGION_EUS1 = "eu-south-1";

    private static final String REGION_EUS2 = "eu-south-2";

    private static final String REGION_ILC1 = "il-central-1";

    private static final String REGION_AFS1 = "af-south-1";

    private static final String REGION_MES1 = "me-south-1";

    private static final String REGION_MEC1 = "me-central-1";

    private static final String REGION_APE1 = "ap-east-1";

    private static final String REGION_USGE1 = "us-gov-east-1";

    private static final String REGION_USGW1 = "us-gov-west-1";

    private static final String REGION_APS3 = "ap-southeast-3";

    private static final String REGION_DUMMY = "dummy";

    @Inject
    private DatabaseServerSslCertificateConfig underTest;

    @Test
    void getNumberOfCertsByPlatformTest() {
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_AFS1)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_EUS1)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MES1)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MEC1)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APE1)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGW1)).isEqualTo(FOUR_CERTS);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGE1)).isEqualTo(FOUR_CERTS);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_DUMMY)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APS3)).isEqualTo(SINGLE_CERT);
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEqualTo(THREE_CERTS);
    }

    @Test
    void getMinVersionByPlatformTest() {
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_AFS1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_EUS1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MES1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MEC1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APE1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGW1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGE1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_DUMMY)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APS3)).isEqualTo(VERSION_0);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEqualTo(VERSION_0);
    }

    @Test
    void getMaxVersionByPlatformTest() {
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_AFS1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_EUS1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MES1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MEC1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APE1)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGW1)).isEqualTo(VERSION_3);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGE1)).isEqualTo(VERSION_3);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_DUMMY)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APS3)).isEqualTo(VERSION_0);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null)).isEqualTo(VERSION_2);
    }

    @Test
    void getSupportedPlatformsForCertsTest() {
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(
                Set.of(
                    "aws",
                    "aws." + REGION_EUS1,
                    "aws." + REGION_EUS2,
                    "aws." + REGION_ILC1,
                    "aws." + REGION_USGE1,
                    "aws." + REGION_AFS1,
                    "aws." + REGION_MES1,
                    "aws." + REGION_MEC1,
                    "aws." + REGION_APE1,
                    "aws." + REGION_USGW1,
                    "aws." + REGION_APS3,
                    "azure",
                    "mock.london"
                )
        );
    }

    @Test
    void getCertsTest() {
        Map<String, String> certs = underTest.getCerts();

        assertThat(certs).isNotNull();
        assertThat(certs).hasSize(NUM_CERTS_TOTAL);
        assertThat(certs.values()).doesNotContainNull();
        certs.values().forEach(c -> assertThat(c).isNotBlank());
    }

    @Test
    void getNumberOfCertsTotalTest() {
        assertThat(underTest.getNumberOfCertsTotal()).isEqualTo(NUM_CERTS_TOTAL);
    }

    @Test
    void getCertsByPlatformTest() {
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);

        assertThat(certsAwsGlobal).isNotNull();
        assertThat(certsAwsGlobal).hasSize(SINGLE_CERT);
        assertThat(certsAwsGlobal).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsAfs1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_AFS1);

        assertThat(certsAwsAfs1).isNotNull();
        assertThat(certsAwsAfs1).hasSize(SINGLE_CERT);
        assertThat(certsAwsAfs1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsEus1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_EUS1);

        assertThat(certsAwsEus1).isNotNull();
        assertThat(certsAwsEus1).hasSize(SINGLE_CERT);
        assertThat(certsAwsEus1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsMes1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MES1);

        assertThat(certsAwsMes1).isNotNull();
        assertThat(certsAwsMes1).hasSize(SINGLE_CERT);
        assertThat(certsAwsMes1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsMec1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_MEC1);

        assertThat(certsAwsMec1).isNotNull();
        assertThat(certsAwsMec1).hasSize(SINGLE_CERT);
        assertThat(certsAwsMec1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsApe1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APE1);

        assertThat(certsAwsApe1).isNotNull();
        assertThat(certsAwsApe1).hasSize(SINGLE_CERT);
        assertThat(certsAwsApe1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsUsgw1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGW1);

        assertThat(certsAwsUsgw1).isNotNull();
        assertThat(certsAwsUsgw1).hasSize(FOUR_CERTS);
        assertThat(certsAwsUsgw1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsApse3 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_APS3);

        assertThat(certsAwsApse3).isNotNull();
        assertThat(certsAwsApse3).hasSize(SINGLE_CERT);
        assertThat(certsAwsApse3).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsUsge1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_USGE1);

        assertThat(certsAwsUsge1).isNotNull();
        assertThat(certsAwsUsge1).hasSize(FOUR_CERTS);
        assertThat(certsAwsUsge1).doesNotContainNull();

        Set<SslCertificateEntry> certsAwsDummy = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_DUMMY);

        assertThat(certsAwsDummy).isNotNull();
        assertThat(certsAwsDummy).hasSize(SINGLE_CERT);
        assertThat(certsAwsDummy).doesNotContainNull();

        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);

        assertThat(certsAzure).isNotNull();
        assertThat(certsAzure).hasSize(THREE_CERTS);
        assertThat(certsAzure).doesNotContainNull();
    }

    @Test
    void getCertsByPlatformAndVersionsTest() {
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), null, VERSION_0);

        assertThat(certsAwsGlobal).isNotNull();
        assertThat(certsAwsGlobal).hasSize(SINGLE_CERT);
        assertThat(certsAwsGlobal).doesNotContainNull();
        verifyCertEntry(certsAwsGlobal, VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        Set<SslCertificateEntry> certsAwsAfs1 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_AFS1, VERSION_0);

        assertThat(certsAwsAfs1).isNotNull();
        assertThat(certsAwsAfs1).hasSize(SINGLE_CERT);
        assertThat(certsAwsAfs1).doesNotContainNull();
        verifyCertEntry(certsAwsAfs1, VERSION_0, CERT_ISSUER_AWS_AFS1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_AFS1_0);

        Set<SslCertificateEntry> certsAwsEus1 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_EUS1, VERSION_0);

        assertThat(certsAwsEus1).isNotNull();
        assertThat(certsAwsEus1).hasSize(SINGLE_CERT);
        assertThat(certsAwsEus1).doesNotContainNull();
        verifyCertEntry(certsAwsEus1, VERSION_0, CERT_ISSUER_AWS_EUS1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_EUS1_0);

        Set<SslCertificateEntry> certsAwsMes1 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_MES1, VERSION_0);

        assertThat(certsAwsMes1).isNotNull();
        assertThat(certsAwsMes1).hasSize(SINGLE_CERT);
        assertThat(certsAwsMes1).doesNotContainNull();
        verifyCertEntry(certsAwsMes1, VERSION_0, CERT_ISSUER_AWS_MES1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_MES1_0);

        Set<SslCertificateEntry> certsAwsApe1 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_APE1, VERSION_0);

        assertThat(certsAwsApe1).isNotNull();
        assertThat(certsAwsApe1).hasSize(SINGLE_CERT);
        assertThat(certsAwsApe1).doesNotContainNull();
        verifyCertEntry(certsAwsApe1, VERSION_0, CERT_ISSUER_AWS_APE1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_APE1_0);

        Set<SslCertificateEntry> certsAwsUsgw1 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_USGW1, VERSION_0,
                VERSION_1, VERSION_2, VERSION_3);

        Set<SslCertificateEntry> certsAwsApse3 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_APS3, VERSION_0);

        assertThat(certsAwsApse3).isNotNull();
        assertThat(certsAwsApse3).hasSize(SINGLE_CERT);
        assertThat(certsAwsApse3).doesNotContainNull();
        verifyCertEntry(certsAwsApse3, VERSION_0, CERT_ISSUER_AWS_APSE3_0, CLOUD_PROVIDER_IDENTIFIER_AWS_APSE3_0);

        assertThat(certsAwsUsgw1).isNotNull();
        assertThat(certsAwsUsgw1).hasSize(FOUR_CERTS);
        assertThat(certsAwsUsgw1).doesNotContainNull();
        verifyCertEntry(certsAwsUsgw1, VERSION_0, CERT_ISSUER_AWS_USGW1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_0);
        verifyCertEntry(certsAwsUsgw1, VERSION_1, CERT_ISSUER_AWS_USGW1_1, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_1);
        verifyCertEntry(certsAwsUsgw1, VERSION_2, CERT_ISSUER_AWS_USGW1_2, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_2);
        verifyCertEntry(certsAwsUsgw1, VERSION_3, CERT_ISSUER_AWS_USGW1_3, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_3);

        Set<SslCertificateEntry> certsAwsUsge1 = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_USGE1, VERSION_0,
                VERSION_1, VERSION_2, VERSION_3);

        assertThat(certsAwsUsge1).isNotNull();
        assertThat(certsAwsUsge1).hasSize(FOUR_CERTS);
        assertThat(certsAwsUsge1).doesNotContainNull();
        verifyCertEntry(certsAwsUsge1, VERSION_0, CERT_ISSUER_AWS_USGE1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_0);
        verifyCertEntry(certsAwsUsge1, VERSION_1, CERT_ISSUER_AWS_USGE1_1, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_1);
        verifyCertEntry(certsAwsUsge1, VERSION_2, CERT_ISSUER_AWS_USGE1_2, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_2);
        verifyCertEntry(certsAwsUsge1, VERSION_3, CERT_ISSUER_AWS_USGE1_3, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_3);

        Set<SslCertificateEntry> certsAwsDummy = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AWS.name(), REGION_DUMMY, VERSION_0);

        assertThat(certsAwsDummy).isNotNull();
        assertThat(certsAwsDummy).hasSize(SINGLE_CERT);
        assertThat(certsAwsDummy).doesNotContainNull();
        verifyCertEntry(certsAwsDummy, VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);

        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegionAndVersions(CloudPlatform.AZURE.name(), null, VERSION_0, VERSION_1);

        assertThat(certsAzure).isNotNull();
        assertThat(certsAzure).hasSize(TWO_CERTS);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_0, CERT_ISSUER_AZURE_0, CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(certsAzure, VERSION_1, CERT_ISSUER_AZURE_1, CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
    }

    @Test
    void getCertByPlatformAndVersionTest() {
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), null, VERSION_0), VERSION_0, CERT_ISSUER_AWS_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_AFS1, VERSION_0), VERSION_0, CERT_ISSUER_AWS_AFS1_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_AFS1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_EUS1, VERSION_0), VERSION_0, CERT_ISSUER_AWS_EUS1_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_EUS1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_MES1, VERSION_0), VERSION_0, CERT_ISSUER_AWS_MES1_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_MES1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_APE1, VERSION_0), VERSION_0, CERT_ISSUER_AWS_APE1_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_APE1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_EUS2, VERSION_0), VERSION_0, CERT_ISSUER_AWS_EUS2_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_EUS2_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGW1, VERSION_0), VERSION_0,
                CERT_ISSUER_AWS_USGW1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_APS3, VERSION_0), VERSION_0,
                CERT_ISSUER_AWS_APSE3_0, CLOUD_PROVIDER_IDENTIFIER_AWS_APSE3_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGW1, VERSION_1), VERSION_1,
                CERT_ISSUER_AWS_USGW1_1, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_1);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGW1, VERSION_2), VERSION_2,
                CERT_ISSUER_AWS_USGW1_2, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_2);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGW1, VERSION_3), VERSION_3,
                CERT_ISSUER_AWS_USGW1_3, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_3);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGE1, VERSION_0), VERSION_0,
                CERT_ISSUER_AWS_USGE1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGE1, VERSION_1), VERSION_1,
                CERT_ISSUER_AWS_USGE1_1, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_1);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGE1, VERSION_2), VERSION_2,
                CERT_ISSUER_AWS_USGE1_2, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_2);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_USGE1, VERSION_3), VERSION_3,
                CERT_ISSUER_AWS_USGE1_3, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_3);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AWS.name(), REGION_DUMMY, VERSION_0), VERSION_0, CERT_ISSUER_AWS_0,
                CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AZURE.name(), null, VERSION_0), VERSION_0, CERT_ISSUER_AZURE_0,
                CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndVersion(CloudPlatform.AZURE.name(), null, VERSION_1), VERSION_1, CERT_ISSUER_AZURE_1,
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
        assertThat(cert).isNotNull();
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
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), null, CLOUD_PROVIDER_IDENTIFIER_AWS_0),
                VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_AFS1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_AFS1_0),
                VERSION_0, CERT_ISSUER_AWS_AFS1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_AFS1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_EUS1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_EUS1_0),
                VERSION_0, CERT_ISSUER_AWS_EUS1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_EUS1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_MES1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_MES1_0),
                VERSION_0, CERT_ISSUER_AWS_MES1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_MES1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_APE1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_APE1_0),
                VERSION_0, CERT_ISSUER_AWS_APE1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_APE1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGW1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_0),
                VERSION_0, CERT_ISSUER_AWS_USGW1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_APS3,
                CLOUD_PROVIDER_IDENTIFIER_AWS_APSE3_0),
                VERSION_0, CERT_ISSUER_AWS_APSE3_0, CLOUD_PROVIDER_IDENTIFIER_AWS_APSE3_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGW1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_1),
                VERSION_1, CERT_ISSUER_AWS_USGW1_1, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_1);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGW1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_2),
                VERSION_2, CERT_ISSUER_AWS_USGW1_2, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_2);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGW1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_3),
                VERSION_3, CERT_ISSUER_AWS_USGW1_3, CLOUD_PROVIDER_IDENTIFIER_AWS_USGW1_3);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGE1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_0),
                VERSION_0, CERT_ISSUER_AWS_USGE1_0, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGE1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_1),
                VERSION_1, CERT_ISSUER_AWS_USGE1_1, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_1);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGE1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_2),
                VERSION_2, CERT_ISSUER_AWS_USGE1_2, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_2);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_USGE1,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_3),
                VERSION_3, CERT_ISSUER_AWS_USGE1_3, CLOUD_PROVIDER_IDENTIFIER_AWS_USGE1_3);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION_DUMMY,
                        CLOUD_PROVIDER_IDENTIFIER_AWS_0),
                VERSION_0, CERT_ISSUER_AWS_0, CLOUD_PROVIDER_IDENTIFIER_AWS_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), null,
                        CLOUD_PROVIDER_IDENTIFIER_AZURE_0),
                VERSION_0, CERT_ISSUER_AZURE_0, CLOUD_PROVIDER_IDENTIFIER_AZURE_0);
        verifyCertEntry(underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AZURE.name(), null,
                        CLOUD_PROVIDER_IDENTIFIER_AZURE_1),
                VERSION_1, CERT_ISSUER_AZURE_1, CLOUD_PROVIDER_IDENTIFIER_AZURE_1);
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
