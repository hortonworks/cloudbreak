package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

class MinionUtilTest {

    private final Node node = new Node("10.0.0.1", null, null, "hg");

    private final List<String> gatewayPrivateIps = List.of("172.16.252.43");

    private final MinionUtil underTest = new MinionUtil();

    @ParameterizedTest(name = "{index} Create minion with restartNeededFlagSupported={0} and restartNeeded={1}")
    @MethodSource("createMinionSource")
    void createMinionTest(boolean restartNeededFlagSupported, boolean restartNeeded, List<String> expectedServers, boolean expectedRestartNeededFlag) {
        Minion minion = underTest.createMinion(node, gatewayPrivateIps, restartNeededFlagSupported, restartNeeded);

        Assertions.assertThat(minion)
                .returns(expectedServers, Minion::getServers)
                .returns(expectedRestartNeededFlag, Minion::isRestartNeeded);
    }

    private static Stream<Arguments> createMinionSource() {
        return Stream.of(
                Arguments.of(false, false, List.of("172.16.252.43"), false),
                Arguments.of(true, true, List.of("172.16.252.43"), true),
                Arguments.of(true, false, List.of("172.16.252.43"), false),
                Arguments.of(false, true, List.of("127.0.0.1"), true));
    }

}
