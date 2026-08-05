package com.sequenceiq.environment.environment.poller;

import static com.sequenceiq.flow.api.model.FlowType.FLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

public class StackPollerProviderTest {

    private final StackService stackService = mock(StackService.class);

    private final FlowLogDBService flowLogDBService = mock(FlowLogDBService.class);

    private final FlowResultPollerEvaluator flowResultPollerEvaluator = mock(FlowResultPollerEvaluator.class);

    private final StackPollerProvider underTest = new StackPollerProvider(stackService, flowLogDBService, flowResultPollerEvaluator);

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

    @ParameterizedTest
    @MethodSource("updateSslConfigsSource")
    public void testUpdateSslConfig(boolean hasActiveFlow, boolean latestFlowFinalizedAndFailed, AttemptState expectedResult) {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FLOW, "123");
        FlowCheckResponse flowCheckResponse = mock(FlowCheckResponse.class);

        when(flowCheckResponse.getHasActiveFlow()).thenReturn(hasActiveFlow);
        when(flowCheckResponse.getLatestFlowFinalizedAndFailed()).thenReturn(latestFlowFinalizedAndFailed);
        when(stackService.checkFlow(flowIdentifier)).thenReturn(flowCheckResponse);

        AttemptResult<Void> result = underTest.updateSslConfig(1L, flowIdentifier);

