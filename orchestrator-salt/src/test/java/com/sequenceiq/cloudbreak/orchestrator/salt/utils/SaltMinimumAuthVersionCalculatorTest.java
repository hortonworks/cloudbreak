package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SaltMinimumAuthVersionCalculatorTest {

    private final SaltMinimumAuthVersionCalculator underTest = new SaltMinimumAuthVersionCalculator();

    static Stream<Arguments> minionVersions() {
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
    @MethodSource("minionVersions")
    void minimumAuthVersionAdmitsTheMinion(String minionVersion, int expectedMinimumAuthVersion) {
        Optional<Integer> result = underTest.calculate(List.of("3006.25"), Arrays.asList("3006.25", minionVersion));

        assertThat(result).hasValue(expectedMinimumAuthVersion);
    }

    @Test
    void oldestMinionDecides() {
        Optional<Integer> result = underTest.calculate(List.of("3006.25"), List.of("3006.25", "3006.10", "3001.8"));

        assertThat(result).hasValue(0);
    }

    @Test
    void noMinionMeansEnforcement() {
        Optional<Integer> result = underTest.calculate(List.of("3006.25"), List.of());

        assertThat(result).hasValue(3);
    }

    @ParameterizedTest
    @MethodSource("mastersWithoutMinimumAuthVersionOption")
    void unmanagedWhenNoMasterKnowsTheOption(List<String> masterVersions) {
        Optional<Integer> result = underTest.calculate(masterVersions, List.of("3006.10"));

        assertThat(result).isEmpty();
    }

    static Stream<List<String>> mastersWithoutMinimumAuthVersionOption() {
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
    @MethodSource("mastersWithMinimumAuthVersionOption")
    void managedWhenAnyMasterKnowsTheOption(List<String> masterVersions) {
        Optional<Integer> result = underTest.calculate(masterVersions, List.of("3006.10"));

        assertThat(result).hasValue(2);
    }

    static Stream<List<String>> mastersWithMinimumAuthVersionOption() {
        return Stream.of(
                Arrays.asList("3006.10", null, "3006.17"),
                List.of("3006.25"),
                List.of("3006.17", "3007.8"),
                List.of("3007.9"),
                List.of("3008.0")
        );
    }
}
