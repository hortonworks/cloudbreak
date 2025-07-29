package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
        ReflectionTestUtils.setField(underTest, "targetMajorVersion", TargetMajorVersion.VERSION_11);
        Map<String, Object> actualResult = underTest.createParamsForEmbeddedDBUpgradePreparation(stackDto);
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        Assertions.assertEquals(upgradeParams.get("original_postgres_version"), "version");
        Assertions.assertEquals(upgradeParams.get("original_postgres_binaries"), "/usr/pgsql-version");
        Assertions.assertEquals(upgradeParams.get("temp_directory"), "/dbfs/tmp");
        Assertions.assertEquals(upgradeParams.get("new_postgres_version"), TargetMajorVersion.VERSION_11.getMajorVersion());
    }

    @Test
    void testCreateParamsIfDbVersionIsNotSet() {
        StackDto stackDto = mock(StackDto.class);
        Database database = new Database();
        when(stackDto.getDatabase()).thenReturn(database);
        ReflectionTestUtils.setField(underTest, "targetMajorVersion", TargetMajorVersion.VERSION_11);
        Map<String, Object> actualResult = underTest.createParamsForEmbeddedDBUpgradePreparation(stackDto);
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        Assertions.assertEquals(upgradeParams.get("original_postgres_version"), "10");
        Assertions.assertEquals(upgradeParams.get("original_postgres_binaries"), "/usr/pgsql-10");
    }

    @Test
    void  testCreateParamsWithPostgresVersion() {
        ReflectionTestUtils.setField(underTest, "targetMajorVersion", TargetMajorVersion.VERSION_11);
        Map<String, Object> actualResult = underTest.createParamsWithPostgresVersion();
        Map<String, Object> postgresParams = (Map<String, Object>) actualResult.get("postgres");
        Assertions.assertEquals(postgresParams.get("postgres_version"), TargetMajorVersion.VERSION_11.getMajorVersion());
    }
}
