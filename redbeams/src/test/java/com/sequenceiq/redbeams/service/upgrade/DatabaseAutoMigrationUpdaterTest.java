package com.sequenceiq.redbeams.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationException;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationParams;
import com.sequenceiq.cloudbreak.cloud.exception.RdsAutoMigrationException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class DatabaseAutoMigrationUpdaterTest {
    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBResourceService dbResourceService;

    @InjectMocks
    private DatabaseAutoMigrationUpdater underTest;

    @Test
    void testUpdateDatabaseIfAutoMigrationHappenedNoMigrate() {
        DBStack dbStack = new DBStack();

        underTest.updateDatabaseIfAutoMigrationHappened(dbStack, null);

        verify(dbStackService, never()).save(dbStack);
    }

    @Test
    void testUpdateDatabaseIfAutoMigrationHappenedNoAzureMigrate() {
        DBStack dbStack = new DBStack();
        RdsAutoMigrationException exception = new RdsAutoMigrationException("exception");

        underTest.updateDatabaseIfAutoMigrationHappened(dbStack, exception);

        verify(dbStackService, never()).save(dbStack);
    }

    @Test
    void testUpdateDatabaseIfAutoMigrationHappenedAzureMigrate() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        dbStack.setId(1L);
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setName("dbserver");
        dbStack.setDatabaseServer(databaseServer);
        AzureRDSAutoMigrationParams azureRDSAutoMigrationParams = new AzureRDSAutoMigrationParams(AzureDatabaseType.FLEXIBLE_SERVER, "serverid");
        RdsAutoMigrationException exception = new AzureRDSAutoMigrationException("exception", azureRDSAutoMigrationParams);
        DBResource dbResource = new DBResource();
        when(dbResourceService.findByStackAndNameAndType(1L, "dbserver", ResourceType.AZURE_DATABASE)).thenReturn(Optional.of(dbResource));

        underTest.updateDatabaseIfAutoMigrationHappened(dbStack, exception);

        assertEquals("serverid", dbResource.getResourceReference());
        assertEquals(databaseServer.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY),
                AzureDatabaseType.FLEXIBLE_SERVER.name());
        verify(dbResourceService, times(1)).save(dbResource);
        verify(dbStackService, times(1)).save(dbStack);
    }

    @Test
    void testUpdateDatabaseIfAutoMigrationHappenedAzureMigrateNoDbResource() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        dbStack.setId(1L);
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setName("dbserver");
        dbStack.setDatabaseServer(databaseServer);
        AzureRDSAutoMigrationParams azureRDSAutoMigrationParams = new AzureRDSAutoMigrationParams(AzureDatabaseType.FLEXIBLE_SERVER, "serverid");
        RdsAutoMigrationException exception = new AzureRDSAutoMigrationException("exception", azureRDSAutoMigrationParams);
        when(dbResourceService.findByStackAndNameAndType(1L, "dbserver", ResourceType.AZURE_DATABASE)).thenReturn(Optional.empty());

        underTest.updateDatabaseIfAutoMigrationHappened(dbStack, exception);

        verify(dbResourceService, never()).save(any());
    }

    @Test
    void testUpdateDatabaseIfAutoMigrationHappenedAzureMigrateNoMigrationParams() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AZURE.name());
        dbStack.setId(1L);
        dbStack.setName("dbserver");
        DatabaseServer databaseServer = new DatabaseServer();
        dbStack.setDatabaseServer(databaseServer);
        RdsAutoMigrationException exception = new AzureRDSAutoMigrationException("exception", null);

        underTest.updateDatabaseIfAutoMigrationHappened(dbStack, exception);

        verify(dbResourceService, never()).findByStackAndNameAndType(anyLong(), anyString(), any());
        verify(dbResourceService, never()).save(any());
        verify(dbStackService, never()).save(any());
    }
}
