package com.sequenceiq.redbeams.domain.stack;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class DBStackTest {

    private static final Network NETWORK = new Network();

    private static final DatabaseServer SERVER = new DatabaseServer();

    private static final Json TAGS = new Json("{}");

    private static final Map PARAMETERS = ImmutableMap.of("foo", "bar");

    private DBStack dbStack;

    @Before
    public void setUp() throws Exception {
        dbStack = new DBStack();
    }

    @Test
    public void testGettersAndSetters() {
        dbStack.setId(1L);
        assertEquals(1L, dbStack.getId().longValue());

        dbStack.setName("mydbstack");
        assertEquals("mydbstack", dbStack.getName());

        dbStack.setDisplayName("My DB Stack");
        assertEquals("My DB Stack", dbStack.getDisplayName());

        dbStack.setDescription("mine not yours");
        assertEquals("mine not yours", dbStack.getDescription());

        dbStack.setRegion("us-east-1");
        assertEquals("us-east-1", dbStack.getRegion());

        dbStack.setAvailabilityZone("us-east-1b");
        assertEquals("us-east-1b", dbStack.getAvailabilityZone());

        dbStack.setNetwork(NETWORK);
        assertEquals(NETWORK, dbStack.getNetwork());

        dbStack.setDatabaseServer(SERVER);
        assertEquals(SERVER, dbStack.getDatabaseServer());

        dbStack.setTags(TAGS);
        assertEquals(TAGS, dbStack.getTags());

        dbStack.setParameters(PARAMETERS);
        assertEquals(PARAMETERS, dbStack.getParameters());

        dbStack.setCloudPlatform("AWS");
        assertEquals("AWS", dbStack.getCloudPlatform());

        dbStack.setPlatformVariant("GovCloud");
        assertEquals("GovCloud", dbStack.getPlatformVariant());

        dbStack.setEnvironmentId("myenv");
        assertEquals("myenv", dbStack.getEnvironmentId());

        dbStack.setTemplate("template");
        assertEquals("template", dbStack.getTemplate());

        Crn ownerCrn = Crn.safeFromString("crn:altus:iam:us-west-1:cloudera:user:bob@cloudera.com");
        dbStack.setOwnerCrn(ownerCrn);
        assertEquals(ownerCrn, dbStack.getOwnerCrn());
    }

}
