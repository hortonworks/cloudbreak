package com.sequenceiq.redbeams.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.service.EnvironmentService;

public class DBStackToDatabaseStackConverterTest {

    private static final String NETWORK_ATTRIBUTES = "{ \"foo\": \"bar\" }";

    private static final String DATABASE_SERVER_ATTRIBUTES = "{ \"this\": \"that\", \"this1\": \"that\" }";

    private static final String STACK_TAGS = "{ \"userDefinedTags\": { \"ukey1\" : \"uvalue1\", \"key1\": \"value1\" }, "
                                            + " \"defaultTags\": { \"dkey1\" : \"dvalue1\", \"key1\": \"shadowed\" } }";

    private static final String CLOUD_PLATFORM = "AZURE";

    private static final String RESOURCE_GROUP = "resource-group";

    private DBStack dbStack;

    @InjectMocks
    private DBStackToDatabaseStackConverter underTest;

    @Mock
    private EnvironmentService environmentService;

    @Before
    public void setUp() {
        // the converter will probably get mocks soon
        initMocks(this);

        dbStack = new DBStack();
        dbStack.setId(1L);
        dbStack.setName("mystack");
        dbStack.setDisplayName("My Stack");
        dbStack.setDescription("my stack");
        dbStack.setEnvironmentId("myenv");
    }

    @Test
    public void testConversionNormal() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        dbStack.setNetwork(network);

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

        assertEquals(1, convertedStack.getNetwork().getParameters().size());
        assertEquals("bar", convertedStack.getNetwork().getParameters().get("foo"));

        assertEquals("myserver", convertedStack.getDatabaseServer().getServerId());
        assertEquals("db.m3.medium", convertedStack.getDatabaseServer().getFlavor());
        assertEquals(DatabaseEngine.POSTGRESQL, convertedStack.getDatabaseServer().getEngine());
        assertEquals("org.postgresql.Driver", convertedStack.getDatabaseServer().getConnectionDriver());
        assertEquals("root", convertedStack.getDatabaseServer().getRootUserName());
        assertEquals("cloudera", convertedStack.getDatabaseServer().getRootPassword());
        assertEquals(50L, convertedStack.getDatabaseServer().getStorageSize().longValue());
        assertEquals(List.of("sg-1234"), convertedStack.getDatabaseServer().getSecurity().getCloudSecurityIds());
        // FIXME test instanceStatus
        assertEquals(2, convertedStack.getDatabaseServer().getParameters().size());
        assertEquals("that", convertedStack.getDatabaseServer().getParameters().get("this"));
        assertEquals("template", convertedStack.getTemplate());

        Tags tags = convertedStack.getTags();
        assertEquals(3, tags.size());
        assertEquals("uvalue1", tags.getTagValue("ukey1"));
        assertEquals("dvalue1", tags.getTagValue("dkey1"));
        assertEquals("value1", tags.getTagValue("key1"));
    }

    @Test
    public void testConversionEmpty() {
        dbStack.setNetwork(null);
        dbStack.setDatabaseServer(null);
        dbStack.setTags(null);
        dbStack.setParameters(null);
        dbStack.setTemplate(null);

        DatabaseStack convertedStack = underTest.convert(dbStack);

        assertNull(convertedStack.getNetwork());
        assertNull(convertedStack.getDatabaseServer());
        assertNull(convertedStack.getTemplate());
        assertEquals(0, convertedStack.getTags().size());
    }

    @Test
    public void testConversionAzureWithMultipleResourceGroups() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        dbStack.setNetwork(network);
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
        assertFalse(parameters.containsKey(RESOURCE_GROUP_NAME_PARAMETER));
        assertFalse(parameters.containsKey(RESOURCE_GROUP_USAGE_PARAMETER));
        assertEquals(2, parameters.size());
    }

    @Test
    public void testConversionAzureWithSingleResourceGroups() {
        Network network = new Network();
        network.setAttributes(new Json(NETWORK_ATTRIBUTES));
        dbStack.setNetwork(network);
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
        assertEquals(RESOURCE_GROUP, parameters.get(RESOURCE_GROUP_NAME_PARAMETER).toString());
        assertEquals(ResourceGroupUsage.SINGLE.name(), parameters.get(RESOURCE_GROUP_USAGE_PARAMETER).toString());
        assertEquals(4, parameters.size());
    }
}
