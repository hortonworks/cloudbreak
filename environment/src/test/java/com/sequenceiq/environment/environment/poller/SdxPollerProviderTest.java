package com.sequenceiq.environment.environment.poller;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

class SdxPollerProviderTest {

    private static final Long ENV_ID = 1000L;

    private final SdxService sdxService = Mockito.mock(SdxService.class);

    private final ClusterPollerResultEvaluator clusterPollerResultEvaluator = new ClusterPollerResultEvaluator();

    private SdxPollerProvider underTest = new SdxPollerProvider(sdxService, clusterPollerResultEvaluator);

    @ParameterizedTest
    @MethodSource("datalakeStopStatuses")
    void testStopDatalakePoller(SdxClusterStatusResponse s1Status, SdxClusterStatusResponse s2Status,
            AttemptState attemptState, String message, List<String> remaining) throws Exception {
        List<String> pollingCrn = new ArrayList<>();
        pollingCrn.add("crn1");
        pollingCrn.add("crn2");
        SdxClusterResponse sdx1 = getSdxResponse(s1Status, "crn1");
        SdxClusterResponse sdx2 = getSdxResponse(s2Status, "crn2");

        when(sdxService.getByCrn("crn1")).thenReturn(sdx1);
        when(sdxService.getByCrn("crn2")).thenReturn(sdx2);

        AttemptResult<Void> result = underTest.stopSdxClustersPoller(ENV_ID, pollingCrn).process();

        assertEquals(attemptState, result.getState());
    }

    @ParameterizedTest
    @MethodSource("datalakeStartStatuses")
    void testStartDatalakePoller(SdxClusterStatusResponse s1Status, SdxClusterStatusResponse s2Status, AttemptState attemptState, String message,
            List<String> remaining) throws Exception {
        List<String> pollingCrn = new ArrayList<>();
        pollingCrn.add("crn1");
        pollingCrn.add("crn2");
        SdxClusterResponse sdx1 = getSdxResponse(s1Status, "crn1");
        SdxClusterResponse sdx2 = getSdxResponse(s2Status, "crn2");

        when(sdxService.getByCrn("crn1")).thenReturn(sdx1);
        when(sdxService.getByCrn("crn2")).thenReturn(sdx2);

        AttemptResult<Void> result = underTest.startSdxClustersPoller(ENV_ID, pollingCrn).process();

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
                Arguments.of(SdxClusterStatusResponse.STOPPED, SdxClusterStatusResponse.STOPPED, AttemptState.FINISH, "", emptyList()),
                Arguments.of(SdxClusterStatusResponse.STOP_IN_PROGRESS, SdxClusterStatusResponse.STOPPED, AttemptState.CONTINUE, "", List.of("crn1")),
                Arguments.of(SdxClusterStatusResponse.STOP_FAILED, SdxClusterStatusResponse.STOPPED, AttemptState.BREAK, "SDX stop failed 'crn1', reason",
                        List.of("crn1"))
        );
    }

    private static Stream<Arguments> datalakeStartStatuses() {
        return Stream.of(
                Arguments.of(SdxClusterStatusResponse.RUNNING, SdxClusterStatusResponse.RUNNING, AttemptState.FINISH, "", emptyList()),
                Arguments.of(SdxClusterStatusResponse.START_IN_PROGRESS, SdxClusterStatusResponse.RUNNING, AttemptState.CONTINUE, "", List.of("crn1")),
                Arguments.of(SdxClusterStatusResponse.START_FAILED, SdxClusterStatusResponse.RUNNING, AttemptState.BREAK, "SDX start failed 'crn1', reason",
                        List.of("crn1"))
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
