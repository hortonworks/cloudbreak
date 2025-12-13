package com.sequenceiq.environment.environment.poller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

class SdxPollerProviderTest {

    private static final Long ENV_ID = 1000L;

    private final SdxService sdxService = mock(SdxService.class);

    private final FlowIdentifier flowIdentifier1 = mock(FlowIdentifier.class);

    private final FlowIdentifier flowIdentifier2 = mock(FlowIdentifier.class);

    private final DatalakeMultipleFlowsResultEvaluator multipleFlowsResultEvaluator = mock(DatalakeMultipleFlowsResultEvaluator.class);

    private SdxPollerProvider underTest = new SdxPollerProvider(sdxService, multipleFlowsResultEvaluator);

    @ParameterizedTest
    @MethodSource("datalakeStopStatuses")
    void testStopDatalakePoller(AttemptState attemptState, Boolean allFinished) throws Exception {
        when(multipleFlowsResultEvaluator.allFinished(anyList())).thenReturn(allFinished);

        AttemptResult<Void> result = underTest.flowListPoller(ENV_ID, List.of(flowIdentifier1, flowIdentifier2)).process();

        assertEquals(attemptState, result.getState());
    }

    @ParameterizedTest
    @MethodSource("datalakeStartStatuses")
    void testStartDatalakePoller(AttemptState attemptState, Boolean allFinished) throws Exception {
        when(multipleFlowsResultEvaluator.allFinished(anyList())).thenReturn(allFinished);

        AttemptResult<Void> result = underTest.flowListPoller(ENV_ID, List.of(flowIdentifier1, flowIdentifier2)).process();

        assertEquals(attemptState, result.getState());
    }

    @ParameterizedTest(name = "Operation Status: {0}, Expected attempt state: {1}")
    @MethodSource("upgradeCcmScenarios")
    void upgradeCcmPollerTest(OperationProgressStatus status, AttemptState expectedState) {
        when(sdxService.getOperation("crn", false)).thenReturn(getOperationViewWithStatus(status));
        AttemptResult<Void> result = underTest.upgradeCcmPoller(ENV_ID, "crn");
        assertThat(result.getState()).isEqualTo(expectedState);
    }

    private static Stream<Arguments> datalakeStopStatuses() {
        return Stream.of(
                Arguments.of(AttemptState.FINISH, Boolean.TRUE),
                Arguments.of(AttemptState.CONTINUE, Boolean.FALSE)
        );
    }

    private static Stream<Arguments> datalakeStartStatuses() {
        return Stream.of(
                Arguments.of(AttemptState.FINISH, Boolean.TRUE),
                Arguments.of(AttemptState.CONTINUE, Boolean.FALSE)
        );
    }

    private static Stream<Arguments> upgradeCcmScenarios() {
        return Stream.of(
                Arguments.of(OperationProgressStatus.UNKNOWN, AttemptState.BREAK),
                Arguments.of(OperationProgressStatus.CANCELLED, AttemptState.BREAK),
                Arguments.of(OperationProgressStatus.FAILED, AttemptState.BREAK),
                Arguments.of(OperationProgressStatus.FINISHED, AttemptState.FINISH),
                Arguments.of(OperationProgressStatus.RUNNING, AttemptState.CONTINUE)
        );
    }

    private OperationView getOperationViewWithStatus(OperationProgressStatus status) {
        OperationView operationView = new OperationView();
        operationView.setProgressStatus(status);
        return operationView;
    }

    private SdxClusterResponse getSdxResponse(SdxClusterStatusResponse status, String name) {
        SdxClusterResponse stack1 = new SdxClusterResponse();
        stack1.setStatus(status);
        stack1.setName(name);
        stack1.setCrn(name);
        stack1.setStatusReason("reason");
        return stack1;
    }
}
