package com.sequenceiq.redbeams.service.sslcertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@ExtendWith(MockitoExtension.class)
class DatabaseServerSslCertificateSyncServiceTest {

    private static final String REGION = "myRegion";

    private static final String CERT_ID_1 = "certID-1";

    private static final String CERT_ID_2 = "certID-2";

    private static final String CERT_ID_3 = "certID-3";

    private static final String AWS = "aws";

    private static final int CERT_VERSION = 789;

    private static final int CERT_VERSION_OTHER = 456;

    private static final String CERT_PEM = "cert-PEM";

    private static final String CERT_PEM_OTHER = "cert-PEM-other";

    private static final Long SSL_CONF_ID = 16L;

    @Mock
    private SslConfigService sslConfigService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @InjectMocks
    private DatabaseServerSslCertificateSyncService underTest;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Captor
    private ArgumentCaptor<SslConfig> sslConfigArgumentCaptor;

    @Mock
    private X509Certificate x509Cert;

    private DatabaseStack databaseStack;

    @BeforeEach
    void setUp() {
        databaseStack = new DatabaseStack(null, null, Map.of(), "");
    }

    @Test
    void syncSslCertificateIfNeededTestWhenSuccessNoSsl() throws Exception {
        DBStack dbStack = getDBStack();
        dbStack.setCloudPlatform(CloudPlatform.AZURE.toString());

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(dbStack.getSslConfig()).isNull();
    }

    @ParameterizedTest(name = "sslCertificateType={0}")
    @NullSource
    @EnumSource(value = SslCertificateType.class, names = {"NONE", "BRING_YOUR_OWN"})
    void syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateType(SslCertificateType sslCertificateType) throws Exception {
        syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeOrBadCloudPlatformInternal(sslCertificateType, CloudPlatform.AWS.name());
    }

