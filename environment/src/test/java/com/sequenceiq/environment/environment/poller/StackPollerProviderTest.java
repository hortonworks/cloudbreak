package com.sequenceiq.environment.environment.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

public class StackPollerProviderTest {

    private final StackService stackService = mock(StackService.class);

    private final FlowLogDBService flowLogDBService = mock(FlowLogDBService.class);

    private final StackPollerProvider underTest = new StackPollerProvider(stackService, flowLogDBService);

    @ParameterizedTest
    @MethodSource("stackUpdateConfigStates")
    public void testStackUpdateConfigPoller(Exception crn1Exception, Exception crn2Exception,
            String flowState, AttemptState expectedResult) throws Exception {
        List<String> stackCrns = new ArrayList<>();
        stackCrns.add("crn1");
        stackCrns.add("crn2");
        if (crn1Exception != null) {
            doThrow(crn1Exception).when(stackService).triggerConfigUpdateForStack("crn1");
        }
        if (crn2Exception != null) {
            doThrow(crn2Exception).when(stackService).triggerConfigUpdateForStack("crn2");
        }
        FlowLogWithoutPayload flowLog = getFlowLog(flowState);
        when(flowLogDBService.getLastFlowLog("1")).thenReturn(Optional.of(flowLog));
        AttemptResult<Void> result = underTest.stackUpdateConfigPoller(stackCrns, 1L, "1").process();
        assertEquals(expectedResult, result.getState());
    }

    private static Stream<Arguments> stackUpdateConfigStates() {
        return Stream.of(
                Arguments.of(null, null, FlowConstants.CANCELLED_STATE, AttemptState.FINISH),
                Arguments
                        .of(null, null, EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_START_STATE.name(),
                                AttemptState.FINISH),
                Arguments.of(new BadRequestException("flow running"), null,
                        EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_START_STATE.name(),
                        AttemptState.CONTINUE),
                Arguments.of(new BadRequestException("flow running"),
                        new BadRequestException("flow running"),
                        EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_START_STATE.name(),
                        AttemptState.CONTINUE),
                Arguments.of(new WebApplicationException("some 500 error"), null,
                        EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_START_STATE.name(),
                        AttemptState.BREAK)
        );
    }

    private FlowLogWithoutPayload getFlowLog(String state) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        when(flowLog.getCurrentState()).thenReturn(state);
        return flowLog;
    }
}
