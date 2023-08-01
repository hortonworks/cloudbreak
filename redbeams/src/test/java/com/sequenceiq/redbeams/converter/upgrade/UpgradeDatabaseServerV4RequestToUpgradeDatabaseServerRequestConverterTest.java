package com.sequenceiq.redbeams.converter.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.converter.stack.DatabaseServerV4StackRequestToDatabaseServerConverter;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;

@ExtendWith(MockitoExtension.class)
public class UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverterTest {

    @Mock
    private DatabaseServerV4StackRequestToDatabaseServerConverter databaseServerV4StackRequestToDatabaseServerConverter;

    @InjectMocks
    private UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter underTest;

    @Test
    void testConvertWithMigrationParamThenSuccess() {
        // Given
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_14);
        DatabaseServerV4StackRequest upgradedDatabaseSettings = new DatabaseServerV4StackRequest();
        request.setUpgradedDatabaseSettings(upgradedDatabaseSettings);
        DatabaseServer migratedDatabaseServer = new DatabaseServer();

        when(databaseServerV4StackRequestToDatabaseServerConverter.buildDatabaseServer(any(), any())).thenReturn(migratedDatabaseServer);

        // When
        UpgradeDatabaseRequest result = underTest.convert(request);

        // Then
        assertNotNull(result);
        assertEquals(TargetMajorVersion.VERSION_14, result.getTargetMajorVersion());
        assertEquals(migratedDatabaseServer, result.getMigratedDatabaseServer());
    }

    @Test
    void testConvertNullMigrationParamThenSuccess() {
        // Given
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_11);

        // When
        UpgradeDatabaseRequest result = underTest.convert(request);

        // Then
        assertNotNull(result);
        assertEquals(TargetMajorVersion.VERSION_11, result.getTargetMajorVersion());
        assertNull(result.getMigratedDatabaseServer());
    }
}