        assertEquals(expectedResult, result.getState());
    }

    private static Stream<Arguments> updateSslConfigsSource() {
        return Stream.of(
                Arguments.of(false, false, AttemptState.FINISH),
                Arguments.of(true, false, AttemptState.CONTINUE),
                Arguments.of(false, true, AttemptState.BREAK)
        );
    }

    @ParameterizedTest
    @MethodSource("userDefinedTagsUpdateStates")
    public void testUserDefinedTagsUpdatePoller(Exception crn1Exception, Exception crn2Exception, AttemptState expectedResult) throws Exception {
        List<String> stackCrns = new ArrayList<>();
        stackCrns.add("crn1");
        stackCrns.add("crn2");
        Map<String, String> tags = Map.of("key", "value");

        FlowIdentifier flow1 = new FlowIdentifier(FLOW, "flow-crn1");
        FlowIdentifier flow2 = new FlowIdentifier(FLOW, "flow-crn2");

        if (crn1Exception != null) {
            doThrow(crn1Exception).when(stackService).triggerUserDefinedTagsUpdate("crn1", tags);
        } else {
            when(stackService.triggerUserDefinedTagsUpdate("crn1", tags)).thenReturn(flow1);
        }
        if (crn2Exception != null) {
            doThrow(crn2Exception).when(stackService).triggerUserDefinedTagsUpdate("crn2", tags);
        } else {
            when(stackService.triggerUserDefinedTagsUpdate("crn2", tags)).thenReturn(flow2);
        }

        stubFlowResultPollerEvaluator();

        AttemptResult<List<FlowIdentifier>> result = underTest.userDefinedTagsUpdatePoller(stackCrns, 1L, tags).process();

        assertEquals(expectedResult, result.getState());
    }

    private static Stream<Arguments> userDefinedTagsUpdateStates() {
        return Stream.of(
                Arguments.of(null, null, AttemptState.FINISH),
                Arguments.of(new BadRequestException("flow running"), null, AttemptState.CONTINUE),
                Arguments.of(new BadRequestException("flow running"), new BadRequestException("flow running"), AttemptState.CONTINUE),
                Arguments.of(new WebApplicationException("some 500 error"), null, AttemptState.BREAK)
        );
    }

    @SuppressWarnings("unchecked")
    private void stubFlowResultPollerEvaluator() {
        when(flowResultPollerEvaluator.attemptResultFinisher(any())).thenAnswer(invocation -> {
            List<AttemptResult<FlowIdentifier>> results = invocation.getArgument(0);
            Optional<AttemptResult<FlowIdentifier>> error = results.stream()
                    .filter(r -> r.getState() == AttemptState.BREAK)
                    .findFirst();
            if (error.isPresent()) {
                return AttemptResults.breakFor(error.get().getCause());
            }
            boolean anyContinuing = results.stream().anyMatch(r -> r.getState() == AttemptState.CONTINUE);
            if (anyContinuing) {
                return AttemptResults.justContinue();
            }
            return AttemptResults.finishWith(results.stream().map(AttemptResult::getResult).collect(Collectors.toList()));
        });
    }

    @Test
    public void testUpdateUserDefinedTagsFinishesWhenAllFlowsFinished() throws Exception {
        Long envId = 10L;
        FlowIdentifier flow1 = new FlowIdentifier(FLOW, "flow-1");
        FlowIdentifier flow2 = new FlowIdentifier(FLOW, "flow-2");

        stubFlowCheck(flow1, false, false);
        stubFlowCheck(flow2, false, false);

        AttemptResult<Void> result = underTest.updateUserDefinedTags(envId, List.of(flow1, flow2)).process();

        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    public void testUpdateUserDefinedTagsContinuesWhenSomeFlowsStillActive() throws Exception {
        Long envId = 11L;
        FlowIdentifier flow1 = new FlowIdentifier(FLOW, "flow-1");
        FlowIdentifier flow2 = new FlowIdentifier(FLOW, "flow-2");

        stubFlowCheck(flow1, true, false);
        stubFlowCheck(flow2, false, false);

        AttemptMaker<Void> attemptMaker = underTest.updateUserDefinedTags(envId, List.of(flow1, flow2));

        AttemptResult<Void> firstResult = attemptMaker.process();
        assertEquals(AttemptState.CONTINUE, firstResult.getState());

        stubFlowCheck(flow1, false, false);
        AttemptResult<Void> secondResult = attemptMaker.process();
        assertEquals(AttemptState.FINISH, secondResult.getState());
        verify(stackService, times(1)).checkFlow(flow2);
        verify(stackService, times(2)).checkFlow(flow1);
    }

    @Test
    public void testUpdateUserDefinedTagsBreaksWhenAnyFlowFailed() throws Exception {
        Long envId = 12L;
        FlowIdentifier flow1 = new FlowIdentifier(FLOW, "flow-1");
        FlowIdentifier flow2 = new FlowIdentifier(FLOW, "flow-2");

        stubFlowCheck(flow1, false, false);
        stubFlowCheck(flow2, false, true);

        AttemptResult<Void> result = underTest.updateUserDefinedTags(envId, List.of(flow1, flow2)).process();

        assertEquals(AttemptState.BREAK, result.getState());
    }

    @Test
    public void testUpdateUserDefinedTagsBreaksEarlyWithoutCheckingRemainingFlows() throws Exception {
        Long envId = 13L;
        FlowIdentifier flow1 = new FlowIdentifier(FLOW, "flow-1");
        FlowIdentifier flow2 = new FlowIdentifier(FLOW, "flow-2");

        stubFlowCheck(flow1, false, true);

        AttemptResult<Void> result = underTest.updateUserDefinedTags(envId, List.of(flow1, flow2)).process();

        assertEquals(AttemptState.BREAK, result.getState());
        verify(stackService, times(0)).checkFlow(flow2);
    }

    private void stubFlowCheck(FlowIdentifier flowIdentifier, boolean hasActiveFlow, boolean latestFlowFinalizedAndFailed) {
        FlowCheckResponse flowCheckResponse = mock(FlowCheckResponse.class);
        when(flowCheckResponse.getHasActiveFlow()).thenReturn(hasActiveFlow);
        when(flowCheckResponse.getLatestFlowFinalizedAndFailed()).thenReturn(latestFlowFinalizedAndFailed);
        when(stackService.checkFlow(flowIdentifier)).thenReturn(flowCheckResponse);
    }

    private FlowLogWithoutPayload getFlowLog(String state) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        when(flowLog.getCurrentState()).thenReturn(state);
        return flowLog;
    }
}
