package com.sequenceiq.redbeams.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDBStorageCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.service.DatabaseCapabilityService;

@ExtendWith(MockitoExtension.class)
public class DBUpgradeMigrationServiceTest {
    private static final String NEW_TEMPLATE = "NEW_TEMPLATE_CONTENT";

    private static final String ORIGINAL_TEMPLATE = "ORIGINAL_TEMPLATE";

    private static final SortedSet<Long> DB_STORAGE_SIZES = new TreeSet<>(List.of(128 * 1024L, 256 * 1024L, 512 * 1024L));

    private static final Long GB_TO_MB = 1024L;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Mock
    private CloudConnector connector;

    @Mock
    private DatabaseCapabilityService databaseCapabilityService;

    @InjectMocks
    private DBUpgradeMigrationService dbUpgradeMigrationService;

    @Test
    void testMergeDatabaseStacksTemplateSupported() throws TemplatingNotSupportedException {
        // Given
        DBStack originalDbStack = mock(DBStack.class);
        DatabaseStack migratedDatabaseStack = mock(DatabaseStack.class);
        UpgradeDatabaseMigrationParams migrationParams = getMigrationParams();
        DatabaseServer originalDatabaseServer = mock(DatabaseServer.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        PlatformResources platformResources = mock(PlatformResources.class);
        com.sequenceiq.redbeams.domain.stack.DatabaseServer originalDatabaseServerEntity = new com.sequenceiq.redbeams.domain.stack.DatabaseServer();
        Network network = mock(Network.class);

        when(databaseStackConverter.convert(originalDbStack)).thenReturn(migratedDatabaseStack);
        when(migratedDatabaseStack.getNetwork()).thenReturn(network);
        when(connector.resources()).thenReturn(resourceConnector);
        when(connector.resources().getDBStackTemplate(migratedDatabaseStack)).thenReturn(NEW_TEMPLATE);
        when(connector.platformResources()).thenReturn(platformResources);
        when(platformResources.databaseStorageCapabilities(any(), any())).thenReturn(Optional.of(
                new PlatformDBStorageCapabilities(DB_STORAGE_SIZES)));
        when(migratedDatabaseStack.getDatabaseServer()).thenReturn(originalDatabaseServer);
        when(originalDbStack.getDatabaseServer()).thenReturn(originalDatabaseServerEntity);

        // When
        DatabaseStack result = dbUpgradeMigrationService.mergeDatabaseStacks(originalDbStack, migrationParams, connector, null, null,
                new ExternalDatabaseParameters(ExternalDatabaseStatus.STARTED, AzureDatabaseType.SINGLE_SERVER, 200 * GB_TO_MB));

        // Then
        assertNotNull(result);
        assertEquals(network, result.getNetwork());
        assertEquals(NEW_TEMPLATE, result.getTemplate());
        assertCommonParameters("Standard_E4ds_v4", 256L);
    }

    @Test
    void testMergeDatabaseStacksTemplateNotSupported() throws TemplatingNotSupportedException {
        // Given
        DBStack originalDbStack = mock(DBStack.class);
        DatabaseStack migratedDatabaseStack = mock(DatabaseStack.class);
        UpgradeDatabaseMigrationParams migrationParams = getMigrationParams();
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        PlatformResources platformResources = mock(PlatformResources.class);
        com.sequenceiq.redbeams.domain.stack.DatabaseServer originalDatabaseServerEntity = new com.sequenceiq.redbeams.domain.stack.DatabaseServer();
        Network network = mock(Network.class);

        when(databaseStackConverter.convert(originalDbStack)).thenReturn(migratedDatabaseStack);
        when(migratedDatabaseStack.getNetwork()).thenReturn(network);
        when(migratedDatabaseStack.getTemplate()).thenReturn(ORIGINAL_TEMPLATE);
        when(connector.resources()).thenReturn(resourceConnector);
        when(connector.resources().getDBStackTemplate(migratedDatabaseStack)).thenThrow(TemplatingNotSupportedException.class);
        when(connector.platformResources()).thenReturn(platformResources);
        when(platformResources.databaseStorageCapabilities(any(), any())).thenReturn(Optional.empty());
        when(originalDbStack.getDatabaseServer()).thenReturn(originalDatabaseServerEntity);

        // When
        DatabaseStack result = dbUpgradeMigrationService.mergeDatabaseStacks(originalDbStack, migrationParams, connector, null, null,
                new ExternalDatabaseParameters(ExternalDatabaseStatus.STARTED, AzureDatabaseType.SINGLE_SERVER, 300 * 1024L));

        // Then
        assertNotNull(result);
        assertEquals(network, result.getNetwork());
        assertEquals(ORIGINAL_TEMPLATE, result.getTemplate());
        assertCommonParameters("Standard_E4ds_v4", 512L);
    }

    @Test
    void testMergeDatabaseStacksNoInstanceType() throws TemplatingNotSupportedException {
        // Given
        DBStack originalDbStack = mock(DBStack.class);
        DatabaseStack migratedDatabaseStack = mock(DatabaseStack.class);
        UpgradeDatabaseMigrationParams migrationParams = getMigrationParams();
        migrationParams.setInstanceType(null);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudPlatformVariant cloudPlatformVariant = mock(CloudPlatformVariant.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        com.sequenceiq.redbeams.domain.stack.DatabaseServer originalDatabaseServerEntity = new com.sequenceiq.redbeams.domain.stack.DatabaseServer();
        Network network = mock(Network.class);

        when(databaseStackConverter.convert(originalDbStack)).thenReturn(migratedDatabaseStack);
        when(migratedDatabaseStack.getNetwork()).thenReturn(network);
        when(migratedDatabaseStack.getTemplate()).thenReturn(ORIGINAL_TEMPLATE);
        when(connector.resources()).thenReturn(resourceConnector);
        when(connector.resources().getDBStackTemplate(migratedDatabaseStack)).thenThrow(TemplatingNotSupportedException.class);
        when(originalDbStack.getDatabaseServer()).thenReturn(originalDatabaseServerEntity);
        when(databaseCapabilityService.getDefaultInstanceType(connector, cloudCredential, cloudPlatformVariant,
                Region.region(originalDbStack.getRegion()))).thenReturn("defaultInstanceType");

        // When
        DatabaseStack result = dbUpgradeMigrationService.mergeDatabaseStacks(originalDbStack, migrationParams, connector,
                cloudCredential, cloudPlatformVariant, new ExternalDatabaseParameters(ExternalDatabaseStatus.STARTED, AzureDatabaseType.SINGLE_SERVER, null));

        // Then
        assertNotNull(result);
        assertEquals(network, result.getNetwork());
        assertEquals(ORIGINAL_TEMPLATE, result.getTemplate());
        assertCommonParameters("defaultInstanceType");
    }

    private void assertCommonParameters(String instanceType) {
        assertCommonParameters(instanceType, 128L);
    }

    private void assertCommonParameters(String instanceType, Long storageSize) {
        ArgumentCaptor<DBStack> dbStackArgumentCaptor = ArgumentCaptor.forClass(DBStack.class);
        verify(databaseStackConverter).convert(dbStackArgumentCaptor.capture());
        com.sequenceiq.redbeams.domain.stack.DatabaseServer actual = dbStackArgumentCaptor.getValue().getDatabaseServer();
        assertEquals(storageSize, actual.getStorageSize());
        assertEquals(instanceType, actual.getInstanceType());
        assertEquals("test", actual.getAttributes().getString("key"));
    }

    private UpgradeDatabaseMigrationParams getMigrationParams() {
        UpgradeDatabaseMigrationParams migrationParams = new UpgradeDatabaseMigrationParams();
        migrationParams.setStorageSize(128L);
        migrationParams.setInstanceType("Standard_E4ds_v4");
        Map<String, Object> parameters = Map.of("key", "test");
        Json attributes = new Json(parameters);
        migrationParams.setAttributes(attributes);
        return migrationParams;
    }
}
