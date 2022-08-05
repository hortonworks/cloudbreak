package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class DatabaseDefaultVersionProviderTest {

    private DatabaseDefaultVersionProvider underTest;

    @BeforeEach
    public void init() {
        underTest = new DatabaseDefaultVersionProvider();
    }

    static Object[][] testInput() {
        return new Object[][]{
                {"Version already set, runtime older", "7.2.10", "10", "7.2.12", "11", "10"},
                {"Version already set, runtime same", "7.2.12", "10", "7.2.12", "11", "10"},
                {"Version already set, runtime newer", "7.2.14", "10", "7.2.12", "11", "10"},
                {"Version not set, runtime older", "7.2.10", null, "7.2.12", "11", null},
                {"Version not set, runtime same", "7.2.12", null, "7.2.12", "11", "11"},
                {"Version not set, runtime newer", "7.2.14", null, "7.2.12", "11", "11"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testInput")
    public void testCalculateDbVersionBasedOnRuntimeIfMissing(String name, String runtime, String externalDatabaseEngineVersion, String minRuntime,
            String dbEngineVersion, String expected) {
        ReflectionTestUtils.setField(underTest, "minRuntimeVersion", minRuntime);
        ReflectionTestUtils.setField(underTest, "dbEngineVersion", dbEngineVersion);

        String result = underTest.calculateDbVersionBasedOnRuntimeIfMissing(runtime, externalDatabaseEngineVersion);

        assertEquals(expected, result);
    }

}