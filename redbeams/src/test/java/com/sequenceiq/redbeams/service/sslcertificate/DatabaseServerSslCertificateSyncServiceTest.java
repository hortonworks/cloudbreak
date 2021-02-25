package com.sequenceiq.redbeams.service.sslcertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class DatabaseServerSslCertificateSyncServiceTest {

    private static final String CERT_ID_1 = "certID-1";

    private static final String CERT_ID_2 = "certID-2";

    private static final String CERT_ID_3 = "certID-3";

    private static final int CERT_VERSION = 789;

    private static final String CERT_PEM = "cert-PEM";

    @Mock
    private DBStackService dbStackService;

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
    private CloudConnector<Object> cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector<Object> resourceConnector;

    @Captor
    private ArgumentCaptor<DBStack> dbStackArgumentCaptor;

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

    static Object[][] syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeDataProvider() {
        return new Object[][]{
                // testCaseName sslCertificateType
                {"sslCertificateType=null", null},
                {"SslCertificateType.NONE", SslCertificateType.NONE},
                {"SslCertificateType.BRING_YOUR_OWN", SslCertificateType.BRING_YOUR_OWN},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeDataProvider")
    void syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateType(String testCaseName, SslCertificateType sslCertificateType) throws Exception {
        syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeOrBadCloudPlatformInternal(sslCertificateType, CloudPlatform.AWS.name());
    }

    void syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType sslCertificateType, String cloudPlatform)
            throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(sslCertificateType, CERT_ID_1));
        dbStack.setCloudPlatform(cloudPlatform);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(databaseServerSslCertificateConfig, never()).getCertByPlatformAndCloudProviderIdentifier(anyString(), anyString());
        verify(dbStackService, never()).save(any(DBStack.class));
    }

    @Test
    void syncSslCertificateIfNeededTestWhenSuccessSslBadCloudPlatform() throws Exception {
        syncSslCertificateIfNeededTestWhenSuccessSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType.CLOUD_PROVIDER_OWNED,
                CloudPlatform.AZURE.name());
    }

    @Test
    void syncSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedActiveSslRootCertificateQueryError() throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        Exception e = new Exception();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack)).thenThrow(e);

        Exception exceptionResult =
                assertThrows(Exception.class, () -> underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(exceptionResult).isSameAs(e);

        verify(databaseServerSslCertificateConfig, never()).getCertByPlatformAndCloudProviderIdentifier(anyString(), anyString());
        verify(dbStackService, never()).save(any(DBStack.class));
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
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack)).thenReturn(null);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(databaseServerSslCertificateConfig, never()).getCertByPlatformAndCloudProviderIdentifier(anyString(), anyString());
        verify(dbStackService, never()).save(any(DBStack.class));
    }

    @Test
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMatchingActiveSslRootCertificate() throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_1));

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(databaseServerSslCertificateConfig, never()).getCertByPlatformAndCloudProviderIdentifier(anyString(), anyString());
        verify(dbStackService, never()).save(any(DBStack.class));
    }

    static Object[][] syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateDataProvider() {
        return new Object[][]{
                // testCaseName sslCertificateActiveCloudProviderIdentifier
                {"null", null},
                {"CERT_ID_1", CERT_ID_1},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateDataProvider")
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateUnknown(String testCaseName,
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        when(databaseServerSslCertificateConfig.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AWS.name(), CERT_ID_2)).thenReturn(null);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(dbStackService).save(dbStackArgumentCaptor.capture());
        verifyDbStackCaptured(null, Set.of());
    }

    private void verifyDbStackCaptured(Integer sslCertificateVersionExpected, Set<Object> sslCertificatesExpected) {
        DBStack dbStackCaptured = dbStackArgumentCaptor.getValue();
        assertThat(dbStackCaptured).isNotNull();
        SslConfig sslConfigCaptured = dbStackCaptured.getSslConfig();
        assertThat(sslConfigCaptured).isNotNull();
        assertThat(sslConfigCaptured.getSslCertificateType()).isEqualTo(SslCertificateType.CLOUD_PROVIDER_OWNED);
        assertThat(sslConfigCaptured.getSslCertificateActiveCloudProviderIdentifier()).isEqualTo(CERT_ID_2);
        assertThat(sslConfigCaptured.getSslCertificateActiveVersion()).isEqualTo(sslCertificateVersionExpected);
        assertThat(sslConfigCaptured.getSslCertificates()).isEqualTo(sslCertificatesExpected);
    }

    @Test
    void syncSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateInvalidCertificateEntryCloudProviderIdMismatch()
            throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_3, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AWS.name(), CERT_ID_2)).thenReturn(cert);

        IllegalStateException illegalStateException =
                assertThrows(IllegalStateException.class, () -> underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(illegalStateException).hasMessage(
                String.format("SSL certificate CloudProviderIdentifier mismatch for cloud platform \"%s\": expected=\"%s\", actual=\"%s\"",
                        CloudPlatform.AWS.name(), CERT_ID_2, CERT_ID_3));

        verify(dbStackService, never()).save(any(DBStack.class));
    }

    @Test
    void syncSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateInvalidCertificateEntryBlankPem()
            throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, "", x509Cert);
        when(databaseServerSslCertificateConfig.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AWS.name(), CERT_ID_2)).thenReturn(cert);

        IllegalStateException illegalStateException =
                assertThrows(IllegalStateException.class, () -> underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(illegalStateException).hasMessage(
                String.format("Blank PEM in SSL certificate with CloudProviderIdentifier \"%s\" for cloud platform \"%s\"", CERT_ID_2,
                        CloudPlatform.AWS.name()));

        verify(dbStackService, never()).save(any(DBStack.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateDataProvider")
    void syncSslCertificateIfNeededTestWhenSuccessSslAwsCloudProviderOwnedMismatchingActiveSslRootCertificateFound(String testCaseName,
            String sslCertificateActiveCloudProviderIdentifier) throws Exception {
        DBStack dbStack = getDBStack(createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, sslCertificateActiveCloudProviderIdentifier));
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        setupCloudConnectorMock();
        when(resourceConnector.getDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack))
                .thenReturn(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));

        SslCertificateEntry cert = new SslCertificateEntry(CERT_VERSION, CERT_ID_2, CERT_PEM, x509Cert);
        when(databaseServerSslCertificateConfig.getCertByPlatformAndCloudProviderIdentifier(CloudPlatform.AWS.name(), CERT_ID_2)).thenReturn(cert);

        underTest.syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        verify(dbStackService).save(dbStackArgumentCaptor.capture());
        verifyDbStackCaptured(CERT_VERSION, Set.of(CERT_PEM));
    }

    private DBStack getDBStack() {
        return getDBStack(null);
    }

    private DBStack getDBStack(SslConfig sslConfig) {
        DBStack dbStack = new DBStack();
        dbStack.setSslConfig(sslConfig);
        return dbStack;
    }

    private SslConfig createSslConfig(SslCertificateType sslCertificateType, String sslCertificateActiveCloudProviderIdentifier) {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(sslCertificateType);
        sslConfig.setSslCertificateActiveCloudProviderIdentifier(sslCertificateActiveCloudProviderIdentifier);
        return sslConfig;
    }

}