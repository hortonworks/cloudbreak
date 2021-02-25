package com.sequenceiq.redbeams.service.sslcertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private DatabaseServerSslCertificatePrescriptionService underTest;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector<Object> cloudConnector;

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
        databaseServer = DatabaseServer.builder().build();
        databaseStack = new DatabaseStack(null, databaseServer, Map.of(), "");
        region = Region.region("myregion");
        location = Location.location(region);
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenNoSsl() {
        initDBStack();

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(cloudConnector, never()).platformResources();
    }

    static Object[][] prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeDataProvider() {
        return new Object[][]{
                // testCaseName sslCertificateType
                {"sslCertificateType=null", null},
                {"SslCertificateType.NONE", SslCertificateType.NONE},
                {"SslCertificateType.BRING_YOUR_OWN", SslCertificateType.BRING_YOUR_OWN},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeDataProvider")
    void prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateType(String testCaseName, SslCertificateType sslCertificateType) {
        prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeOrBadCloudPlatformInternal(sslCertificateType, CloudPlatform.AWS.name());
    }

    private void prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType sslCertificateType,
            String cloudPlatform) {
        initDBStack(cloudPlatform, createSslConfig(sslCertificateType, CERT_ID_1));

        underTest.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);

        assertThat(databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).isNull();
        verify(cloudPlatformConnectors, never()).get(any(CloudPlatformVariant.class));
        verify(cloudConnector, never()).platformResources();
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslBadCloudPlatform() {
        prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeOrBadCloudPlatformInternal(SslCertificateType.CLOUD_PROVIDER_OWNED,
                CloudPlatform.AZURE.name());
    }

    @Test
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedNullCertId() {
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
            Set<CloudDatabaseServerSslCertificate> sslCertificates) {
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
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedDefaultCertId() {
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
    void prescribeSslCertificateIfNeededTestWhenSslAwsCloudProviderOwnedCertIdPrescribed() {
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

    private void initDBStack() {
        initDBStack(CloudPlatform.AWS.name(), null);
    }

    private void initDBStack(String cloudPlatform, SslConfig sslConfig) {
        dbStack.setCloudPlatform(cloudPlatform);
        dbStack.setSslConfig(sslConfig);
    }

    private SslConfig createSslConfig(SslCertificateType sslCertificateType, String sslCertificateActiveCloudProviderIdentifier) {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(sslCertificateType);
        sslConfig.setSslCertificateActiveCloudProviderIdentifier(sslCertificateActiveCloudProviderIdentifier);
        return sslConfig;
    }

}