    void syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType sslCertificateType, String cloudPlatform)
            throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(cloudPlatform);
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(sslCertificateType, CERT_ID_1));

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(anyString(), anyString(), anyString());
        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "GCP"}, mode = EnumSource.Mode.EXCLUDE)
    void syncSslCertificateIfNeededTestWhenSuccessSslBadCloudPlatform(CloudPlatform cloudPlatform) throws Exception {
        syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType.CLOUD_PROVIDER_OWNED,
                cloudPlatform.name());
    }

    @Test
    void syncSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedActiveSslRootCertificateQueryError() throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        setupCloudConnectorMock();
        Exception e = new Exception("Foo");
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack)).thenThrow(e);

        Exception exceptionResult =
                assertThrows(Exception.class, () -> underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(exceptionResult).isSameAs(e);

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(anyString(), anyString(), anyString());
        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    private void setupCloudConnectorMock() {
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    @Test
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedNullActiveSslRootCertificate() throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack)).thenReturn(null);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(anyString(), anyString(), anyString());
        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @Test
    void testSyncSslCertificateIfNeededForGcpReturnsValidRootCert() throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.GCP.name());
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, null));

        setupCloudConnectorMock();
        CloudDatabaseServerSslCertificate certificate = new CloudDatabaseServerSslCertificate(
                CloudDatabaseServerSslCertificateType.ROOT,
                CERT_ID_2,
                CERT_PEM,
                new Date().getTime());
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack)).thenReturn(certificate);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(0, Set.of(CERT_PEM), certificate.expirationDate());
    }

    @Test
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMatchingActiveSslRootCertificate() throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_1));

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(anyString(), anyString(), anyString());
        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateUnknown(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(null);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(null, Set.of(), null);
    }

    private void verifySslConfigCaptured(Integer sslCertificateVersionExpected, Set<String> sslCertificatesExpected,
        Long expirationDate) {
        SslConfig sslConfigCaptured = sslConfigArgumentCaptor.getValue();
        assertThat(sslConfigCaptured).isNotNull();
        assertThat(sslConfigCaptured.getSslCertificateType()).isEqualTo(SslCertificateType.CLOUD_PROVIDER_OWNED);
        assertThat(sslConfigCaptured.getSslCertificateActiveCloudProviderIdentifier()).isEqualTo(CERT_ID_2);
        assertThat(sslConfigCaptured.getSslCertificateActiveVersion()).isEqualTo(sslCertificateVersionExpected);
        assertThat(sslConfigCaptured.getSslCertificates()).isEqualTo(sslCertificatesExpected);
        if (expirationDate != null) {
            assertThat(sslConfigCaptured.getSslCertificateExpirationDate()).isEqualTo(expirationDate);
        }
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateUnknownBogusDesiredVersionAndPem(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier, Set.of(CERT_PEM_OTHER),
                        CERT_VERSION_OTHER));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(null);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(CERT_VERSION_OTHER, Set.of(CERT_PEM_OTHER), null);
    }

    @Test
    void syncSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateInvalidCertificateEntryCloudProviderIdMismatch()
            throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_3, CERT_ID_3, AWS, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        IllegalStateException illegalStateException =
                assertThrows(IllegalStateException.class, () -> underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(illegalStateException).hasMessage(
                String.format("SSL certificate CloudProviderIdentifier mismatch for cloud platform \"%s\": expected=\"%s\", actual=\"%s\"",
                        CloudPlatform.AWS.name(), CERT_ID_2, CERT_ID_3));

        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @Test
    void syncSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateInvalidCertificateEntryBlankPem()
            throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_ID_2, AWS, "", x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        IllegalStateException illegalStateException =
                assertThrows(IllegalStateException.class, () -> underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(illegalStateException).hasMessage(
                String.format("Blank PEM in SSL certificate with CloudProviderIdentifier \"%s\" for cloud platform \"%s\"", CERT_ID_2,
                        CloudPlatform.AWS.name()));

        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateFound(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_ID_2, AWS, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(CERT_VERSION, Set.of(CERT_PEM), cert.expirationDate());
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateFoundNullDesiredCerts(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier, null));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_ID_2, AWS, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(CERT_VERSION, Set.of(CERT_PEM), cert.expirationDate());
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateFoundDesiredCertsWithActivePemOnly(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier, Set.of(CERT_PEM)));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_ID_2, AWS, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(CERT_VERSION, Set.of(CERT_PEM), null);
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateFoundDesiredCertsWithActiveAndOtherPems(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier,
                        Set.of(CERT_PEM, CERT_PEM_OTHER)));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_ID_2, AWS, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(CERT_VERSION, Set.of(CERT_PEM, CERT_PEM_OTHER), null);
    }

    @ParameterizedTest(name = "sslCertificateActiveCloudProviderIdentifier={0}")
    @NullSource
    @ValueSource(strings = CERT_ID_1)
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateFoundDesiredCertsWithOtherPemOnly(
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(SSL_CONF_ID);
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        when(sslConfigService.fetchById(SSL_CONF_ID))
                .thenReturn(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier, Set.of(CERT_PEM_OTHER)));

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_ID_2, AWS, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(CloudPlatform.AWS.name(), REGION, CERT_ID_2))
                .thenReturn(cert);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verifySslConfigCaptured(CERT_VERSION, Set.of(CERT_PEM, CERT_PEM_OTHER), cert.expirationDate());
    }

    @Test
    public void testGetCertificateFromProviderWithValidConfigAndPlatform() throws Exception {
        DBStack dbStack = mock(DBStack.class);
        SslConfig sslConfig = mock(SslConfig.class);
        CloudDatabaseServerSslCertificate cloudDatabaseServerSslCertificate =
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, "213");
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(sslConfig.getSslCertificateType()).thenReturn(SslCertificateType.CLOUD_PROVIDER_OWNED);
        when(dbStack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, "213"));

        // Act
        Optional<CloudDatabaseServerSslCertificate> result =
                underTest.getCertificateFromProvider(cloudContext, cloudCredential, dbStack, databaseStack);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(cloudDatabaseServerSslCertificate, result.get());
    }

    @Test
    public void testGetCertificateFromProviderWithInvalidConfig() throws Exception {
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<CloudDatabaseServerSslCertificate> result =
                underTest.getCertificateFromProvider(cloudContext, cloudCredential, dbStack, databaseStack);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetCertificateFromProviderWithUnsupportedPlatform() throws Exception {
        DBStack dbStack = mock(DBStack.class);
        SslConfig sslConfig = mock(SslConfig.class);
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(sslConfig.getSslCertificateType()).thenReturn(SslCertificateType.CLOUD_PROVIDER_OWNED);
        when(dbStack.getCloudPlatform()).thenReturn("UnsupportedPlatform");

        // Act
        Optional<CloudDatabaseServerSslCertificate> result =
                underTest.getCertificateFromProvider(cloudContext, cloudCredential, dbStack, databaseStack);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetCertificateFromProviderWithNonProviderOwnedCertType() throws Exception {
        DBStack dbStack = mock(DBStack.class);
        SslConfig sslConfig = mock(SslConfig.class);
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(sslConfig.getSslCertificateType()).thenReturn(SslCertificateType.BRING_YOUR_OWN);
        when(dbStack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        // Act
        Optional<CloudDatabaseServerSslCertificate> result =
                underTest.getCertificateFromProvider(cloudContext, cloudCredential, dbStack, databaseStack);

        // Assert
        assertFalse(result.isPresent());
    }

    private DBStack getDBStack() {
        return getDBStack(null);
    }

    private DBStack getDBStack(Long sslConfig) {
        DBStack dbStack = new DBStack();
        dbStack.setRegion(REGION);
        dbStack.setSslConfig(sslConfig);
        return dbStack;
    }

    private Optional<SslConfig> createSslConfig(SslCertificateType sslCertificateType, String sslCertificateActiveCloudProviderIdentifier) {
        return createSslConfig(sslCertificateType, sslCertificateActiveCloudProviderIdentifier, new HashSet<>());
    }

    private Optional<SslConfig> createSslConfig(SslCertificateType sslCertificateType, String sslCertificateActiveCloudProviderIdentifier,
            Set<String> sslCertificates) {
        return createSslConfig(sslCertificateType, sslCertificateActiveCloudProviderIdentifier, sslCertificates, null);
    }

    private Optional<SslConfig> createSslConfig(SslCertificateType sslCertificateType, String sslCertificateActiveCloudProviderIdentifier,
            Set<String> sslCertificates, Integer sslCertificateActiveVersion) {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(sslCertificateType);
        sslConfig.setSslCertificateActiveCloudProviderIdentifier(sslCertificateActiveCloudProviderIdentifier);
        sslConfig.setSslCertificates(sslCertificates);
        sslConfig.setSslCertificateActiveVersion(sslCertificateActiveVersion);
        return Optional.of(sslConfig);
    }

}
