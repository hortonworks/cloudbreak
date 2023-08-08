package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class DatabaseDefaultVersionProviderTest {

    private DatabaseDefaultVersionProvider underTest;

    @BeforeEach
    public void init() {
        underTest = new DatabaseDefaultVersionProvider();
    }

    static Object[][] testInput() {
        return new Object[][]{
                {"Version already set, runtime older, os centos7", "7.2.10", "centos7", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version already set, runtime older, os redhat8", "7.2.10", "redhat8", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version already set, runtime same, os centos7", "7.2.12", "centos7", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version already set, runtime same, os redhat8", "7.2.12", "redhat8", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version already set, runtime newer, os centos7", "7.2.14", "centos7", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version already set, runtime newer, os redhat8", "7.2.14", "redhat8", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version not set, runtime older, os centos7", "7.2.10", "centos7", null, "7.2.12", "11", null, CloudPlatform.AWS, false},
                {"Version not set, runtime older, os redhat8", "7.2.10", "redhat8", null, "7.2.12", "11", null, CloudPlatform.AWS, false},
                {"Version not set, runtime same, os centos7", "7.2.12", "centos7", null, "7.2.12", "11", "11", CloudPlatform.AWS, false},
                {"Version not set, runtime same, os redhat8", "7.2.12", "redhat8", null, "7.2.12", "11", "11", CloudPlatform.AWS, false},
                {"Version not set, runtime newer, os centos7", "7.2.14", "centos7", null, "7.2.12", "11", "11", CloudPlatform.AWS, false},
                {"Version not set, runtime newer, os redhat8", "7.2.14", "redhat8", null, "7.2.12", "11", "11", CloudPlatform.AWS, false},
                {"Version not set, runtime null, os centos7", null, "centos7", null, "7.2.12", "11", null, CloudPlatform.AWS, false},
                {"Version not set, runtime null, os redhat8", null, "redhat8", null, "7.2.12", "11", "11", CloudPlatform.AWS, false},
                {"Version already set, runtime null, os centos7", null, "centos7", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version already set, runtime null, os redhat8", null, "redhat8", "10", "7.2.12", "11", "10", CloudPlatform.AWS, false},
                {"Version not set, runtime newer, os centos7, Azure embedded", "7.2.14", "centos7", null, "7.2.12", "14", "14", CloudPlatform.AZURE, false},
                {"Version not set, runtime newer, os centos7, Azure external", "7.2.14", "centos7", null, "7.2.12", "14", "11", CloudPlatform.AZURE, true},
                {"Version not set, runtime newer, os redhat8, Azure embedded", "7.2.14", "redhat8", null, "7.2.12", "14", "14", CloudPlatform.AZURE, false},
                {"Version not set, runtime newer, os redhat8, Azure external", "7.2.14", "redhat8", null, "7.2.12", "14", "11", CloudPlatform.AZURE, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testInput")
    public void testCalculateDbVersionBasedOnRuntimeIfMissing(String name, String runtime, String os, String externalDatabaseEngineVersion, String minRuntime,
            String dbEngineVersion, String expected, CloudPlatform cloudPlatform, boolean externalDb) {
        ReflectionTestUtils.setField(underTest, "minRuntimeVersion", minRuntime);
        ReflectionTestUtils.setField(underTest, "dbEngineVersion", dbEngineVersion);

        String result = underTest.calculateDbVersionBasedOnRuntimeAndOsIfMissing(runtime, os, externalDatabaseEngineVersion, cloudPlatform, externalDb);

        assertEquals(expected, result);
    }

}