package com.sequenceiq.redbeams.service.sslcertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.repository.SslConfigRepository;

@ExtendWith(MockitoExtension.class)
class SslConfigServiceTest {

    private static final String FIELD_SSL_ENABLED = "sslEnabled";

    private static final String CLOUDPLATFORM = CloudPlatform.MOCK.name();

    private static final String REGION = "default";

    private static final int MAX_VERSION = 3;

    private static final int VERSION_1 = 1;

    private static final int VERSION_2 = 2;

    private static final int VERSION_3 = 3;

    private static final String CLOUD_PROVIDER_IDENTIFIER_V1 = "cert-id-1";

    private static final String CLOUD_PROVIDER_IDENTIFIER_V2 = "cert-id-2";

    private static final String CLOUD_PROVIDER_IDENTIFIER_V3 = "cert-id-3";

    private static final int SINGLE_CERT = 1;

    private static final int TWO_CERTS = 2;

    private static final int THREE_CERTS = 3;

    private static final String CERT_PEM_V1 = "super-cert-1";

    private static final String CERT_PEM_V2 = "super-cert-2";

    private static final String CERT_PEM_V3 = "super-cert-3";

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private SslConfigRepository repository;

    @Mock
    private X509Certificate x509Certificate;

    private DBStack dbStack;

    private SslCertificateEntry sslCertificateEntryV2;

    private SslCertificateEntry sslCertificateEntryV3;

    @InjectMocks
    private SslConfigService underTest;

    @BeforeEach
    void init() {
        dbStack = new DBStack();
        dbStack.setCloudPlatform(CLOUDPLATFORM);
        dbStack.setRegion(REGION);

        sslCertificateEntryV2 = new SslCertificateEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_V2,
                CLOUD_PROVIDER_IDENTIFIER_V2, CLOUDPLATFORM.toLowerCase(Locale.ROOT), CERT_PEM_V2, x509Certificate);
        sslCertificateEntryV3 = new SslCertificateEntry(VERSION_3, CLOUD_PROVIDER_IDENTIFIER_V3,
                CLOUD_PROVIDER_IDENTIFIER_V3, CLOUDPLATFORM.toLowerCase(Locale.ROOT), CERT_PEM_V3, x509Certificate);

