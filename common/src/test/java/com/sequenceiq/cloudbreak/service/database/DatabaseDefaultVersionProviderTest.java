package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.VersionComparator;

@ExtendWith(MockitoExtension.class)
class DatabaseDefaultVersionProviderTest {

    private static final String MIN_RUNTIME_VERSION = "7.2.12";

    private static final String ENGINE_VERSION = "11";

    private static final String MIN_RUNTIME_VERSION_2 = "7.3.2";

    private static final String ENGINE_VERSION_2 = "17";

    @InjectMocks
    private DatabaseDefaultVersionProvider underTest;

    @Mock
    private DbOverrideConfig dbOverrideConfig;

    private final VersionComparator versionComparator = new VersionComparator();

    @BeforeEach
    public void init() {
        lenient().when(dbOverrideConfig.findMinEngineVersion()).thenReturn(ENGINE_VERSION);
    }

    static Object[][] testInput() {
        return new Object[][]{
                {"Version already set, runtime older", "7.2.10", "10", "10"},
                {"Version already set, runtime same", "7.2.12", "10", "10"},
                {"Version already set, runtime newer", "7.2.14", "10", "10"},
                {"Version not set, runtime older", "7.2.10", null, "11"},
                {"Version not set, runtime same", "7.2.12", null, "11"},
                {"Version not set, runtime newer", "7.2.14", null, "11"},
                {"Version not set, runtime null, os redhat8", null, null, "11"},
                {"Version not set, runtime newest", "7.3.2", null, "17"},
                {"Version already set, runtime null", null, "10", "10"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testInput")
    public void testCalculateDbVersionBasedOnRuntimeIfMissing(String name, String runtime, String externalDatabaseEngineVersion,
            String expected) {
        if (externalDatabaseEngineVersion == null && runtime != null) {
            when(dbOverrideConfig.findEngineVersionForRuntime(runtime)).thenReturn(findEngineVersionForRuntime(runtime));
        }
        String result = underTest.calculateDbVersionBasedOnRuntime(runtime, externalDatabaseEngineVersion
        );

        assertEquals(expected, result);
    }

    private String findEngineVersionForRuntime(String runtime) {
        if (versionComparator.compare(() -> runtime, () -> MIN_RUNTIME_VERSION_2) >= 0) {
            return ENGINE_VERSION_2;
        } else if (versionComparator.compare(() -> runtime, () -> MIN_RUNTIME_VERSION) >= 0) {
            return ENGINE_VERSION;
        }
        return null;
    }

}