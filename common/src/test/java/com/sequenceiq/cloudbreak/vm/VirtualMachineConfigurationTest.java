package com.sequenceiq.cloudbreak.vm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VirtualMachineConfigurationTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" is parsed to {2} for default versions, \"{1}\" parsed to {3} for runtime")
    @MethodSource("supportedJavaVersionsArguments")
    void supportedJavaVersions(String defaultVersions, Map<String, String> versionsByRuntime, Set<Integer> expectedDefault, Set<Integer> expectedRuntime) {
        VirtualMachineConfiguration configuration = new VirtualMachineConfiguration();
        configuration.setSupportedJavaVersions(defaultVersions);
        configuration.setSupportedJavaVersionsByRuntime(versionsByRuntime);
        configuration.init();
        Set<Integer> resultDefault = configuration.getSupportedJavaVersions();
        assertThat(resultDefault).isEqualTo(expectedDefault);
        Set<Integer> resultRuntime = configuration.getSupportedJavaVersionsByRuntime().get("7.3.2");
        assertThat(resultRuntime).isEqualTo(expectedRuntime);
    }

    static Stream<Arguments> supportedJavaVersionsArguments() {
        return Stream.of(
                Arguments.of("", Map.of("7.3.2", ""), Set.of(), Set.of()),
                Arguments.of(" ", Map.of("7.3.2", " "), Set.of(), Set.of()),
                Arguments.of("8", Map.of("7.3.2", "11"), Set.of(8), Set.of(11)),
                Arguments.of("8 ", Map.of("7.3.2", "11 "), Set.of(8), Set.of(11)),
                Arguments.of("8,11", Map.of("7.3.2", "11,17"), Set.of(8, 11), Set.of(11, 17)),
                Arguments.of("8,11,", Map.of("7.3.2", "11,17,"), Set.of(8, 11), Set.of(11, 17))
        );
    }

}