        lenient().when(repository.save(any(SslConfig.class))).thenAnswer(invocation -> invocation.getArgument(0, SslConfig.class));
        lenient().when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(eq(CLOUDPLATFORM), eq(REGION))).thenReturn(MAX_VERSION);
        ReflectionTestUtils.setField(underTest, FIELD_SSL_ENABLED, true);
    }

    static Object[][] conversionTestWhenSslDisabledDataProvider() {
        return new Object[][]{
                // testCaseName fieldSslEnabled sslConfigV4Request
                {"false, null", false, null},
                {"true, null", true, null},
                {"false, request with sslMode=null", false, new SslConfigV4Request()},
                {"true, request with sslMode=null", true, new SslConfigV4Request()},
                {"false, request with sslMode=DISABLED", false, createSslConfigV4Request(SslMode.DISABLED)},
                {"true, request with sslMode=DISABLED", true, createSslConfigV4Request(SslMode.DISABLED)},
                {"false, request with sslMode=ENABLED", false, createSslConfigV4Request(SslMode.ENABLED)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conversionTestWhenSslDisabledDataProvider")
    void conversionTestWhenSslDisabled(String testCaseName, boolean fieldSslEnabled, SslConfigV4Request sslConfigV4Request) {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(sslConfigV4Request);
        ReflectionTestUtils.setField(underTest, FIELD_SSL_ENABLED, fieldSslEnabled);

        SslConfig sslConfig = underTest.createSslConfig(request, dbStack);

        assertThat(sslConfig).isNotNull();
        Set<String> sslCertificates = sslConfig.getSslCertificates();
        assertThat(sslCertificates).isNotNull();
        assertThat(sslCertificates).isEmpty();
        assertThat(sslConfig.getSslCertificateType()).isEqualTo(SslCertificateType.NONE);
    }

    private static AllocateDatabaseServerV4Request createAllocateDbRequestWithSSlConfigRequest(SslConfigV4Request sslConfigV4Request) {
        AllocateDatabaseServerV4Request request = new AllocateDatabaseServerV4Request();
        request.setSslConfig(sslConfigV4Request);
        return request;
    }

    private static SslConfigV4Request createSslConfigV4Request(SslMode sslMode) {
        SslConfigV4Request sslConfigV4Request = new SslConfigV4Request();
        sslConfigV4Request.setSslMode(sslMode);
        return sslConfigV4Request;
    }

    @Test
    void conversionTestWhenSslEnabledAndNoCerts() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));
        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(0);

        SslConfig sslConfig = underTest.createSslConfig(request, dbStack);

        verifySsl(sslConfig, Set.of(), null);
        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegion(anyString(), anyString());
    }

    private void verifySsl(SslConfig sslConfig, Set<String> sslCertificatesExpected, String cloudProviderIdentifierExpected) {
        assertThat(sslConfig).isNotNull();
        Set<String> sslCertificates = sslConfig.getSslCertificates();
        assertThat(sslCertificates).isNotNull();
        assertThat(sslCertificates).isEqualTo(sslCertificatesExpected);
        assertThat(sslConfig.getSslCertificateType()).isEqualTo(SslCertificateType.CLOUD_PROVIDER_OWNED);
        assertThat(sslConfig.getSslCertificateActiveVersion()).isEqualTo(MAX_VERSION);
        assertThat(sslConfig.getSslCertificateActiveCloudProviderIdentifier()).isEqualTo(cloudProviderIdentifierExpected);
        verify(repository).save(sslConfig);
    }

    @Test
    void conversionTestWhenSslEnabledAndSingleCert() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(SINGLE_CERT);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(Set.of(sslCertificateEntryV3));

        SslConfig sslConfig = underTest.createSslConfig(request, dbStack);

        verifySsl(sslConfig, Set.of(CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);
    }

    @Test
    void conversionTestWhenSslEnabledAndSingleCertErrorBlankCloudProviderIdentifier() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(SINGLE_CERT);
        SslCertificateEntry sslCertificateEntryV3Broken = new SslCertificateEntry(
                VERSION_3,
                CERT_PEM_V3,
                "",
                CLOUDPLATFORM.toLowerCase(Locale.ROOT),
                CERT_PEM_V3,
                x509Certificate);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV3Broken));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.createSslConfig(request, dbStack));
        assertThat(illegalStateException)
                .hasMessage("Blank CloudProviderIdentifier in SSL certificate version 3 for cloud platform \""
                        + CLOUDPLATFORM + "\" and region \"" + REGION + "\"");
        verifyNoInteractions(repository);
    }

    @Test
    void conversionTestWhenSslEnabledAndSingleCertErrorBlankPem() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(SINGLE_CERT);
        SslCertificateEntry sslCertificateEntryV3Broken = new SslCertificateEntry(
                VERSION_3,
                CLOUD_PROVIDER_IDENTIFIER_V3,
                CLOUD_PROVIDER_IDENTIFIER_V3,
                CLOUDPLATFORM.toLowerCase(Locale.ROOT),
                "",
                x509Certificate);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV3Broken));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.createSslConfig(request, dbStack));
        assertThat(illegalStateException)
                .hasMessage("Blank PEM in SSL certificate version 3 for cloud platform \"" + CLOUDPLATFORM + "\" and region \"" + REGION + "\"");
        verifyNoInteractions(repository);
    }

    @Test
    void conversionTestWhenSslEnabledAndSingleCertReturnedInternal() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));
        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(SINGLE_CERT);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(Set.of(sslCertificateEntryV3));

        SslConfig sslConfig = underTest.createSslConfig(request, dbStack);

        verifySsl(sslConfig, Set.of(CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);
    }

    @Test
    void conversionTestWhenSslEnabledAndTwoCerts() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));
        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV2, sslCertificateEntryV3));

        SslConfig sslConfig = underTest.createSslConfig(request, dbStack);

        verifySsl(sslConfig, Set.of(CERT_PEM_V2, CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);
    }

    @Test
    void conversionTestWhenSslEnabledAndThreeCertsReturnedInternal() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));
        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(THREE_CERTS);
        SslCertificateEntry sslCertificateEntryV1 = new SslCertificateEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_V1,
                CLOUD_PROVIDER_IDENTIFIER_V1, CLOUDPLATFORM.toLowerCase(Locale.ROOT), CERT_PEM_V1, x509Certificate);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV1, sslCertificateEntryV2, sslCertificateEntryV3));

        SslConfig sslConfig = underTest.createSslConfig(request, dbStack);

        verifySsl(sslConfig, Set.of(CERT_PEM_V1, CERT_PEM_V2, CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);
    }

    @Test
    void conversionTestWhenSslEnabledAndTwoCertsErrorNullCert() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(TWO_CERTS);

        Set<SslCertificateEntry> certs = new HashSet<>();
        certs.add(sslCertificateEntryV3);
        certs.add(null);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(certs);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.createSslConfig(request, dbStack));
        assertThat(illegalStateException)
                .hasMessage("SSL certificate count mismatch for cloud platform \"" + CLOUDPLATFORM + "\" and region \"" + REGION + "\": expected=2, actual=1");
        verifyNoInteractions(repository);
    }

    @Test
    void conversionTestWhenSslEnabledAndTwoCertsErrorFewerCerts() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV3));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.createSslConfig(request, dbStack));
        assertThat(illegalStateException)
                .hasMessage("SSL certificate count mismatch for cloud platform \"" + CLOUDPLATFORM + "\" and region \"" + REGION + "\": expected=2, actual=1");
        verifyNoInteractions(repository);
    }

    @Test
    void conversionTestWhenSslEnabledAndTwoCertsErrorDuplicatedCertPem() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        SslCertificateEntry sslCertificateEntryV2DuplicateOfV3 = new SslCertificateEntry(
                VERSION_2,
                CLOUD_PROVIDER_IDENTIFIER_V3,
                CLOUD_PROVIDER_IDENTIFIER_V3,
                CLOUDPLATFORM.toLowerCase(Locale.ROOT),
                CERT_PEM_V3,
                x509Certificate);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV2DuplicateOfV3, sslCertificateEntryV3));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.createSslConfig(request, dbStack));
        assertThat(illegalStateException)
                .hasMessage("Duplicated SSL certificate PEM for cloud platform \"" + CLOUDPLATFORM + "\" and region \""
                        + REGION + "\". Unique count: expected=2, actual=1");
        verifyNoInteractions(repository);
    }

    @Test
    void conversionTestWhenSslEnabledAndTwoCertsErrorVersionMismatch() {
        AllocateDatabaseServerV4Request request = createAllocateDbRequestWithSSlConfigRequest(createSslConfigV4Request(SslMode.ENABLED));

        SslCertificateEntry sslCertificateEntryV2Broken = new SslCertificateEntry(
                VERSION_1,
                CLOUD_PROVIDER_IDENTIFIER_V2,
                CLOUD_PROVIDER_IDENTIFIER_V2,
                CLOUDPLATFORM.toLowerCase(Locale.ROOT),
                CERT_PEM_V2,
                x509Certificate);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(CLOUDPLATFORM, REGION))
                .thenReturn(Set.of(sslCertificateEntryV2Broken, sslCertificateEntryV3));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.createSslConfig(request, dbStack));
        assertThat(illegalStateException)
                .hasMessage("Could not find SSL certificate version 2 for cloud platform \"" + CLOUDPLATFORM + "\" and region \"" + REGION + "\"");
        verifyNoInteractions(repository);
    }
}