package com.sequenceiq.cloudbreak.vm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VirtualMachineConfigurationTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" is parsed to {1}")
    @MethodSource("supportedJavaVersionsArguments")
    void supportedJavaVersions(String property, Set<Integer> expected) {
        Set<Integer> result = new VirtualMachineConfiguration(property).getSupportedJavaVersions();
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> supportedJavaVersionsArguments() {
        return Stream.of(
                Arguments.of("", Set.of()),
                Arguments.of(" ", Set.of()),
                Arguments.of("8", Set.of(8)),
                Arguments.of("8 ", Set.of(8)),
                Arguments.of("8,11", Set.of(8, 11)),
                Arguments.of("8,11,", Set.of(8, 11))
        );
    }

}
