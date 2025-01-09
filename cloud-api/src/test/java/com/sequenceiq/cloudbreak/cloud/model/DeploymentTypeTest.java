package com.sequenceiq.cloudbreak.cloud.model;

import static com.sequenceiq.cloudbreak.cloud.model.DeploymentType.CANARY_TEST_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.cloud.model.DeploymentType.PROVISION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeploymentTypeTest {

    public static Stream<Arguments> provideTestCombinations() {
        return Stream.of(
                Arguments.of("PROVISION", PROVISION),
                Arguments.of("CANARY_TEST_DEPLOYMENT", CANARY_TEST_DEPLOYMENT),
                Arguments.of("  CANARY_TEST_DEPLOYMENT  ", CANARY_TEST_DEPLOYMENT),
                Arguments.of("", PROVISION),
                Arguments.of("INVALID_TYPE", PROVISION),
                Arguments.of(null, PROVISION));
    }

    @ParameterizedTest
    @MethodSource("provideTestCombinations")
    public void testProvisionInput(String input, DeploymentType expected) {
        DeploymentType result = DeploymentType.safeValueOf(input);
        assertEquals(expected, result);
    }
}