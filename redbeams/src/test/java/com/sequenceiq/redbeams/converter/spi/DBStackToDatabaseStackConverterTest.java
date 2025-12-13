package com.sequenceiq.redbeams.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENCRYPTION_KEY_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENCRYPTION_KEY_URL;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENCRYPTION_USER_MANAGED_IDENTITY;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.network.NetworkService;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@ExtendWith(MockitoExtension.class)
public class DBStackToDatabaseStackConverterTest {

    private static final String NETWORK_ATTRIBUTES = "{ \"foo\": \"bar\" }";

    private static final String DATABASE_SERVER_ATTRIBUTES = "{ \"this\": \"that\", \"this1\": \"that\" }";

    private static final String STACK_TAGS = "{ \"userDefinedTags\": { \"ukey1\" : \"uvalue1\", \"key1\": \"value1\" }, "
                                            + " \"defaultTags\": { \"dkey1\" : \"dvalue1\", \"key1\": \"shadowed\" } }";

    private static final Long NETWORK_ID = 12L;

    private static final String CLOUD_PLATFORM = "AZURE";

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String KEY_URL = "resource-group";

    private DBStack dbStack;

    @InjectMocks
    private DBStackToDatabaseStackConverter underTest;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private NetworkService networkService;

    @Mock
    private SslConfigService sslConfigService;

    @BeforeEach
    public void setUp() {
        dbStack = new DBStack();
        dbStack.setId(1L);
        dbStack.setName("mystack");
        dbStack.setDisplayName("My Stack");
        dbStack.setDescription("my stack");
        dbStack.setEnvironmentId("myenv");
        dbStack.setNetwork(NETWORK_ID);
        lenient().when(sslConfigService.fetchById(isNull())).thenReturn(Optional.empty());
    }

