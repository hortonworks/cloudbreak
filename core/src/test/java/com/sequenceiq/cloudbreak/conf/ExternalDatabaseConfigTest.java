package com.sequenceiq.cloudbreak.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfigKey;
import com.sequenceiq.common.model.AzureDatabaseType;

class ExternalDatabaseConfigTest {

    private ExternalDatabaseConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExternalDatabaseConfig();
        ReflectionTestUtils.setField(underTest, "dbServiceSslEnforcementSupportedPlatforms", Set.of(CloudPlatform.AWS, CloudPlatform.AZURE));
        ReflectionTestUtils.setField(underTest, "allPossibleExternalDbPlatforms", Set.of(CloudPlatform.AZURE));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"}, mode = EnumSource.Mode.INCLUDE)
    void isExternalDatabaseSslEnforcementSupportedForTestWhenTrue(CloudPlatform cloudPlatform) {
        assertThat(underTest.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"}, mode = EnumSource.Mode.EXCLUDE)
    void isExternalDatabaseSslEnforcementSupportedForTestWhenFalse(CloudPlatform cloudPlatform) {
        assertThat(underTest.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform)).isFalse();
    }

    @Test
    void testDatabaseConfigsAzure() throws IOException {
        Map<DatabaseStackConfigKey, DatabaseStackConfig> actualResult =  underTest.databaseConfigs();
        assertEquals(2, actualResult.size());
        DatabaseStackConfig singleDatabaseStackConfig = actualResult.get(new DatabaseStackConfigKey(CloudPlatform.AZURE, AzureDatabaseType.SINGLE_SERVER));
        assertEquals("MO_Gen5_4", singleDatabaseStackConfig.getInstanceType());
        assertEquals(100, singleDatabaseStackConfig.getVolumeSize());
        DatabaseStackConfig flexibleDatabaseStackConfig = actualResult.get(new DatabaseStackConfigKey(CloudPlatform.AZURE, AzureDatabaseType.FLEXIBLE_SERVER));
        assertEquals("Standard_E4ds_v4", flexibleDatabaseStackConfig.getInstanceType());
        assertEquals(128, flexibleDatabaseStackConfig.getVolumeSize());
    }
}
