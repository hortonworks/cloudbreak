package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CdhVersionProviderTest {

    static Stream<Arguments> testParseVersionStringArguments() {
        return Stream.of(
                Arguments.of(null, Optional.empty(), Optional.empty(), Optional.empty()),
                Arguments.of("", Optional.empty(), Optional.empty(), Optional.empty()),
                Arguments.of("invalid-version", Optional.empty(), Optional.empty(), Optional.empty()),
                Arguments.of("7.2.18", Optional.of("7.2.18"), Optional.empty(), Optional.empty()),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101", Optional.of("7.2.18"), Optional.of(1101), Optional.empty()),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", Optional.of("7.2.18"), Optional.of(1101), Optional.of(68994679)),
                Arguments.of("7.2.18-1200", Optional.of("7.2.18"), Optional.of(1200), Optional.empty())
        );
    }

    @MethodSource("testParseVersionStringArguments")
    @ParameterizedTest
    void testParseVersionString(String versionString,
            Optional<String> expectedStackVersion, Optional<Integer> expectedPatchVersion, Optional<Integer> expectedBuildNumber) {
        assertEquals(expectedStackVersion, CdhVersionProvider.getCdhStackVersionFromVersionString(versionString));
        assertEquals(expectedPatchVersion, CdhVersionProvider.getCdhPatchVersionFromVersionString(versionString));
        assertEquals(expectedBuildNumber, CdhVersionProvider.getCdhBuildNumberFromVersionString(versionString));
    }

}
