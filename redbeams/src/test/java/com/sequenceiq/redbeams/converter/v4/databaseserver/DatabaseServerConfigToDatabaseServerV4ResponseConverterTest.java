package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@ExtendWith(MockitoExtension.class)
public class DatabaseServerConfigToDatabaseServerV4ResponseConverterTest {

    private static final String RESOURCE_TYPE_DATABASE_SERVER = "databaseServer";

    private static final String RESOURCE_ID = "myserver";

    private static final String HOST = "myserver.db.example.com";

    private static final int PORT = 5432;

    private static final String STATUS_REASON = "Lorem ipsum";

    private static final String ENVIRONMENT_ID = "myenvironment";

    private static final Set<String> CERTS = Set.of("my-cert");

    private static final int CERT_MAX_VERSION = 3;

    private static final int CERT_ACTIVE_VERSION = 2;

    private static final String CERT_ACTIVE_CLOUD_PROVIDER_IDENTIFIER = "cert-id-2";

    private static final int CERT_LEGACY_MAX_VERSION = 1;

    private static final String CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIER = "cert-id-1";

    private static final String CLOUD_PLATFORM = CloudPlatform.AWS.name();

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @InjectMocks
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter converter;

    @Test
    public void testConversion() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setName(RESOURCE_ID);
        server.setDescription("mine not yours");
        server.setHost(HOST);
        server.setPort(PORT);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        initializeSecrets(server);
        server.setCreationDate(System.currentTimeMillis());
        server.setEnvironmentId(ENVIRONMENT_ID);
        server.setClusterCrn("myclustercrn");
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        DBStack dbStack = new DBStack();
        initDBStackStatus(dbStack);
        server.setDbStack(dbStack);
        when(stringToSecretResponseConverter.convert(anyString())).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(stringToSecretResponseConverter, times(2)).convert(anyString());
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(server.getId());
        assertThat(response.getCrn()).isEqualTo(server.getResourceCrn().toString());
        assertThat(response.getName()).isEqualTo(server.getName());
        assertThat(response.getDescription()).isEqualTo(server.getDescription());
        assertThat(response.getHost()).isEqualTo(server.getHost());
        assertThat(response.getPort()).isEqualTo(server.getPort());
        assertThat(response.getDatabaseVendor()).isEqualTo(server.getDatabaseVendor().databaseType());
        assertThat(response.getDatabaseVendorDisplayName()).isEqualTo(server.getDatabaseVendor().displayName());
        assertThat(response.getConnectionUserName()).isNotNull();
        assertThat(response.getConnectionPassword()).isNotNull();
        assertThat(response.getCreationDate()).isEqualTo(server.getCreationDate());
        assertThat(response.getEnvironmentCrn()).isEqualTo(server.getEnvironmentId());
        assertThat(response.getClusterCrn()).isEqualTo(server.getClusterCrn());
        assertThat(response.getResourceStatus()).isEqualTo(server.getResourceStatus());
        assertThat(response.getStatus()).isEqualTo(dbStack.getStatus());
        assertThat(response.getStatusReason()).isEqualTo(dbStack.getStatusReason());
    }

    private void initDBStackStatus(DBStack dbStack) {
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(Status.CREATE_IN_PROGRESS);
        dbStackStatus.setStatusReason(STATUS_REASON);
        dbStack.setDBStackStatus(dbStackStatus);
    }

    @Test
    public void testConversionWhenUserManaged() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setName(RESOURCE_ID);
        server.setDescription("mine not yours");
        server.setHost(HOST);
        server.setPort(PORT);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        initializeSecrets(server);
        server.setCreationDate(System.currentTimeMillis());
        server.setEnvironmentId(ENVIRONMENT_ID);
        server.setResourceStatus(ResourceStatus.USER_MANAGED);
        when(stringToSecretResponseConverter.convert(anyString())).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(stringToSecretResponseConverter, times(2)).convert(anyString());
        assertThat(response).isNotNull();
        assertThat(response.getResourceStatus()).isEqualTo(server.getResourceStatus());
        assertThat(response.getStatus()).isEqualTo(Status.AVAILABLE);
        assertThat(response.getStatusReason()).isNull();
    }

    @Test
    void testConversionOfSslConfigWhenDbStackAbsentAndNoHostOrPort() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.DISABLED);
        assertThat(response.getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void testConversionOfSslConfigWhenDbStackAbsentButHostAndPortPresent() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setHost(HOST);
        server.setPort(PORT);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.DISABLED);
        assertThat(response.getStatus()).isEqualTo(Status.AVAILABLE);
    }

    @Test
    void testConversionOfSslConfigWhenDbStackPresentButSslConfigAbsent() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DBStack dbStack = new DBStack();
        initDBStackStatus(dbStack);
        server.setDbStack(dbStack);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.DISABLED);
        assertThat(response.getStatus()).isEqualTo(dbStack.getStatus());
    }

    @Test
    void testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeNone() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DBStack dbStack = new DBStack();
        dbStack.setSslConfig(new SslConfig());
        initDBStackStatus(dbStack);
        server.setDbStack(dbStack);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        SslConfigV4Response sslConfigV4Response = response.getSslConfig();
        assertThat(sslConfigV4Response).isNotNull();
        assertThat(sslConfigV4Response.getSslMode()).isEqualTo(SslMode.DISABLED);
        assertThat(sslConfigV4Response.getSslCertificateType()).isEqualTo(SslCertificateType.NONE);
        assertThat(response.getStatus()).isEqualTo(dbStack.getStatus());
    }

    @Test
    void testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeBringYourOwn() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DBStack dbStack = new DBStack();
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.BRING_YOUR_OWN);
        dbStack.setSslConfig(sslConfig);
        server.setDbStack(dbStack);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        SslConfigV4Response sslConfigV4Response = response.getSslConfig();
        assertThat(sslConfigV4Response).isNotNull();
        assertThat(sslConfigV4Response.getSslMode()).isEqualTo(SslMode.ENABLED);
        assertThat(sslConfigV4Response.getSslCertificateType()).isEqualTo(SslCertificateType.BRING_YOUR_OWN);
    }

    static Object[][] testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeCloudProviderOwnedDataProvider() {
        return new Object[][]{
                // testCaseName certActiveVersionInput certActiveCloudProviderIdentifierInput certActiveVersionExpected
                //      certActiveCloudProviderIdentifierExpected
                {"null, null", null, null, CERT_LEGACY_MAX_VERSION, CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIER},
                {"certActiveVersion, null", CERT_ACTIVE_VERSION, null, CERT_ACTIVE_VERSION, CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIER},
                {"null, certActiveCloudProviderIdentifier", null, CERT_ACTIVE_CLOUD_PROVIDER_IDENTIFIER, CERT_LEGACY_MAX_VERSION,
                        CERT_ACTIVE_CLOUD_PROVIDER_IDENTIFIER},
                {"certActiveVersion, certActiveCloudProviderIdentifier", CERT_ACTIVE_VERSION, CERT_ACTIVE_CLOUD_PROVIDER_IDENTIFIER, CERT_ACTIVE_VERSION,
                        CERT_ACTIVE_CLOUD_PROVIDER_IDENTIFIER},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeCloudProviderOwnedDataProvider")
    void testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeCloudProviderOwned(String testCaseName, Integer certActiveVersionInput,
            String certActiveCloudProviderIdentifierInput, int certActiveVersionExpected, String certActiveCloudProviderIdentifierExpected) {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        sslConfig.setSslCertificates(CERTS);
        sslConfig.setSslCertificateActiveVersion(certActiveVersionInput);
        sslConfig.setSslCertificateActiveCloudProviderIdentifier(certActiveCloudProviderIdentifierInput);
        dbStack.setSslConfig(sslConfig);
        server.setDbStack(dbStack);

        when(databaseServerSslCertificateConfig.getMaxVersionByPlatform(CLOUD_PLATFORM)).thenReturn(CERT_MAX_VERSION);
        when(databaseServerSslCertificateConfig.getLegacyMaxVersionByPlatform(CLOUD_PLATFORM)).thenReturn(CERT_LEGACY_MAX_VERSION);
        when(databaseServerSslCertificateConfig.getLegacyCloudProviderIdentifierByPlatform(CLOUD_PLATFORM)).thenReturn(CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIER);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        SslConfigV4Response sslConfigV4Response = response.getSslConfig();
        assertThat(sslConfigV4Response).isNotNull();
        assertThat(sslConfigV4Response.getSslMode()).isEqualTo(SslMode.ENABLED);
        assertThat(sslConfigV4Response.getSslCertificateType()).isEqualTo(SslCertificateType.CLOUD_PROVIDER_OWNED);
        assertThat(sslConfigV4Response.getSslCertificates()).isSameAs(CERTS);
        assertThat(sslConfigV4Response.getSslCertificateHighestAvailableVersion()).isEqualTo(CERT_MAX_VERSION);
        assertThat(sslConfigV4Response.getSslCertificateActiveVersion()).isEqualTo(certActiveVersionExpected);
        assertThat(sslConfigV4Response.getSslCertificateActiveCloudProviderIdentifier()).isEqualTo(certActiveCloudProviderIdentifierExpected);
    }

    private void initializeSecrets(DatabaseServerConfig server) {
        server.setConnectionUserName("root");
        Optional.ofNullable((Secret) ReflectionTestUtils.getField(server, "connectionUserName"))
                .ifPresent(secret -> ReflectionTestUtils.setField(secret, "secret", "rootSecret"));

        server.setConnectionPassword("cloudera");
        Optional.ofNullable((Secret) ReflectionTestUtils.getField(server, "connectionPassword"))
                .ifPresent(secret -> ReflectionTestUtils.setField(secret, "secret", "clouderaSecret"));
    }

}
