package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ComponentVersionComparatorTest {

    private final ComponentVersionComparator underTest = new ComponentVersionComparator();

    @ParameterizedTest(name = "[{index}] Upgrade from {0}.{1}-{2} to {3}.{4}-{5} should be {6}")
    @MethodSource("provideTestParameters")
    public void testPermitCmAndStackUpgradeByComponentVersion(String currentVersion, Integer currentPatchVersion, Integer currentBuildNumber,
            String candidateVersion, Integer candidatePatchVersion, Integer candidateBuildNumber, boolean expectedResult) {
        VersionComparisonContext currentVersionContext = new VersionComparisonContext.Builder()
                .withMajorVersion(currentVersion)
                .withPatchVersion(currentPatchVersion)
                .withBuildNumber(currentBuildNumber)
                .build();
        VersionComparisonContext candidateVersionContext = new VersionComparisonContext.Builder()
                .withMajorVersion(candidateVersion)
                .withPatchVersion(candidatePatchVersion)
                .withBuildNumber(candidateBuildNumber)
                .build();
        assertEquals(expectedResult, underTest.permitCmAndStackUpgradeByComponentVersion(currentVersionContext, candidateVersionContext));
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of("7.2.0", 100, 1, "7.3.0", 200, 2, true),
                Arguments.of("7.2.9", 100, 1, "7.3.0", 200, 2, true),
                Arguments.of("7.2.9", 100, 1, "7.9.0", 200, 2, true),
                Arguments.of("7.2.0", 100, 1, "7.2.0", 100, 1, true),
                Arguments.of("7.3.0", 200, 2, "7.2.0", 100, 1, false),
                Arguments.of("7.9.0", 200, 2, "7.2.9", 100, 1, false),
                Arguments.of("7.2.2", 100, 1, "7.2.0", 100, 1, false),
                Arguments.of("7.2.0", 100, 1, "7.2.0", 101, 1, true),
                Arguments.of("7.2.0", 100, 2, "7.2.0", 101, 1, true),
                Arguments.of("7.2.0", 100, 1, "7.2.0", 100, 2, true),
                Arguments.of("7.2.0", 100, 1, "7.2.1", 100, 1, true),
                Arguments.of("7.2.0", 100, 1, "7.2.0", 101, 2, true),
                Arguments.of("7.2.0", 100, 1, "7.2.1", 99, 1, true),
                Arguments.of("7.2.0", 100, 1, "7.2.1", 100, 0, true),
                Arguments.of("7.2.0", 100, 1, "7.2.0", 99, 2, false),
                Arguments.of("7.2.0", null, 1, "7.3.0", null, 2, true),
                Arguments.of("7.2.0", null, 1, "7.2.0", null, 1, true),
                Arguments.of("7.2.0", null, 1, "7.2.0", null, 2, true),
                Arguments.of("7.2.0", null, 1, "7.2.1", null, 1, true),
                Arguments.of("7.2.0", 100, 1, "7.2.0", null, 1, false),
                Arguments.of("7.2.0", null, 1, "7.2.0", 100, 1, true)
        );
    }

}