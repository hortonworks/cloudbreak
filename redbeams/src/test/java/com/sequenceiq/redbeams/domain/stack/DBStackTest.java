package com.sequenceiq.redbeams.domain.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.model.common.Status;

public class DBStackTest {

    private static final DatabaseServer SERVER = new DatabaseServer();

    private static final Json TAGS = new Json("{}");

    private static final Map<String, String> PARAMETERS = ImmutableMap.of("foo", "bar");

    private static final DBStackStatus STATUS = new DBStackStatus();

    private static final String DATABASE_SERVER_ATTRIBUTES_WITH_ENGINE = "{ \"engineVersion\": \"10\", \"this\": \"that\" }";

    private static final String DATABASE_SERVER_ATTRIBUTES_WITH_DB = "{ \"dbVersion\": \"10\", \"this\": \"that\" }";

    private static final Json SAMPLE_JSON = new Json("{\"test\": \"test\"}");

    static {
        STATUS.setStatus(Status.AVAILABLE);
        STATUS.setStatusReason("because");
    }

    private final DBStack dbStack = new DBStack();

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

        Crn ownerCrn = Crn.safeFromString("crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com");
        dbStack.setOwnerCrn(ownerCrn);
        assertEquals(ownerCrn, dbStack.getOwnerCrn());

        dbStack.setUserName("username");
        assertEquals("username", dbStack.getUserName());

        dbStack.setDBStackStatus(STATUS);
        assertEquals(STATUS, dbStack.getDbStackStatus());
        assertEquals(STATUS.getStatus(), dbStack.getStatus());
        assertEquals(STATUS.getStatusReason(), dbStack.getStatusReason());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AZURE", "MOCK", "YARN"})
    public void testIsHaWhenNotAwsThenTrue(CloudPlatform cloudPlatform) {
        dbStack.setCloudPlatform(cloudPlatform.name());

        assertTrue(dbStack.isHa());
    }

    @Test
    public void testIsHaWhenAwsWithNoParametersThenTrue() {
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        assertTrue(dbStack.isHa());
    }

    @Test
    public void testIsHaWhenAwsWithEmptyParametersThenTrue() {
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        dbStack.setParameters(new HashMap<>());

        assertTrue(dbStack.isHa());
    }

    @Test
    public void testIsHaWhenAwsWithMultiAzParameterTrueThenTrue() {
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        dbStack.setParameters(Map.of("multiAZ", Boolean.TRUE.toString()));

        assertTrue(dbStack.isHa());
    }

    @Test
    public void testIsHaWhenAwsWithMultiAzParameterFalseThenFalse() {
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        dbStack.setParameters(Map.of("multiAZ", Boolean.FALSE.toString()));

        assertFalse(dbStack.isHa());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class)
    public void testGetMajorVersion(CloudPlatform cloudPlatform) {
        dbStack.setCloudPlatform(cloudPlatform.name());
        setDatabaseServer(dbStack, cloudPlatform);
        assertEquals(MajorVersion.VERSION_10, dbStack.getMajorVersion());
    }

    private static void setDatabaseServer(DBStack dbStack, CloudPlatform cloudPlatform) {
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(
                CloudPlatform.AZURE == cloudPlatform ?
                        DATABASE_SERVER_ATTRIBUTES_WITH_DB :
                        DATABASE_SERVER_ATTRIBUTES_WITH_ENGINE));
        dbStack.setDatabaseServer(databaseServer);
    }

    @ParameterizedTest
    @MethodSource("provideJsonContentsPerProvider")
    public void testSetMajorVersion(CloudPlatform cloudPlatform, Json expectedJson) {
        dbStack.setCloudPlatform(cloudPlatform.name());
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(SAMPLE_JSON);
        dbStack.setDatabaseServer(databaseServer);
        dbStack.setMajorVersion(MajorVersion.VERSION_11);
        assertEquals(expectedJson, dbStack.getDatabaseServer().getAttributes());
    }

    private static Stream<Arguments> provideJsonContentsPerProvider() {
        return Stream.of(
                Arguments.of(CloudPlatform.AWS,
                        new Json("{\"engineVersion\":\"11\",\"cloudPlatform\":\"AWS\"}")),
                Arguments.of(CloudPlatform.GCP,
                        new Json("{\"engineVersion\":\"11\",\"cloudPlatform\":\"GCP\"}")),
                Arguments.of(CloudPlatform.AZURE,
                        new Json("{\"cloudPlatform\":\"AZURE\",\"AZURE_DATABASE_TYPE\":\"SINGLE_SERVER\"," +
                                "\"geoRedundantBackup\":false,\"dbVersion\":\"11\",\"storageAutoGrow\":false}")),
                Arguments.of(CloudPlatform.MOCK, SAMPLE_JSON),
                Arguments.of(CloudPlatform.YARN, SAMPLE_JSON),
                Arguments.of(CloudPlatform.OPENSTACK, SAMPLE_JSON)
        );
    }
}
