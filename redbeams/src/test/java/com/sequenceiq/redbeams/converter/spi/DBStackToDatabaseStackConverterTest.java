package com.sequenceiq.redbeams.converter.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

public class DBStackToDatabaseStackConverterTest {

    private static final String NETWORK_ATTRIBUTES = "{ \"foo\": \"bar\" }";

    private static final String DATABASE_SERVER_ATTRIBUTES = "{ \"this\": \"that\" }";

    private static final String STACK_TAGS = "{ \"userDefinedTags\": { \"ukey1\" : \"uvalue1\", \"key1\": \"value1\" }, "
                                            + " \"defaultTags\": { \"dkey1\" : \"dvalue1\", \"key1\": \"shadowed\" } }";

    private DBStack dbStack;

    @InjectMocks
    private DBStackToDatabaseStackConverter underTest;

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
        assertEquals("root", convertedStack.getDatabaseServer().getRootUserName());
        assertEquals("cloudera", convertedStack.getDatabaseServer().getRootPassword());
        assertEquals(50L, convertedStack.getDatabaseServer().getStorageSize());
        assertEquals(List.of("sg-1234"), convertedStack.getDatabaseServer().getSecurity().getCloudSecurityIds());
        // FIXME test instanceStatus
        assertEquals(1, convertedStack.getDatabaseServer().getParameters().size());
        assertEquals("that", convertedStack.getDatabaseServer().getParameters().get("this"));
        assertEquals("template", convertedStack.getTemplate());

        Map<String, String> tags = convertedStack.getTags();
        assertEquals(3, tags.size());
        assertEquals("uvalue1", tags.get("ukey1"));
        assertEquals("dvalue1", tags.get("dkey1"));
        assertEquals("value1", tags.get("key1"));
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

}
