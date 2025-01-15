package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabasePropertiesV4Response;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;

@ExtendWith(MockitoExtension.class)
class DatabaseServerConfigToDatabasePropertiesV4ResponseConverterTest {

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String RESOURCE_ID = "resource-id";

    @InjectMocks
    private DatabaseServerConfigToDatabasePropertiesV4ResponseConverter underTest;

    @Test
    void testConvertWhenTheDatabaseTypeIsSingleServer() {
        DBResource resourceGroupResource = new DBResource();
        resourceGroupResource.setResourceType(ResourceType.AZURE_RESOURCE_GROUP);
        resourceGroupResource.setResourceName(RESOURCE_GROUP);
        DBResource resourceIdResource = new DBResource();
        resourceIdResource.setResourceType(ResourceType.AZURE_DATABASE);
        resourceIdResource.setResourceReference(RESOURCE_ID);

        DBStack dbStack = new DBStack();
        dbStack.setDatabaseResources(Set.of(resourceGroupResource, resourceIdResource));
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER)));
        dbStack.setDatabaseServer(databaseServer);

        DatabaseServerConfig source = new DatabaseServerConfig();
        source.setDbStack(dbStack);

        DatabasePropertiesV4Response actual = underTest.convert(source);

        assertEquals(AzureDatabaseType.SINGLE_SERVER.name(), actual.getDatabaseType());
        assertEquals(ConnectionNameFormat.USERNAME_WITH_HOSTNAME, actual.getConnectionNameFormat());
        assertEquals(RESOURCE_GROUP, actual.getAzure().getResourceGroup());
        assertEquals(RESOURCE_ID, actual.getAzure().getResourceId());
    }

    @Test
    void testConvertWhenTheDatabaseTypeIsFlexibleServer() {
        DBResource resourceGroupResource = new DBResource();
        resourceGroupResource.setResourceType(ResourceType.AZURE_RESOURCE_GROUP);
        resourceGroupResource.setResourceName(RESOURCE_GROUP);
        DBResource resourceIdResource = new DBResource();
        resourceIdResource.setResourceType(ResourceType.AZURE_DATABASE);
        resourceIdResource.setResourceReference(RESOURCE_ID);

        DBStack dbStack = new DBStack();
        dbStack.setDatabaseResources(Set.of(resourceGroupResource, resourceIdResource));
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER)));
        dbStack.setDatabaseServer(databaseServer);

        DatabaseServerConfig source = new DatabaseServerConfig();
        source.setDbStack(dbStack);

        DatabasePropertiesV4Response actual = underTest.convert(source);

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER.name(), actual.getDatabaseType());
        assertEquals(ConnectionNameFormat.USERNAME_ONLY, actual.getConnectionNameFormat());
        assertEquals(RESOURCE_GROUP, actual.getAzure().getResourceGroup());
        assertEquals(RESOURCE_ID, actual.getAzure().getResourceId());
    }

    @Test
    void testConvertWhenTheDbResourcesAreEmpty() {
        DBStack dbStack = new DBStack();
        dbStack.setDatabaseResources(Collections.emptySet());
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER)));
        dbStack.setDatabaseServer(databaseServer);

        DatabaseServerConfig source = new DatabaseServerConfig();
        source.setDbStack(dbStack);

        DatabasePropertiesV4Response actual = underTest.convert(source);

        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER.name(), actual.getDatabaseType());
        assertEquals(ConnectionNameFormat.USERNAME_ONLY, actual.getConnectionNameFormat());
        assertNull(actual.getAzure().getResourceGroup());
        assertNull(actual.getAzure().getResourceId());
    }

    @Test
    void testConvertWhenTheCloudPlatformIsAws() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        DatabaseServerConfig source = new DatabaseServerConfig();
        source.setDbStack(dbStack);

        DatabasePropertiesV4Response actual = underTest.convert(source);

        assertNull(actual.getDatabaseType());
        assertNull(actual.getAzure());
        assertEquals(ConnectionNameFormat.USERNAME_ONLY, actual.getConnectionNameFormat());
    }

}