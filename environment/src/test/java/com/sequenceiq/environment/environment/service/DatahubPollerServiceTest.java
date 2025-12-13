package com.sequenceiq.environment.environment.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.poller.ClusterPollerResultEvaluator;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.FlowResultPollerEvaluator;
import com.sequenceiq.environment.environment.service.datahub.DatahubPollerService;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.FlowEndpoint;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
class DatahubPollerServiceTest {

    private static final long ENV_ID = 165_343_536L;

    private static final String ENV_CRN = "envCrn";

    private static final String STACK_CRN = "stackCrn";

    private final DatahubService datahubService = mock(DatahubService.class);

    private final FlowEndpoint flowEndpoint = mock(FlowEndpoint.class);

    private final DatahubPollerProvider datahubPollerProvider = new DatahubPollerProvider(datahubService, new ClusterPollerResultEvaluator(), flowEndpoint,
            new FlowResultPollerEvaluator());

    private final DatahubPollerService underTest =
            new DatahubPollerService(datahubService, datahubPollerProvider, new WebApplicationExceptionMessageExtractor());

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "attemptCount", 5);
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
        EnvironmentInMemoryStateStore.delete(ENV_ID);
    }

    @Test
    void testStopAttachedDatahubWhenNoAttachedDatahub() {
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Collections.emptySet()));

        underTest.stopAttachedDatahubClusters(ENV_ID, ENV_CRN);

        verify(datahubService, times(0)).putStopByCrns(eq(ENV_CRN), anyList());
    }

    @Test
    void testStopAttachedDatahubWhenDatahubIsAvailable() {
        StackViewV4Response stackView = getStackView(Status.AVAILABLE);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(stackView)));
        when(datahubService.getByCrn(anyString(), anySet()))
                .thenReturn(getStack(Status.AVAILABLE), getStack(Status.AVAILABLE), getStack(Status.STOPPED));

        underTest.stopAttachedDatahubClusters(ENV_ID, ENV_CRN);

        verify(datahubService, times(1)).putStopByCrns(eq(ENV_CRN), anyList());
    }

    @Test
    void testStartAttachedDatahubWhenNoAttachedDatahub() {
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Collections.emptySet()));

        underTest.startAttachedDatahubClusters(ENV_ID, ENV_CRN);

        verify(datahubService, times(0)).putStartByCrns(eq(ENV_CRN), anyList());
    }

    @Test
    void testStartAttachedDatahubWhenDatahubIsStopped() {
        StackViewV4Response stackView = getStackView(Status.STOPPED);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(stackView)));
        when(datahubService.getByCrn(anyString(), anySet()))
                .thenReturn(getStack(Status.STOPPED), getStack(Status.STOPPED), getStack(Status.AVAILABLE));

        underTest.startAttachedDatahubClusters(ENV_ID, ENV_CRN);

        verify(datahubService, times(1)).putStartByCrns(eq(ENV_CRN), anyList());
    }

    private StackV4Response getStack(Status status) {
        StackV4Response stack = new StackV4Response();
        stack.setStatus(status);
        stack.setCrn(STACK_CRN);
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setStatus(status);
        stack.setCluster(cluster);
        return stack;
    }

    private StackViewV4Response getStackView(Status status) {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setCrn(STACK_CRN);
        stack.setStatus(status);
        return stack;
    }
}
