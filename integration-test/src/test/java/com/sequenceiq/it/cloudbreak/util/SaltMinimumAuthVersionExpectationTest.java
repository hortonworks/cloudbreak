package com.sequenceiq.it.cloudbreak.util;

import static com.sequenceiq.it.cloudbreak.util.SaltMinimumAuthVersionExpectation.expectedMinimumAuthVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SaltMinimumAuthVersionExpectationTest {

    private static final List<String> GATEWAY_WITH_MINIMUM_AUTH_VERSION_OPTION = List.of("3006.25");

    static Stream<Arguments> minionVersionsAndExpectedAuthProtocol() {
        return Stream.of(
                Arguments.of("3000.8", 0),
                Arguments.of("3001.8", 0),
                Arguments.of("3004.2", 0),
                Arguments.of("3005", 2),
                Arguments.of("3005.1", 2),
                Arguments.of("3006.10", 2),
                Arguments.of("3006.11", 2),
                Arguments.of("3006.12", 3),
                Arguments.of("3006.16", 3),
                Arguments.of("3006.25", 3),
                Arguments.of("3007", 2),
                Arguments.of("3007.1", 2),
                Arguments.of("3007.3", 2),
                Arguments.of("3007.4", 3),
                Arguments.of("3007.8", 3),
                Arguments.of("3007.9", 3),
                Arguments.of("3008.0", 3),
                Arguments.of("", 0),
                Arguments.of(null, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("minionVersionsAndExpectedAuthProtocol")
    void minimumAuthVersionIsTheAuthProtocolOfTheMinion(String minionVersion, int expectedAuthProtocol) {
        Optional<Integer> result = expectedMinimumAuthVersion(GATEWAY_WITH_MINIMUM_AUTH_VERSION_OPTION, Arrays.asList("3006.25", minionVersion));

        assertEquals(Optional.of(expectedAuthProtocol), result);
    }

    @Test
    void oldestMinionDecides() {
        Optional<Integer> result = expectedMinimumAuthVersion(GATEWAY_WITH_MINIMUM_AUTH_VERSION_OPTION,
                List.of("3006.25", "3006.10", "3001.8"));

        assertEquals(Optional.of(0), result);
    }

    @ParameterizedTest
    @MethodSource("gatewayVersionsWithoutMinimumAuthVersionOption")
    void noMinimumAuthVersionWhenNoGatewayKnowsTheOption(List<String> gatewayVersions) {
        Optional<Integer> result = expectedMinimumAuthVersion(gatewayVersions, List.of("3006.10"));

        assertEquals(Optional.empty(), result);
    }

    static Stream<List<String>> gatewayVersionsWithoutMinimumAuthVersionOption() {
        return Stream.of(
                List.of(),
                List.of("3006.10"),
                List.of("3006.16", "3001.8"),
                List.of("3007"),
                List.of("3007.3"),
                List.of("3007.4"),
                List.of("3007.8", "3006.16"),
                Arrays.asList("", null, "3006.10")
        );
    }

    @ParameterizedTest
    @MethodSource("gatewayVersionsWithMinimumAuthVersionOption")
    void minimumAuthVersionIsExpectedWhenAnyGatewayKnowsTheOption(List<String> gatewayVersions) {
        Optional<Integer> result = expectedMinimumAuthVersion(gatewayVersions, List.of("3006.10"));

        assertEquals(Optional.of(2), result);
    }

    static Stream<List<String>> gatewayVersionsWithMinimumAuthVersionOption() {
        return Stream.of(
                List.of("3006.17"),
                List.of("3006.25"),
                List.of("3007.9"),
                List.of("3008.0"),
                List.of("3006.16", "3007.9"),
                Arrays.asList("3006.10", null, "3006.17")
        );
    }
}
