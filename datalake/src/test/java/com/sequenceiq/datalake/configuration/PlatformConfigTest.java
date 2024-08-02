package com.sequenceiq.datalake.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfig;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

class PlatformConfigTest {

    private PlatformConfig underTest;

    private final Map<CloudPlatform, Set<? extends DatabaseType>> databaseTypeMap = Map.of(CloudPlatform.AZURE, EnumSet.allOf(AzureDatabaseType.class));

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
        assertEquals("MO_Gen5_4", singleDatabaseStackConfig.getInstanceType());
        assertEquals(100, singleDatabaseStackConfig.getVolumeSize());
        DatabaseConfig flexibleDatabaseStackConfig = actualResult.get(
                new DatabaseConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, AzureDatabaseType.FLEXIBLE_SERVER));
        assertNull(flexibleDatabaseStackConfig.getInstanceType());
        assertEquals(128, flexibleDatabaseStackConfig.getVolumeSize());
    }

    @Test
    void testDatabaseConfigsNotReturnedForUnsupportedShapes() throws IOException {
        List<SdxClusterShape> unsupportedShapes = Arrays.stream(SdxClusterShape.values())
                .filter(SdxClusterShape::isDbConfigUnsupported)
                .toList();

        Map<DatabaseConfigKey, DatabaseConfig> actualResult = underTest.databaseConfigs();
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            Set<? extends DatabaseType> databaseTypes = databaseTypeMap.getOrDefault(cloudPlatform, Set.of());
            for (SdxClusterShape shape : unsupportedShapes) {
                if (databaseTypes.isEmpty()) {
                    assertFalse(actualResult.containsKey(new DatabaseConfigKey(cloudPlatform, shape, null)));
                } else {
                    for (DatabaseType databaseType : databaseTypes) {
                        assertFalse(actualResult.containsKey(new DatabaseConfigKey(cloudPlatform, shape, databaseType)));
                    }
                }
            }
        }
    }

    @Test
    void initPlatformsTest() {
        assertThat(underTest.getAllPossibleExternalDbPlatforms()).isEqualTo(Set.of(CloudPlatform.AZURE));
        assertThat(underTest.getMultiAzSupportedPlatforms()).isNull();

        ReflectionTestUtils.setField(underTest, "dbServiceSupportedPlatforms", EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE));
        ReflectionTestUtils.setField(underTest, "dbServiceExperimentalPlatforms", EnumSet.of(CloudPlatform.OPENSTACK, CloudPlatform.YARN));

        underTest.initPlatforms();

        assertThat(underTest.getAllPossibleExternalDbPlatforms()).isEqualTo(EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE, CloudPlatform.OPENSTACK,
                CloudPlatform.YARN));
        assertThat(underTest.getMultiAzSupportedPlatforms()).isEqualTo(EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE));
    }

}
