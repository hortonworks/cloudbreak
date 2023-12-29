package com.sequenceiq.redbeams.service.sslcertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificates;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@ExtendWith(MockitoExtension.class)
class DatabaseServerSslCertificatePrescriptionServiceTest {

    private static final String CERT_ID_1 = "certID-1";

    private static final String CERT_ID_2 = "certID-2";

    private static final String CERT_ID_3 = "certID-3";

    private static final Long SSL_CONF_ID = 16L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private SslConfigService sslConfigService;

    @InjectMocks
    private DatabaseServerSslCertificatePrescriptionService underTest;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private PlatformResources platformResources;

    private DBStack dbStack;

    private DatabaseServer databaseServer;

    private DatabaseStack databaseStack;

    private Location location;

    private Region region;

    @BeforeEach
    void setUp() {
        dbStack = new DBStack();
        dbStack.setName("db_stack");
        databaseServer = DatabaseServer.builder().build();
        databaseStack = new DatabaseStack(null, databaseServer, Map.of(), "");
        region = Region.region("myregion");
        location = Location.location(region);
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenNoSsl() throws Exception {
        initDBStack();

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(cloudConnector, never()).platformResources();
    }

    @ParameterizedTest(name = "sslCertificateType={0}")
    @NullSource
    @EnumSource(value = SslCertificateType.class, names = {"NONE", "BRING_YOUR_OWN"})
    void prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateType(SslCertificateType sslCertificateType) throws Exception {
        prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeOrBadCloudPlatformInternal(sslCertificateType, CloudPlatform.AWS.name());
    }

    private void prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType sslCertificateType,
            String cloudPlatform) throws Exception {
        initDBStack(cloudPlatform, createSslConfig(sslCertificateType, CERT_ID_1));

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(cloudConnector, never()).platformResources();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = "AWS", mode = EnumSource.Mode.EXCLUDE)
    void prescribeSslCertificateIfNeededTestWhenSslBadCloudPlatform(CloudPlatform cloudPlatform) throws Exception {
        prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType.CLOUD_PROVIDER_OWNED,
                cloudPlatform.name());
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedNullCertId() throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, null));

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(cloudConnector, never()).platformResources();
    }

    static Object[][] prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedUnsupportedCertIdDataProvider() {
        return new Object[][]{
                // testCaseName sslCertificates
                {"{}", Set.of()},
                {"{CERT_ID_2}", Set.of(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2))},
                {"{CERT_ID_2, CERT_ID_3}", Set.of(new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2),
                        new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_3))},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedUnsupportedCertIdDataProvider")
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedUnsupportedCertId(String testCaseName,
            Set<CloudDatabaseServerSslCertificate> sslCertificates) throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.databaseServerGeneralSslRootCertificates(cloudCredential, region))
                .thenReturn(new CloudDatabaseServerSslCertificates(sslCertificates));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudContext.getLocation()).thenReturn(location);

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudConnector).platformResources();
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenFailureSslAwsCloudProviderOwnedAvailableSslCertificatesQueryError() throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        Exception e = new Exception("Foo");
        when(platformResources.databaseServerGeneralSslRootCertificates(cloudCredential, region)).thenThrow(e);
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudContext.getLocation()).thenReturn(location);

        Exception exceptionResult =
                assertThrows(Exception.class, () -> underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack));

        assertThat(exceptionResult).isSameAs(e);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudConnector).platformResources();
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedDefaultCertId() throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        Set<CloudDatabaseServerSslCertificate> sslCertificates = Set.of(
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_1));
        when(platformResources.databaseServerGeneralSslRootCertificates(cloudCredential, region))
                .thenReturn(new CloudDatabaseServerSslCertificates(sslCertificates));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudContext.getLocation()).thenReturn(location);

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudConnector).platformResources();
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedCertIdPrescribed() throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        Set<CloudDatabaseServerSslCertificate> sslCertificates = Set.of(
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_1),
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));
        when(platformResources.databaseServerGeneralSslRootCertificates(cloudCredential, region))
                .thenReturn(new CloudDatabaseServerSslCertificates(sslCertificates));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudContext.getLocation()).thenReturn(location);

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isEqualTo(CERT_ID_1);
        verify(cloudConnector).platformResources();
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedOverriddenMatchingCertId() throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_1));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        Set<CloudDatabaseServerSslCertificate> sslCertificates = Set.of(
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_1, true),
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));
        when(platformResources.databaseServerGeneralSslRootCertificates(cloudCredential, region))
                .thenReturn(new CloudDatabaseServerSslCertificates(sslCertificates));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudContext.getLocation()).thenReturn(location);

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudConnector).platformResources();
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedOverriddenDifferentCertId() throws Exception {
        initDBStack(CloudPlatform.AWS.name(), createSslConfig(SslCertificateType.CLOUD_PROVIDER_OWNED, CERT_ID_3));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        Set<CloudDatabaseServerSslCertificate> sslCertificates = Set.of(
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_1, true),
                new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, CERT_ID_2));
        when(platformResources.databaseServerGeneralSslRootCertificates(cloudCredential, region))
                .thenReturn(new CloudDatabaseServerSslCertificates(sslCertificates));
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudContext.getLocation()).thenReturn(location);

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudConnector).platformResources();
    }

    private void initDBStack() {
        initDBStack(CloudPlatform.AWS.name(), null);
    }

    private void initDBStack(String cloudPlatform, SslConfig sslConfig) {
        dbStack.setCloudPlatform(cloudPlatform);
        dbStack.setSslConfig(Optional.ofNullable(sslConfig).map(SslConfig::getId).orElse(null));
    }

    private SslConfig createSslConfig(SslCertificateType sslCertificateType, String sslCertIdentifier) {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setId(SSL_CONF_ID);
        sslConfig.setSslCertificateType(sslCertificateType);
        sslConfig.setSslCertificateActiveCloudProviderIdentifier(sslCertIdentifier);
        when(sslConfigService.fetchById(SSL_CONF_ID)).thenReturn(Optional.of(sslConfig));
        return sslConfig;
    }

}
