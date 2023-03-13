package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

import software.amazon.awssdk.services.efs.model.LifeCycleState;

class LifeCycleStateToResourceStatusConverterTest {

    private final LifeCycleStateToResourceStatusConverter underTest = new LifeCycleStateToResourceStatusConverter();

    private static Stream<Arguments> statusParameters() {
        return Stream.of(
                Arguments.of(LifeCycleState.AVAILABLE, ResourceStatus.CREATED),
                Arguments.of(LifeCycleState.DELETED, ResourceStatus.DELETED),
                Arguments.of(LifeCycleState.ERROR, ResourceStatus.FAILED),
                Arguments.of(LifeCycleState.CREATING, ResourceStatus.IN_PROGRESS),
                Arguments.of(LifeCycleState.UPDATING, ResourceStatus.IN_PROGRESS),
                Arguments.of(LifeCycleState.DELETING, ResourceStatus.IN_PROGRESS)
        );
    }

    @ParameterizedTest
    @MethodSource("statusParameters")
    void testConvertShouldReturnTheCorrectValue(LifeCycleState sourceState, ResourceStatus expectedStatus) {
        assertEquals(expectedStatus, underTest.convert(sourceState));
    }

}