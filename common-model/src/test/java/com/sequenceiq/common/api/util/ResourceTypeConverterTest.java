package com.sequenceiq.common.api.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResourceTypeConverterTest {

    @ParameterizedTest
    @MethodSource("resources")
    public void testConvertToHumanReadableName(String resource, String humanReadableName) {
        String actual = ResourceTypeConverter.convertToHumanReadableName(resource);
        Assertions.assertThat(actual).isEqualTo(humanReadableName);
    }

    static Object[][] resources() {
        return new Object[][] {
                // resource      human readable name
                { "WORKLOAD",    "Data Hub" },
                { "DATAHUB",     "Data Hub" },
                { "DATALAKE",    "Data Lake" },
                { "SDX",         "Data Lake" },
                { "FREEIPA",     "FreeIPA" },
                { "REDBEAMS",    "External Database" },
        };
    }
}
