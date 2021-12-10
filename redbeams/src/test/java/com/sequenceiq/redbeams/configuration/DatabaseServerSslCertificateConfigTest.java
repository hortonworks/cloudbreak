package com.sequenceiq.redbeams.configuration;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class DatabaseServerSslCertificateConfigTest {

    private static final String CERT_ENTRY_FORMAT = "%d:%s:%s";

    private static final String CERT_LIST_SEPARATOR = ";";

    private static final String CERT_PEM_1 =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIICjTCCAhSgAwIBAgIIdebfy8FoW6gwCgYIKoZIzj0EAwIwfDELMAkGA1UEBhMC\n" +
                    "VVMxDjAMBgNVBAgMBVRleGFzMRAwDgYDVQQHDAdIb3VzdG9uMRgwFgYDVQQKDA9T\n" +
                    "U0wgQ29ycG9yYXRpb24xMTAvBgNVBAMMKFNTTC5jb20gUm9vdCBDZXJ0aWZpY2F0\n" +
                    "aW9uIEF1dGhvcml0eSBFQ0MwHhcNMTYwMjEyMTgxNDAzWhcNNDEwMjEyMTgxNDAz\n" +
                    "WjB8MQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0\n" +
                    "b24xGDAWBgNVBAoMD1NTTCBDb3Jwb3JhdGlvbjExMC8GA1UEAwwoU1NMLmNvbSBS\n" +
                    "b290IENlcnRpZmljYXRpb24gQXV0aG9yaXR5IEVDQzB2MBAGByqGSM49AgEGBSuB\n" +
                    "BAAiA2IABEVuqVDEpiM2nl8ojRfLliJkP9x6jh3MCLOicSS6jkm5BBtHllirLZXI\n" +
                    "7Z4INcgn64mMU1jrYor+8FsPazFSY0E7ic3s7LaNGdM0B9y7xgZ/wkWV7Mt/qCPg\n" +
                    "CemB+vNH06NjMGEwHQYDVR0OBBYEFILRhXMw5zUE044CkvvlpNHEIejNMA8GA1Ud\n" +
                    "EwEB/wQFMAMBAf8wHwYDVR0jBBgwFoAUgtGFczDnNQTTjgKS++Wk0cQh6M0wDgYD\n" +
                    "VR0PAQH/BAQDAgGGMAoGCCqGSM49BAMCA2cAMGQCMG/n61kRpGDPYbCWe+0F+S8T\n" +
                    "kdzt5fxQaxFGRrMcIQBiu77D5+jNB5n5DQtdcj7EqgIwH7y6C+IwJPt8bYBVCpk+\n" +
                    "gA0z5Wajs6O7pdWLjwkspl1+4vAHCGht0nxpbl/f5Wpl\n" +
                    "-----END CERTIFICATE-----";

    private static final String CERT_ISSUER_1 = "CN=SSL.com Root Certification Authority ECC,O=SSL Corporation,L=Houston,ST=Texas,C=US";

    private static final String CLOUD_PROVIDER_IDENTIFIER_1 = "SSL.com-Root-Certification-Authority-ECC";

    private static final String CERT_PEM_2 =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFwTCCA6mgAwIBAgIITrIAZwwDXU8wDQYJKoZIhvcNAQEFBQAwSTELMAkGA1UE\n" +
                    "BhMCQ0gxFTATBgNVBAoTDFN3aXNzU2lnbiBBRzEjMCEGA1UEAxMaU3dpc3NTaWdu\n" +
                    "IFBsYXRpbnVtIENBIC0gRzIwHhcNMDYxMDI1MDgzNjAwWhcNMzYxMDI1MDgzNjAw\n" +
                    "WjBJMQswCQYDVQQGEwJDSDEVMBMGA1UEChMMU3dpc3NTaWduIEFHMSMwIQYDVQQD\n" +
                    "ExpTd2lzc1NpZ24gUGxhdGludW0gQ0EgLSBHMjCCAiIwDQYJKoZIhvcNAQEBBQAD\n" +
                    "ggIPADCCAgoCggIBAMrfogLi2vj8Bxax3mCq3pZcZB/HL37PZ/pEQtZ2Y5Wu669y\n" +
                    "IIpFR4ZieIbWIDkm9K6j/SPnpZy1IiEZtzeTIsBQnIJ71NUERFzLtMKfkr4k2Htn\n" +
                    "IuJpX+UFeNSH2XFwMyVTtIc7KZAoNppVRDBopIOXfw0enHb/FZ1glwCNioUD7IC+\n" +
                    "6ixuEFGSzH7VozPY1kneWCqv9hbrS3uQMpe5up1Y8fhXSQQeol0GcN1x2/ndi5ob\n" +
                    "jM89o03Oy3z2u5yg+gnOI2Ky6Q0f4nIoj5+saCB9bzuohTEJfwvH6GXp43gOCWcw\n" +
                    "izSC+13gzJ2BbWLuCB4ELE6b7P6pT1/9aXjvCR+htL/68++QHkwFix7qepF6w9fl\n" +
                    "+zC8bBsQWJj3Gl/QKTIDE0ZNYWqFTFJ0LwYfexHihJfGmfNtf9dng34TaNhxKFrY\n" +
                    "zt3oEBSa/m0jh26OWnA81Y0JAKeqvLAxN23IhBQeW71FYyBrS3SMvds6DsHPWhaP\n" +
                    "pZjydomyExI7C3d3rLvlPClKknLKYRorXkzig3R3+jVIeoVNjZpTxN94ypeRSCtF\n" +
                    "KwH3HBqi7Ri6Cr2D+m+8jVeTO9TUps4e8aCxzqv9KyiaTxvXw3LbpMS/XUz13XuW\n" +
                    "ae5ogObnmLo2t/5u7Su9IPhlGdpVCX4l3P5hYnL5fhgC72O00Puv5TtjjGePAgMB\n" +
                    "AAGjgawwgakwDgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0O\n" +
                    "BBYEFFCvzAeHFUdvOMW0ZdHelarp35zMMB8GA1UdIwQYMBaAFFCvzAeHFUdvOMW0\n" +
                    "ZdHelarp35zMMEYGA1UdIAQ/MD0wOwYJYIV0AVkBAQEBMC4wLAYIKwYBBQUHAgEW\n" +
                    "IGh0dHA6Ly9yZXBvc2l0b3J5LnN3aXNzc2lnbi5jb20vMA0GCSqGSIb3DQEBBQUA\n" +
                    "A4ICAQAIhab1Fgz8RBrBY+D5VUYI/HAcQiiWjrfFwUF1TglxeeVtlspLpYhg0DB0\n" +
                    "uMoI3LQwnkAHFmtllXcBrqS3NQuB2nEVqXQXOHtYyvkv+8Bldo1bAbl93oI9ZLi+\n" +
                    "FHSjClTTLJUYFzX1UWs/j6KWYTl4a0vlpqD4U99REJNi54Av4tHgvI42Rncz7Lj7\n" +
                    "jposiU0xEQ8mngS7twSNC/K5/FqdOxa3L8iYq/6KUFkuozv8KV2LwUvJ4ooTHbG/\n" +
                    "u0IdUt1O2BReEMYxB+9xJ/cbOQncguqLs5WGXv312l0xpuAxtpTmREl0xRbl9x8D\n" +
                    "YSjFyMsSoEJL+WuICI20MhjzdZ/EfwBPBZWcoxcCw7NTm6ogOSkrZvqdr16zktK1\n" +
                    "puEa+S1BaYEUtLS17Yk9zvupnTVCRLEcFHOBzyoBNZox1S2PbYTfgE1X4z/FhHXa\n" +
                    "icYwu+uPyyIIoK6q8QNsOktNCaUOcsZWayFCTiMlFGiudgp8DAdwZPmaL/YFOSbG\n" +
                    "DI8Zf0NebvRbFS/bYV3mZy8/CJT5YLSYMdp08YSTcU1f+2BY0fvEwW2JorsgH51x\n" +
                    "kcsymxM9Pn2SUjWskpSi0xjCfMfqr3YFFt1nJ8J+HAciIfNAChs0B0QTwoRqjt8Z\n" +
                    "Wr9/6x3iGjjRXK9HkmuAtTClyY3YqzGBH9/CZjfTk6mFhnll0g==\n" +
                    "-----END CERTIFICATE-----";

    private static final String CERT_ISSUER_2 = "CN=SwissSign Platinum CA - G2,O=SwissSign AG,C=CH";

    private static final String CLOUD_PROVIDER_IDENTIFIER_2 = "SwissSign-Platinum-CA-G2";

    private static final String CERT_PEM_3 =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIICaTCCAe+gAwIBAgIQISpWDK7aDKtARb8roi066jAKBggqhkjOPQQDAzBtMQsw\n" +
                    "CQYDVQQGEwJDSDEQMA4GA1UEChMHV0lTZUtleTEiMCAGA1UECxMZT0lTVEUgRm91\n" +
                    "bmRhdGlvbiBFbmRvcnNlZDEoMCYGA1UEAxMfT0lTVEUgV0lTZUtleSBHbG9iYWwg\n" +
                    "Um9vdCBHQyBDQTAeFw0xNzA1MDkwOTQ4MzRaFw00MjA1MDkwOTU4MzNaMG0xCzAJ\n" +
                    "BgNVBAYTAkNIMRAwDgYDVQQKEwdXSVNlS2V5MSIwIAYDVQQLExlPSVNURSBGb3Vu\n" +
                    "ZGF0aW9uIEVuZG9yc2VkMSgwJgYDVQQDEx9PSVNURSBXSVNlS2V5IEdsb2JhbCBS\n" +
                    "b290IEdDIENBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAETOlQwMYPchi82PG6s4ni\n" +
                    "eUqjFqdrVCTbUf/q9Akkwwsin8tqJ4KBDdLArzHkdIJuyiXZjHWd8dvQmqJLIX4W\n" +
                    "p2OQ0jnUsYd4XxiWD1AbNTcPasbc2RNNpI6QN+a9WzGRo1QwUjAOBgNVHQ8BAf8E\n" +
                    "BAMCAQYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUSIcUrOPDnpBgOtfKie7T\n" +
                    "rYy0UGYwEAYJKwYBBAGCNxUBBAMCAQAwCgYIKoZIzj0EAwMDaAAwZQIwJsdpW9zV\n" +
                    "57LnyAyMjMPdeYwbY9XJUpROTYJKcx6ygISpJcBMWm1JKWB4E+J+SOtkAjEA2zQg\n" +
                    "Mgj/mkkCtojeFK9dbJlxjRo/i9fgojaGHAeCOnZT/cKi7e97sIBPWA9LUzm9\n" +
                    "-----END CERTIFICATE-----";

    private static final String CERT_ISSUER_3 = "CN=OISTE WISeKey Global Root GC CA,OU=OISTE Foundation Endorsed,O=WISeKey,C=CH";

    private static final String CLOUD_PROVIDER_IDENTIFIER_3 = "OISTE-WISeKey-Global-Root-GC-CA";

    private static final int VERSION_0 = 0;

    private static final int VERSION_1 = 1;

    private static final int VERSION_2 = 2;

    private static final int NO_CERTS = 0;

    private static final int DUMMY_VERSION = Integer.MIN_VALUE;

    private static final String RDS_CA_2019 = "rds-ca-2019";

    private static final String DIGI_CERT_GLOBAL_ROOT_G_2 = "DigiCertGlobalRootG2";

    private static final String LONG_CLOUD_PROVIDER_IDENTIFIER =
            "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
                    "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
                    "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

    private static final String REGION_1 = "myRegion1";

    private static final String REGION_2 = "myRegion2";

    private DatabaseServerSslCertificateConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseServerSslCertificateConfig();
    }

    private static String certEntry(int version, String cloudProviderIdentifier, String certPem) {
        return String.format(CERT_ENTRY_FORMAT, version, cloudProviderIdentifier, certPem);
    }

    private static String certList(String... certEntries) {
        return String.join(CERT_LIST_SEPARATOR, certEntries);
    }

    @Test
    void configReadTestWhenEmptyConfigShouldReturnZeroCerts() {
        Map<String, String> certs = Map.of();
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of());

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
    }

    @Test
    void configReadTestWhenAzureHasOneCertShouldReturnOneCert() {
        Map<String, String> certs = Map.of("azure", certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("azure"));

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(1);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
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
    }

    private void initCerts(Map<String, String> certs) {
        ReflectionTestUtils.setField(underTest, "certs", certs);

        underTest.setupCertsCache();
    }

    private void verifyCertStatistics(String cloudPlatform, String region, int numCerts, int minVersion, int maxVersion) {
        assertThat(underTest.getNumberOfCertsByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(numCerts);
        assertThat(underTest.getMinVersionByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(minVersion);
        assertThat(underTest.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(maxVersion);
    }

    private void verifyCertEntry(Set<SslCertificateEntry> certs, int version, String cloudProviderIdentifierExpected, String certPemExpected,
            String certIssuerExpected) {
        Optional<SslCertificateEntry> match = certs.stream()
                .filter(e -> e.getVersion() == version)
                .findFirst();
        assertThat(match).overridingErrorMessage("No cert found for version %d", version).isPresent();

        SslCertificateEntry sslCertificateEntry = match.get();
        assertThat(sslCertificateEntry.getCloudProviderIdentifier()).isEqualTo(cloudProviderIdentifierExpected);
        assertThat(sslCertificateEntry.getCertPem()).isEqualTo(certPemExpected);

        X509Certificate x509Certificate = sslCertificateEntry.getX509Cert();
        assertThat(x509Certificate).isNotNull();

        X500Principal issuerX500Principal = x509Certificate.getIssuerX500Principal();
        assertThat(issuerX500Principal).isNotNull();
        assertThat(issuerX500Principal.getName()).isEqualTo(certIssuerExpected);
    }

    @Test
    void configReadTestWhenAwsIsDummyAzureHasOneCertShouldReturnOneCert() {
        Map<String, String> certs = Map.of("aws", "", "azure", certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("azure"));

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(1);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
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
    }

    @Test
    void configReadTestWhenAwsHasTwoCertsShouldReturnTwoCerts() {
        Map<String, String> certs = Map.of("aws", certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2),
                certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws"));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAwsGlobal).hasSize(2);
        assertThat(certsAwsGlobal).doesNotContainNull();
        verifyCertEntry(certsAwsGlobal, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAwsGlobal, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_1, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsRegion1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_1);
        assertThat(certsAwsRegion1).hasSize(2);
        assertThat(certsAwsRegion1).doesNotContainNull();
        verifyCertEntry(certsAwsRegion1, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAwsRegion1, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", REGION_1)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", REGION_1)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_2, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsRegion2 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_2);
        assertThat(certsAwsRegion2).hasSize(2);
        assertThat(certsAwsRegion2).doesNotContainNull();
        verifyCertEntry(certsAwsRegion2, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAwsRegion2, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
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
    }

    @Test
    void configReadTestWhenAwsHasTwoGlobalCertsAndOneRegionalCertShouldReturnThreeCerts() {
        Map<String, String> certs = Map.ofEntries(
                entry("aws",
                        certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3))),
                entry("aws." + REGION_1,
                        certList(certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1)))
        );
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "aws." + REGION_1.toLowerCase()));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsGlobal = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAwsGlobal).hasSize(2);
        assertThat(certsAwsGlobal).doesNotContainNull();
        verifyCertEntry(certsAwsGlobal, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAwsGlobal, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(2);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_1, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAwsRegion1 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_1);
        assertThat(certsAwsRegion1).hasSize(1);
        assertThat(certsAwsRegion1).doesNotContainNull();
        verifyCertEntry(certsAwsRegion1, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", REGION_1)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", REGION_1)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AWS.name(), REGION_2, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAwsRegion2 = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), REGION_2);
        assertThat(certsAwsRegion2).hasSize(2);
        assertThat(certsAwsRegion2).doesNotContainNull();
        verifyCertEntry(certsAwsRegion2, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAwsRegion2, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
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
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsShouldReturnThreeCerts() {
        Map<String, String> certs = Map.of("aws", certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                "azure", certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2),
                        certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        verifyCertEntry(certsAws, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAzure, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsAndUppercaseIdentifiersShouldReturnThreeCerts() {
        Map<String, String> certs = Map.of("AWS", certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                "AZURE", certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2),
                        certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        verifyCertEntry(certsAws, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAzure, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();
    }

    @Test
    void configReadTestWhenAwsHasOneCertCommonWithAzureShouldReturnThreeCerts() {
        Map<String, String> certs = Map.of("aws", certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                "azure", certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                        certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        verifyCertEntry(certsAws, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        verifyCertEntry(certsAzure, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsWithOneCommonVersionShouldReturnThreeCerts() {
        Map<String, String> certs = Map.of("aws", certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                "azure", certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2),
                        certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_1, VERSION_1);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        verifyCertEntry(certsAws, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAzure, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();
    }

    @Test
    void configReadTestWhenAwsHasOneCertAndAzureHasTwoCertsWithOneCommonCloudProviderIdentifierShouldReturnThreeCerts() {
        Map<String, String> certs = Map.of("aws", certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                "azure", certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_2),
                        certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));
        initCerts(certs);

        assertThat(underTest.getCerts()).isEqualTo(certs);
        assertThat(underTest.getSupportedPlatformsForCerts()).isEqualTo(Set.of("aws", "azure"));

        verifyCertStatistics(CloudPlatform.AWS.name(), null, 1, VERSION_0, VERSION_0);
        Set<SslCertificateEntry> certsAws = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AWS.name(), null);
        assertThat(certsAws).hasSize(1);
        assertThat(certsAws).doesNotContainNull();
        verifyCertEntry(certsAws, VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1, CERT_ISSUER_1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aws", null)).hasSize(1);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Aws", null)).hasSize(1);

        verifyCertStatistics(CloudPlatform.AZURE.name(), null, 2, VERSION_1, VERSION_2);
        Set<SslCertificateEntry> certsAzure = underTest.getCertsByCloudPlatformAndRegion(CloudPlatform.AZURE.name(), null);
        assertThat(certsAzure).hasSize(2);
        assertThat(certsAzure).doesNotContainNull();
        verifyCertEntry(certsAzure, VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_2, CERT_ISSUER_2);
        verifyCertEntry(certsAzure, VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3, CERT_ISSUER_3);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("Azure", null)).hasSize(2);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("aZure", null)).hasSize(2);

        verifyCertStatistics("", null, NO_CERTS, DUMMY_VERSION, DUMMY_VERSION);
        assertThat(underTest.getCertsByCloudPlatformAndRegion("", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion("cloud", null)).isEmpty();
        assertThat(underTest.getCertsByCloudPlatformAndRegion(null, null)).isEmpty();
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedCloudPlatformGarbage() {
        Map<String, String> certs = Map.of("$uper", certList(certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed cloud platform \"$uper (malformed)\".");
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedCloudPlatformMultipleRegions() {
        Map<String, String> certs = Map.of("aws.region1.region2", certList(certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed cloud platform \"aws.region1.region2 (malformed)\".");
    }

    @Test
    void setupCertsCacheTestWhenErrorUnsupportedCloudPlatform() {
        Map<String, String> certs = Map.of("gcp", certList(certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed cloud platform \"gcp (malformed)\".");
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedEntryGarbage() {
        Map<String, String> certs = Map.of("aws", certList(certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), "broken-entry"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed SSL certificate entry for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedEntryGarbageRegional() {
        Map<String, String> certs = Map.of("aws.region1", certList(certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), "broken-entry"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed SSL certificate entry for cloud platform \"aws (region region1)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedEntryExtraField() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1),
                        certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2) + ":foo"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed SSL certificate entry for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorEmptyEntry() {
        Map<String, String> certs = Map.of("aws", certList("", certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Malformed SSL certificate entry for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedCloudProviderIdentifier() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1), certEntry(VERSION_2, LONG_CLOUD_PROVIDER_IDENTIFIER, CERT_PEM_2)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException)
                .hasMessageStartingWith("Malformed SSL certificate CloudProviderIdentifier for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorMalformedPem() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, "broken-pem")));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Error parsing SSL certificate PEM for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorDuplicatedVersion() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessage("Duplicated SSL certificate version 2 for cloud platform \"aws (global)\"");
    }

    @Test
    void setupCertsCacheTestWhenErrorDuplicatedCloudProviderIdentifier() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_3)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException)
                .hasMessage("Duplicated SSL certificate CloudProviderIdentifier for cloud platform \"aws (global)\": \"SwissSign-Platinum-CA-G2\"");
    }

    @Test
    void setupCertsCacheTestWhenErrorDuplicatedPem() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_2)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessageStartingWith("Duplicated SSL certificate PEM for cloud platform \"aws (global)\": \"");
    }

    @Test
    void setupCertsCacheTestWhenErrorBadVersionRange() {
        Map<String, String> certs = Map.of("aws",
                certList(certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3)));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> initCerts(certs));
        assertThat(illegalArgumentException).hasMessage("SSL certificate versions are not contiguous for cloud platform \"aws (global)\"");
    }

    static Object[][] getLegacyMaxVersionByPlatformDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region legacyMaxVersionExpected
                {"null, null", null, null, DUMMY_VERSION},
                {"empty, null", "", null, DUMMY_VERSION},
                {"AWS, null", CloudPlatform.AWS.name(), null, 0},
                {"AWS, eu-south-1", CloudPlatform.AWS.name(), "eu-south-1", 0},
                {"AWS, af-south-1", CloudPlatform.AWS.name(), "af-south-1", 0},
                {"AWS, me-south-1", CloudPlatform.AWS.name(), "me-south-1", 0},
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
    @MethodSource("getLegacyMaxVersionByPlatformDataProvider")
    void getLegacyMaxVersionByPlatformTest(String testCaseName, String cloudPlatform, String region, int legacyMaxVersionExpected) {
        assertThat(underTest.getLegacyMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(legacyMaxVersionExpected);
    }

    static Object[][] getLegacyCloudProviderIdentifierByPlatformDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform region legacyCloudProviderIdentifierExpected
                {"null, null", null, null, null},
                {"empty, null", "", null, null},
                {"AWS, null", CloudPlatform.AWS.name(), null, RDS_CA_2019},
                {"AWS, eu-south-1", CloudPlatform.AWS.name(), "eu-south-1", "rds-ca-2019-eu-south-1"},
                {"AWS, af-south-1", CloudPlatform.AWS.name(), "af-south-1", "rds-ca-2019-af-south-1"},
                {"AWS, me-south-1", CloudPlatform.AWS.name(), "me-south-1", "rds-ca-2019-me-south-1"},
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
    @MethodSource("getLegacyCloudProviderIdentifierByPlatformDataProvider")
    void getLegacyCloudProviderIdentifierByPlatformTest(String testCaseName, String cloudPlatform, String region,
            String legacyCloudProviderIdentifierExpected) {
        assertThat(underTest.getLegacyCloudProviderIdentifierByCloudPlatformAndRegion(cloudPlatform, region)).isEqualTo(legacyCloudProviderIdentifierExpected);
    }

    @Test
    void getSupportedPlatformsForLegacyMaxVersionTest() {
        assertThat(underTest.getSupportedPlatformsForLegacyMaxVersion())
                .isEqualTo(Set.of("aws", "aws.eu-south-1", "aws.af-south-1", "aws.me-south-1", "azure"));
    }

    @Test
    void getSupportedPlatformsForLegacyCloudProviderIdentifierTest() {
        assertThat(underTest.getSupportedPlatformsForLegacyCloudProviderIdentifier())
                .isEqualTo(Set.of("aws", "aws.eu-south-1", "aws.af-south-1", "aws.me-south-1", "azure"));
    }

    static Object[][] getCertByPlatformAndVersionDataProvider() {
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
    @MethodSource("getCertByPlatformAndVersionDataProvider")
    void getCertByPlatformAndVersionTest(String testCaseName, String cloudPlatform, String region, int version, boolean certFoundExpected) {
        Map<String, String> certs = Map.ofEntries(
                entry("aws",
                        certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3))),
                entry("aws." + REGION_1,
                        certList(certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1)))
        );
        initCerts(certs);

        SslCertificateEntry result = underTest.getCertByCloudPlatformAndRegionAndVersion(cloudPlatform, region, version);
        if (certFoundExpected) {
            assertThat(result).isNotNull();
            assertThat(result.getVersion()).isEqualTo(version);
        } else {
            assertThat(result).isNull();
        }
    }

    static Object[][] getCertsByPlatformAndVersionsDataProvider() {
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
    @MethodSource("getCertsByPlatformAndVersionsDataProvider")
    void getCertsByPlatformAndVersionsTest(String testCaseName, String cloudPlatform, String region, int[] versions, Set<Integer> versionsExpected) {
        Map<String, String> certs = Map.ofEntries(
                entry("aws",
                        certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3))),
                entry("aws." + REGION_1,
                        certList(certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1)))
        );
        initCerts(certs);

        Set<SslCertificateEntry> result = underTest.getCertsByCloudPlatformAndRegionAndVersions(cloudPlatform, region, versions);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContainNull();
        Set<Integer> versionsFound = result.stream()
                .map(SslCertificateEntry::getVersion)
                .collect(Collectors.toSet());
        assertThat(versionsFound).containsExactlyInAnyOrderElementsOf(versionsExpected);
    }

    static Object[][] getCertByPlatformAndCloudProviderIdentifierDataProvider() {
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
    @MethodSource("getCertByPlatformAndCloudProviderIdentifierDataProvider")
    void getCertByPlatformAndCloudProviderIdentifierTest(String testCaseName, String cloudPlatform, String region, String cloudProviderIdentifier,
            boolean certFoundExpected) {
        Map<String, String> certs = Map.ofEntries(
                entry("aws",
                        certList(certEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_2, CERT_PEM_2), certEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_3, CERT_PEM_3))),
                entry("aws." + REGION_1,
                        certList(certEntry(VERSION_0, CLOUD_PROVIDER_IDENTIFIER_1, CERT_PEM_1)))
        );
        initCerts(certs);

        SslCertificateEntry result = underTest.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(cloudPlatform, region, cloudProviderIdentifier);
        if (certFoundExpected) {
            assertThat(result).isNotNull();
            assertThat(result.getCloudProviderIdentifier()).isEqualTo(cloudProviderIdentifier);
        } else {
            assertThat(result).isNull();
        }
    }

}