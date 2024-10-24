package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class DatabaseUpgradeBackupRestoreCheckerTest {

    @InjectMocks
    private DatabaseUpgradeBackupRestoreChecker underTest;

    @ParameterizedTest
    @MethodSource("provideTestCombinations")
    public void backupDataFromRds(CloudPlatform cloudPlatform, boolean embeddedDatabaseOnAttachedDisk, String databaseType,
            boolean runBackupRestore) {
        StackView stack = mock(StackView.class);
        ClusterView cluster = mock(ClusterView.class);
        Database database = mock(Database.class);
        initGlobalPrivateFields();
        Json attributes = mock(Json.class);
        when(attributes.getMap()).thenReturn(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, databaseType));
        when(database.getAttributes()).thenReturn(attributes);

        when(stack.getCloudPlatform()).thenReturn(cloudPlatform.name());
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(embeddedDatabaseOnAttachedDisk);

        boolean result = underTest.shouldRunDataBackupRestore(stack, cluster, database);

        assertEquals(runBackupRestore, result);
    }

    private void initGlobalPrivateFields() {
        Field cloudPlatformsToRunBackupRestore = ReflectionUtils.findField(DatabaseUpgradeBackupRestoreChecker.class, "cloudPlatformsToRunBackupRestore");
        ReflectionUtils.makeAccessible(cloudPlatformsToRunBackupRestore);
        ReflectionUtils.setField(cloudPlatformsToRunBackupRestore, underTest, Set.of(AZURE));
    }

    private static Stream<Arguments> provideTestCombinations() {
        return Stream.of(
                //cloudPlatform,embeddedDatabaseOnAttachedDisk,databaseTypeRequiresBackupRestore, runBackup
                Arguments.of(AZURE, true, "FLEXIBLE_SERVER", false),
                Arguments.of(AZURE, true, "SINGLE_SERVER", false),
                Arguments.of(AZURE, false, "FLEXIBLE_SERVER", false),
                Arguments.of(AZURE, false, "SINGLE_SERVER", true),
                Arguments.of(AWS, true, "FLEXIBLE_SERVER", false),
                Arguments.of(AWS, true, "SINGLE_SERVER", false),
                Arguments.of(AWS, false, "FLEXIBLE_SERVER", false),
                Arguments.of(AWS, false, "SINGLE_SERVER", false)
        );
    }
}