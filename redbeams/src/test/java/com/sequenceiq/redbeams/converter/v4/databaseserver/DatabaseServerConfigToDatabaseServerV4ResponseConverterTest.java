package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.model.common.Status;
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

    @Mock
    private ConversionService conversionService;

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
        server.setEnvironmentId("myenvironment");
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        DBStack dbStack = new DBStack();
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(Status.CREATE_IN_PROGRESS);
        dbStackStatus.setStatusReason("Lorem ipsum");
        dbStack.setDBStackStatus(dbStackStatus);
        server.setDbStack(dbStack);
        when(conversionService.convert(anyString(), eq(SecretResponse.class))).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(conversionService, times(2)).convert(anyString(), eq(SecretResponse.class));
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
        assertThat(response.getResourceStatus()).isEqualTo(server.getResourceStatus());
        assertThat(response.getStatus()).isEqualTo(dbStack.getStatus());
        assertThat(response.getStatusReason()).isEqualTo(dbStack.getStatusReason());
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
        server.setEnvironmentId("myenvironment");
        server.setResourceStatus(ResourceStatus.USER_MANAGED);
        when(conversionService.convert(anyString(), eq(SecretResponse.class))).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(conversionService, times(2)).convert(anyString(), eq(SecretResponse.class));
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
    }

    @Test
    void testConversionOfSslConfigWhenDbStackPresentButSslConfigAbsent() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setDbStack(new DBStack());

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.DISABLED);
    }

    @Test
    void testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeNone() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DBStack dbStack = new DBStack();
        dbStack.setSslConfig(new SslConfig());
        server.setDbStack(dbStack);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.DISABLED);
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
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.ENABLED);
    }

    @Test
    void testConversionOfSslConfigWhenDbStackPresentAndCertificateTypeCloudProviderOwned() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setResourceCrn(TestData.getTestCrn(RESOURCE_TYPE_DATABASE_SERVER, RESOURCE_ID));
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);

        DBStack dbStack = new DBStack();
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        dbStack.setSslConfig(sslConfig);
        server.setDbStack(dbStack);

        DatabaseServerV4Response response = converter.convert(server);

        assertThat(response).isNotNull();
        assertThat(response.getSslConfig()).isNotNull();
        assertThat(response.getSslConfig().getSslMode()).isEqualTo(SslMode.ENABLED);
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
