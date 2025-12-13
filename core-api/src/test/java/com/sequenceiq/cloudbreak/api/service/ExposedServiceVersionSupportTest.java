package com.sequenceiq.cloudbreak.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExposedServiceVersionSupportTest {

    private static final Optional<String> BLUEPRINT_VERSION = Optional.of("7.2.6");

    private ExposedServiceVersionSupport underTest = new ExposedServiceVersionSupport();

    static Object[][] maxVersionSupportedParameters() {
        return new Object[][]{
                { BLUEPRINT_VERSION, "7.2.5",   false },
                { BLUEPRINT_VERSION, "7.1.0",   false },
                { BLUEPRINT_VERSION, "7.0.1",   false },
                { BLUEPRINT_VERSION, null,      true },
                { BLUEPRINT_VERSION, "",        true },
                { BLUEPRINT_VERSION, "7.2.6",   true },
                { BLUEPRINT_VERSION, "7.2.7",   true },
                { BLUEPRINT_VERSION, "7.2.8",   true },
        };
    }

    @ParameterizedTest
    @MethodSource("maxVersionSupportedParameters")
    public void testMaxSupportedVersion(Optional<String> blueprintVersion, String maxSupportedVersion, boolean include) {
        assertEquals(include, underTest.maxVersionSupported(blueprintVersion, maxSupportedVersion));
    }

    static Object[][] minVersionSupportedParameters() {
        return new Object[][]{
                { BLUEPRINT_VERSION, "7.2.5",   true },
                { BLUEPRINT_VERSION, "7.1.0",   true },
                { BLUEPRINT_VERSION, "7.0.1",   true },
                { BLUEPRINT_VERSION, null,      true },
                { BLUEPRINT_VERSION, "",        true },
                { BLUEPRINT_VERSION, "7.2.6",   true },
                { BLUEPRINT_VERSION, "7.2.7",   false },
                { BLUEPRINT_VERSION, "7.2.8",   false },
        };
    }

    @ParameterizedTest
    @MethodSource("minVersionSupportedParameters")
    public void testMinSupportedVersion(Optional<String> blueprintVersion, String minSupportedVersion, boolean include) {
        assertEquals(include, underTest.minVersionSupported(blueprintVersion, minSupportedVersion));
    }
}