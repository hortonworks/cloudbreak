package com.sequenceiq.datalake.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfig;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

class PlatformConfigTest {

    private PlatformConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new PlatformConfig();
        ReflectionTestUtils.setField(underTest, "dbServiceSslEnforcementSupportedPlatforms", EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE));
        ReflectionTestUtils.setField(underTest, "allPossibleExternalDbPlatforms", Set.of(CloudPlatform.AZURE));
    }

    static Object[][] sslEnforcementPlatformsDataProvider() {
        return new Object[][]{
                // cloudPlatform resultExpected
                {CloudPlatform.AWS, true},
                {CloudPlatform.AZURE, true},
                {CloudPlatform.GCP, false},
                {CloudPlatform.MOCK, false},
                {CloudPlatform.YARN, false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementPlatformsDataProvider")
    void isExternalDatabaseSslEnforcementSupportedForTest(CloudPlatform cloudPlatform, boolean resultExpected) {
        assertThat(underTest.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform)).isEqualTo(resultExpected);
    }

    @Test
    void testDatabaseConfigs() throws IOException {
        Map<DatabaseConfigKey, DatabaseConfig> actualResult = underTest.databaseConfigs();
        DatabaseConfig singleDatabaseStackConfig = actualResult.get(
                new DatabaseConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, AzureDatabaseType.SINGLE_SERVER));
        Assertions.assertEquals("MO_Gen5_4", singleDatabaseStackConfig.getInstanceType());
        Assertions.assertEquals(100, singleDatabaseStackConfig.getVolumeSize());
        DatabaseConfig flexibleDatabaseStackConfig = actualResult.get(
                new DatabaseConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, AzureDatabaseType.FLEXIBLE_SERVER));
        Assertions.assertEquals("Standard_E4ds_v4", flexibleDatabaseStackConfig.getInstanceType());
        Assertions.assertEquals(128, flexibleDatabaseStackConfig.getVolumeSize());
    }
}
