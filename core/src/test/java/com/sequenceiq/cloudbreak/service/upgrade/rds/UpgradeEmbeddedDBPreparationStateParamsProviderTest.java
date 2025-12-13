package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class UpgradeEmbeddedDBPreparationStateParamsProviderTest {

    @InjectMocks
    private UpgradeEmbeddedDBPreparationStateParamsProvider underTest;

    @Test
    void testCreateParamsIfDbVersionIsSet() {
        StackDto stackDto = mock(StackDto.class);
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("version");
        when(stackDto.getDatabase()).thenReturn(database);
        Map<String, Object> actualResult = underTest.createParamsForEmbeddedDBUpgradePreparation(stackDto, TargetMajorVersion.VERSION_11.getMajorVersion());
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        assertEquals(upgradeParams.get("original_postgres_version"), "version");
        assertEquals(upgradeParams.get("original_postgres_binaries"), "/usr/pgsql-version");
        assertEquals(upgradeParams.get("temp_directory"), "/dbfs/tmp");
        assertEquals(upgradeParams.get("new_postgres_version"), TargetMajorVersion.VERSION_11.getMajorVersion());
    }

    @Test
    void testCreateParamsIfDbVersionIsNotSet() {
        StackDto stackDto = mock(StackDto.class);
        Database database = new Database();
        when(stackDto.getDatabase()).thenReturn(database);
        Map<String, Object> actualResult = underTest.createParamsForEmbeddedDBUpgradePreparation(stackDto, TargetMajorVersion.VERSION_11.getMajorVersion());
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        assertEquals(upgradeParams.get("original_postgres_version"), "10");
        assertEquals(upgradeParams.get("original_postgres_binaries"), "/usr/pgsql-10");
    }

    @Test
    void  testCreateParamsWithPostgresVersion() {
        Map<String, Object> actualResult = underTest.createParamsWithPostgresVersion(TargetMajorVersion.VERSION_11.getMajorVersion());
        Map<String, Object> postgresParams = (Map<String, Object>) actualResult.get("postgres");
        assertEquals(postgresParams.get("postgres_version"), TargetMajorVersion.VERSION_11.getMajorVersion());
    }
}
