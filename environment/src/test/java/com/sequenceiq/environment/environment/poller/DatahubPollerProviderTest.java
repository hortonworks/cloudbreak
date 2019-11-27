package com.sequenceiq.environment.environment.poller;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

class DatahubPollerProviderTest {

    private static final Long ENV_ID = 1000L;

    private final DistroXV1Endpoint distroXV1Endpoint = Mockito.mock(DistroXV1Endpoint.class);

    private final ClusterPollerResultEvaluator clusterPollerResultEvaluator = new ClusterPollerResultEvaluator();

    private final DatahubPollerProvider underTest = new DatahubPollerProvider(distroXV1Endpoint, clusterPollerResultEvaluator);

    @Test
    void testStopPollerWhenPollerCrnIsEmpty() throws Exception {
        AttemptResult<Void> result = underTest.stopDatahubClustersPoller(new ArrayList<>(), ENV_ID).process();

        assertEquals(AttemptState.FINISH, result.getState());
    }

    @ParameterizedTest
    @MethodSource("distroxStopStatuses")
    void testStopDistroXPoller(Status s1Status, Status c1Status, Status s2Status, Status c2Status, AttemptState attemptState, String message,
            List<String> remaining) throws Exception {
        List<String> pollingCrn = new ArrayList<>();
        pollingCrn.add("crn1");
        pollingCrn.add("crn2");
        StackV4Response stack1 = getStackV4Response(s1Status, c1Status, "crn1");
        StackV4Response stack2 = getStackV4Response(s2Status, c2Status, "crn2");

        Mockito.when(distroXV1Endpoint.getByCrn("crn1", Collections.emptySet())).thenReturn(stack1);
        Mockito.when(distroXV1Endpoint.getByCrn("crn2", Collections.emptySet())).thenReturn(stack2);

        AttemptResult<Void> result = underTest.stopDatahubClustersPoller(pollingCrn, ENV_ID).process();

        assertEquals(attemptState, result.getState());
    }

    @ParameterizedTest
    @MethodSource("distroxStartStatuses")
    void testStartDistroXPoller(Status s1Status, Status c1Status,
            Status s2Status, Status c2Status, AttemptState attemptState, String message, List<String> remaining) throws Exception {
        List<String> pollingCrn = new ArrayList<>();
        pollingCrn.add("crn1");
        pollingCrn.add("crn2");
        StackV4Response stack1 = getStackV4Response(s1Status, c1Status, "crn1");
        StackV4Response stack2 = getStackV4Response(s2Status, c2Status, "crn2");

        Mockito.when(distroXV1Endpoint.getByCrn("crn1", Collections.emptySet())).thenReturn(stack1);
        Mockito.when(distroXV1Endpoint.getByCrn("crn2", Collections.emptySet())).thenReturn(stack2);

        AttemptResult<Void> result = underTest.startDatahubClustersPoller(pollingCrn, ENV_ID).process();

        assertEquals(attemptState, result.getState());
    }

    private static Stream<Arguments> distroxStopStatuses() {
        return Stream.of(
                Arguments.of(STOPPED, STOPPED, STOPPED, STOPPED, AttemptState.FINISH, "", Collections.emptyList()),
                Arguments.of(STOPPED, STOP_IN_PROGRESS, STOPPED, STOPPED, AttemptState.CONTINUE, "", List.of("crn1")),
                Arguments.of(STOP_IN_PROGRESS, STOPPED, STOPPED, STOPPED, AttemptState.CONTINUE, "", List.of("crn1")),
                Arguments.of(STOPPED, STOP_FAILED, STOPPED, STOPPED, AttemptState.BREAK, "Datahub cluster stop failed 'crn1', cluster reason",
                        List.of("crn1")),
                Arguments.of(STOP_FAILED, STOPPED, STOPPED, STOPPED, AttemptState.BREAK, "Datahub stack stop failed 'crn1', reason", List.of("crn1"))
        );
    }

    private static Stream<Arguments> distroxStartStatuses() {
        return Stream.of(
                Arguments.of(AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AttemptState.FINISH, "", emptyList()),
                Arguments.of(AVAILABLE, UPDATE_IN_PROGRESS, AVAILABLE, AVAILABLE, AttemptState.CONTINUE, "", List.of("crn1")),
                Arguments.of(UPDATE_IN_PROGRESS, AVAILABLE, AVAILABLE, AVAILABLE, AttemptState.CONTINUE, "", List.of("crn1")),
                Arguments.of(AVAILABLE, START_FAILED, AVAILABLE, AVAILABLE, AttemptState.BREAK, "Datahub cluster start failed 'crn1', cluster reason",
                        List.of("crn1")),
                Arguments.of(START_FAILED, AVAILABLE, AVAILABLE, AVAILABLE, AttemptState.BREAK, "Datahub stack start failed 'crn1', reason", List.of("crn1"))
        );
    }

    private StackV4Response getStackV4Response(Status status, Status clusterStatus, String crn) {
        StackV4Response stack1 = new StackV4Response();
        stack1.setStatus(status);
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setStatus(clusterStatus);
        cluster.setName(crn);
        cluster.setStatusReason("cluster reason");
        stack1.setCluster(cluster);
        stack1.setName(crn);
        stack1.setCrn(crn);
        stack1.setStatusReason("reason");
        return stack1;
    }
}
