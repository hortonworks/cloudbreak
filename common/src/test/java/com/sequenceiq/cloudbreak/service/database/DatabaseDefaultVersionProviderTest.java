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

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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
                {"Version already set, runtime older, os centos7", "7.2.10", "centos7", "10", "10", CloudPlatform.AWS},
                {"Version already set, runtime older, os redhat8", "7.2.10", "redhat8", "10", "10", CloudPlatform.AWS},
                {"Version already set, runtime same, os centos7", "7.2.12", "centos7", "10", "10", CloudPlatform.AWS},
                {"Version already set, runtime same, os redhat8", "7.2.12", "redhat8", "10", "10", CloudPlatform.AWS},
                {"Version already set, runtime newer, os centos7", "7.2.14", "centos7", "10", "10", CloudPlatform.AWS},
                {"Version already set, runtime newer, os redhat8", "7.2.14", "redhat8", "10", "10", CloudPlatform.AWS},
                {"Version not set, runtime older, os centos7", "7.2.10", "centos7", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime older, os redhat8", "7.2.10", "redhat8", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime same, os centos7", "7.2.12", "centos7", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime same, os redhat8", "7.2.12", "redhat8", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime newer, os centos7", "7.2.14", "centos7", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime newer, os redhat8", "7.2.14", "redhat8", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime null, os redhat8", null, "redhat8", null, "11", CloudPlatform.AWS},
                {"Version not set, runtime newest, os centos7", "7.3.2", "centos7", null, "17", CloudPlatform.AWS},
                {"Version not set, runtime newest, os redhat8", "7.3.2", "redhat8", null, "17", CloudPlatform.AWS},
                {"Version already set, runtime null, os centos7", null, "centos7", "10", "10", CloudPlatform.AWS},
                {"Version already set, runtime null, os redhat8", null, "redhat8", "10", "10", CloudPlatform.AWS},
                {"Version not set, runtime newer, os redhat8, Azure", "7.2.14", "redhat8", null, "11", CloudPlatform.AZURE},
                {"Version not set, runtime older, os centos7, Azure", "7.2.10", "centos7", null, "11", CloudPlatform.AZURE},
                {"Version not set, runtime newest, os centos7, Azure", "7.3.2", "centos7", null, "17", CloudPlatform.AZURE},
                {"Version not set, runtime newest, os redhat8, Azure", "7.3.2", "redhat8", null, "17", CloudPlatform.AZURE},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testInput")
    public void testCalculateDbVersionBasedOnRuntimeIfMissing(String name, String runtime, String os, String externalDatabaseEngineVersion,
            String expected, CloudPlatform cloudPlatform) {
        if (externalDatabaseEngineVersion == null && runtime != null) {
            when(dbOverrideConfig.findEngineVersionForRuntime(runtime)).thenReturn(findEngineVersionForRuntime(runtime));
        }
        String result = underTest.calculateDbVersionBasedOnRuntimeAndOsIfMissing(runtime, os, externalDatabaseEngineVersion
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