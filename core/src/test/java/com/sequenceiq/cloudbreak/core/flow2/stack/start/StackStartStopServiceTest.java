package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;

@ExtendWith(MockitoExtension.class)
class StackStartStopServiceTest {

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private MetadataSetupService metadatSetupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private StackStartStopService underTest;

    static Stream<Arguments> stackScenarios() {
        return Stream.concat(
                Arrays.stream(Status.values()).map(status -> Arguments.of(status.name(), stackWithStatus(status), isStopPossibleByStatus(status))),
                Stream.of(Arguments.of("Null stack", null, false),
                        Arguments.of("Null stack status", stackWithStatus(null), false))
        );
    }

    static Stream<Arguments> stackViewScenarios() {
        return Stream.concat(
                Arrays.stream(Status.values()).map(status -> Arguments.of(status.name(), stackViewWithStatus(status), isStopPossibleByStatus(status))),
                Stream.of(Arguments.of("Null stackview", null, false),
                        Arguments.of("Null stackview status", stackViewWithStatus(null), false))
        );
    }

    private static Stack stackWithStatus(Status status) {
        Stack stack = new Stack();
        StackStatus stackStatus = new StackStatus<>(stack, status, null, null);
        stack.setStackStatus(stackStatus);
        return stack;
    }

    private static StackView stackViewWithStatus(Status status) {
        StackStatusView stackStatusView = new StackStatusView();
        stackStatusView.setStatus(status);
        return new StackView(10L, "stackview", "platform", stackStatusView);
    }

    private static boolean isStopPossibleByStatus(Status status) {
        return status == STOP_REQUESTED ||
                status == STOP_IN_PROGRESS ||
                status == EXTERNAL_DATABASE_STOP_FINISHED;
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stackScenarios")
    void isStopPossibleTest(String testName, Stack stack, boolean expected) {
        boolean result = underTest.isStopPossible(stack);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stackViewScenarios")
    void isStopPossibleTest(String testName, StackView stack, boolean expected) {
        boolean result = underTest.isStopPossible(stack);
        assertThat(result).isEqualTo(expected);
    }
}
