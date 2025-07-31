package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class UpgradeEmbeddedDBStateParamsProviderTest {

    @InjectMocks
    private UpgradeEmbeddedDBStateParamsProvider underTest;

    @Test
    void testCreateParamsIfDbVersionIsSet() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getExternalDatabaseEngineVersion()).thenReturn("version");
        Map<String, Object> actualResult = underTest.createParamsForEmbeddedDBUpgrade(stackDto, TargetMajorVersion.VERSION_11.getMajorVersion());
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        Assertions.assertEquals(upgradeParams.get("original_postgres_version"), "version");
        Assertions.assertEquals(upgradeParams.get("original_postgres_binaries"), "/dbfs/tmp/pgsql-version");
        Assertions.assertEquals(upgradeParams.get("original_postgres_directory"), "/dbfs/pgsql");
        Assertions.assertEquals(upgradeParams.get("new_postgres_version"), TargetMajorVersion.VERSION_11.getMajorVersion());
    }

    @Test
    void testCreateParamsIfDbVersionIsNotSet() {
        StackDto stackDto = mock(StackDto.class);
        Map<String, Object> actualResult = underTest.createParamsForEmbeddedDBUpgrade(stackDto, TargetMajorVersion.VERSION_11.getMajorVersion());
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        Assertions.assertEquals(upgradeParams.get("original_postgres_version"), "10");
        Assertions.assertEquals(upgradeParams.get("original_postgres_binaries"), "/dbfs/tmp/pgsql-10");
    }
}