    @Test
    public void testConversionNormal() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));

        DatabaseServer server = new DatabaseServer();
        server.setName("myserver");
        server.setInstanceType("db.m3.medium");
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setConnectionDriver("org.postgresql.Driver");
        server.setRootUserName("root");
        server.setRootPassword("cloudera");
        server.setStorageSize(50L);
        SecurityGroup securityGroup = new SecurityGroup();
        Set<String> securityGroupIds = new HashSet<>();
        securityGroupIds.add("sg-1234");
        securityGroup.setSecurityGroupIds(securityGroupIds);
        server.setSecurityGroup(securityGroup);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);

        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");

        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertThat(convertedStack.getNetwork().getParameters().size()).isEqualTo(1);
        assertThat(convertedStack.getNetwork().getParameters().get("foo")).isEqualTo("bar");

        assertThat(convertedStack.getDatabaseServer().getServerId()).isEqualTo("myserver");
        assertThat(convertedStack.getDatabaseServer().getFlavor()).isEqualTo("db.m3.medium");
        assertThat(convertedStack.getDatabaseServer().getEngine()).isEqualTo(DatabaseEngine.POSTGRESQL);
        assertThat(convertedStack.getDatabaseServer().getConnectionDriver()).isEqualTo("org.postgresql.Driver");
        assertThat(convertedStack.getDatabaseServer().getRootUserName()).isEqualTo("root");
        assertThat(convertedStack.getDatabaseServer().getRootPassword()).isEqualTo("cloudera");
        assertThat(convertedStack.getDatabaseServer().getStorageSize()).isEqualTo(50L);
        assertThat(convertedStack.getDatabaseServer().getSecurity().getCloudSecurityIds()).isEqualTo(List.of("sg-1234"));
        assertThat(convertedStack.getDatabaseServer().getStatus()).isEqualTo(CREATE_REQUESTED);
        assertThat(convertedStack.getDatabaseServer().getParameters().size()).isEqualTo(2);
        assertThat(convertedStack.getDatabaseServer().getParameters().get("this")).isEqualTo("that");
        assertThat(convertedStack.getTemplate()).isEqualTo("template");

        Map<String, String> tags = convertedStack.getTags();
        assertThat(tags.size()).isEqualTo(3);
        assertThat(tags.get("ukey1")).isEqualTo("uvalue1");
        assertThat(tags.get("dkey1")).isEqualTo("dvalue1");
        assertThat(tags.get("key1")).isEqualTo("value1");
    }

    @Test
    public void testConversionEmpty() {
        dbStack.setNetwork(null);
        dbStack.setDatabaseServer(null);
        dbStack.setTags(null);
        dbStack.setParameters(null);
        dbStack.setTemplate(null);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertThat(convertedStack.getNetwork()).isNull();
        assertThat(convertedStack.getDatabaseServer()).isNull();
        assertThat(convertedStack.getTemplate()).isNull();
        assertThat(convertedStack.getTags().size()).isEqualTo(0);
        verifyNoInteractions(networkService);
    }

    @Test
    public void testConversionAzureWithMultipleResourceGroups() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.containsKey(RESOURCE_GROUP_NAME_PARAMETER)).isFalse();
        assertThat(parameters.containsKey(RESOURCE_GROUP_USAGE_PARAMETER)).isFalse();
        assertThat(parameters.size()).isEqualTo(2);
    }

    @Test
    public void testConversionAzureWithSingleResourceGroups() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                        .withName(RESOURCE_GROUP)
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.get(RESOURCE_GROUP_NAME_PARAMETER).toString()).isEqualTo(RESOURCE_GROUP);
        assertThat(parameters.get(RESOURCE_GROUP_USAGE_PARAMETER).toString()).isEqualTo(ResourceGroupUsage.SINGLE.name());
        assertThat(parameters.size()).isEqualTo(4);
    }

    @Test
    public void testConversionAzureWithAzureEncryptionResourcesPresentAndNoEncryptionKeyRG() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withEncryptionKeyUrl(KEY_URL)
                        .withUserManagedIdentity("identity")
                        .build())
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withName(RESOURCE_GROUP)
                        .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.get(ENCRYPTION_KEY_URL).toString()).isEqualTo(KEY_URL);
        assertThat(parameters.get(ENCRYPTION_USER_MANAGED_IDENTITY).toString()).isEqualTo("identity");
        assertThat(parameters.get(ENCRYPTION_KEY_RESOURCE_GROUP_NAME).toString()).isEqualTo(RESOURCE_GROUP);
        assertThat(parameters.size()).isEqualTo(7);
    }

    @Test
    public void testConversionAzureWithAzureEncryptionResourcesPresentAndEncryptionKeyRG() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withEncryptionKeyUrl(KEY_URL)
                        .withEncryptionKeyResourceGroupName(RESOURCE_GROUP)
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.get(ENCRYPTION_KEY_URL).toString()).isEqualTo(KEY_URL);
        assertThat(parameters.get(ENCRYPTION_KEY_RESOURCE_GROUP_NAME).toString()).isEqualTo(RESOURCE_GROUP);
        assertThat(parameters.size()).isEqualTo(4);
    }

    @Test
    public void testConversionGcpWithGcpEncryptionResourcesPresent() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform("GCP");
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("GCP");
        environmentResponse.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey("value")
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.get("key").toString()).isEqualTo("value");
        assertThat(parameters.size()).isEqualTo(3);
    }

    @Test
    public void testConversionAzureWithAzureEncryptionResourcesPresentAndSingleResourceGroup() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform(CLOUD_PLATFORM);
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                        .withName(RESOURCE_GROUP)
                        .build())
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withEncryptionKeyUrl(KEY_URL)
                        .withEncryptionKeyResourceGroupName(RESOURCE_GROUP)
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.get(RESOURCE_GROUP_NAME_PARAMETER).toString()).isEqualTo(RESOURCE_GROUP);
        assertThat(parameters.get(RESOURCE_GROUP_USAGE_PARAMETER).toString()).isEqualTo(ResourceGroupUsage.SINGLE.name());
        assertThat(parameters.get(ENCRYPTION_KEY_URL).toString()).isEqualTo(KEY_URL);
        assertThat(parameters.get(ENCRYPTION_KEY_RESOURCE_GROUP_NAME).toString()).isEqualTo(RESOURCE_GROUP);
        assertThat(parameters.size()).isEqualTo(6);
    }

    @Test
    void testConversionWithNullSslConfig() {
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        dbStack.setDatabaseServer(server);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertThat(convertedStack.getDatabaseServer().isUseSslEnforcement()).isFalse();
    }

    @Test
    void testConversionWithSslCertificateNone() {
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        dbStack.setDatabaseServer(server);
        dbStack.setSslConfig(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(new SslConfig()));

        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertThat(convertedStack.getDatabaseServer().isUseSslEnforcement()).isFalse();
    }

    @Test
    void testConversionWithSslCertificateBringYourOwn() {
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        dbStack.setDatabaseServer(server);
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.BRING_YOUR_OWN);
        dbStack.setSslConfig(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));

        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertThat(convertedStack.getDatabaseServer().isUseSslEnforcement()).isTrue();
    }

    @Test
    void testConversionWithSslCertificateCloudProviderOwned() {
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        dbStack.setDatabaseServer(server);
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        dbStack.setSslConfig(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertThat(convertedStack.getDatabaseServer().isUseSslEnforcement()).isTrue();
    }

    @Test
    public void testConversionAwsWithAwsEncryptionResourcesPresent() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        when(networkService.findById(NETWORK_ID)).thenReturn(Optional.of(network));
        dbStack.setNetwork(NETWORK_ID);
        dbStack.setCloudPlatform("AWS");
        dbStack.setParameters(new HashMap<>());
        DatabaseServer server = new DatabaseServer();
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(server);
        dbStack.setTags(new Json(STACK_TAGS));
        dbStack.setTemplate("template");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setAws(AwsEnvironmentParameters.builder()
                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParameters.builder()
                        .withEncryptionKeyArn("value")
                        .build())
                .build());
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        Map<String, Object> parameters = convertedStack.getDatabaseServer().getParameters();
        assertThat(parameters.get("key").toString()).isEqualTo("value");
        assertThat(parameters.size()).isEqualTo(3);
    }

}
