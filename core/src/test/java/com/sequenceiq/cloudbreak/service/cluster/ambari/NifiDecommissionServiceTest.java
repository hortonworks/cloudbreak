package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.service.PollingResult.FAILURE;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.NotSupportedException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.model.HostComponentStatuses;
import com.sequenceiq.ambari.client.model.HostStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;

@RunWith(MockitoJUnitRunner.class)
public class NifiDecommissionServiceTest {

    private static final String NIFI_MASTER = "NIFI_MASTER";

    private static final List<String> NIFI_COMPONENTS = List.of("NIFI_CA", NIFI_MASTER, "NIFI_REGISTRY_MASTER");

    private static final String NIFI = "NIFI";

    private static final int COMMAND_ID = 1;

    private static final String HOST_1 = "host1";

    private static final String HOST_2 = "host2";

    @InjectMocks
    private NifiDecommissionService underTest;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private Stack stack;

    @Test
    public void testRetireNifiNodesShouldCallRetireCommandWhenNifiIsPresent() throws URISyntaxException, IOException {
        List<String> hostList = List.of(HOST_1, HOST_2);
        Map<String, HostStatus> hostStatusMap = Map.of(HOST_1, createHostStatus(NIFI_MASTER), HOST_2, createHostStatus(NIFI));
        Pair<PollingResult, Exception> retireResult = Pair.of(SUCCESS, null);

        when(ambariClient.retire(anyList(), eq(NIFI), eq(NIFI_MASTER))).thenReturn(COMMAND_ID);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE))).thenReturn(retireResult);

        underTest.retireNifiNodes(stack, ambariClient, hostList, hostStatusMap, true);

        verify(ambariClient).retire(anyList(), eq(NIFI), eq(NIFI_MASTER));
        verify(ambariOperationService).waitForOperations(eq(stack), eq(ambariClient), any(), eq(RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE));
    }

    @Test
    public void testRetireNifiNodesShouldNotThrowExceptionWhenTheRetireCommandFailed() throws URISyntaxException, IOException {
        List<String> hostList = List.of(HOST_1, HOST_2);
        Map<String, HostStatus> hostStatusMap = Map.of(HOST_1, createHostStatus(NIFI_MASTER), HOST_2, createHostStatus(NIFI));

        when(ambariClient.retire(anyList(), eq(NIFI), eq(NIFI_MASTER))).thenThrow(new NotSupportedException());

        underTest.retireNifiNodes(stack, ambariClient, hostList, hostStatusMap, true);

        verify(ambariClient).retire(anyList(), eq(NIFI), eq(NIFI_MASTER));
        verifyZeroInteractions(ambariOperationService);
    }

    @Test(expected = DecommissionException.class)
    public void testRetireNifiNodesShouldThrowExceptionWhenThePollerFailedWithError() throws URISyntaxException, IOException {
        List<String> hostList = List.of(HOST_1, HOST_2);
        Map<String, HostStatus> hostStatusMap = Map.of(HOST_1, createHostStatus(NIFI_MASTER), HOST_2, createHostStatus(NIFI));
        Pair<PollingResult, Exception> retireResult = Pair.of(FAILURE, new TimeoutException());

        when(ambariClient.retire(anyList(), eq(NIFI), eq(NIFI_MASTER))).thenReturn(COMMAND_ID);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE))).thenReturn(retireResult);

        underTest.retireNifiNodes(stack, ambariClient, hostList, hostStatusMap, true);

        verify(ambariClient).retire(anyList(), eq(NIFI), eq(NIFI_MASTER));
        verify(ambariOperationService).waitForOperations(eq(stack), eq(ambariClient), any(), eq(RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE));
    }

    @Test
    public void testSetAutoRestartForNifiShouldCallAmbariToSetNewValue() throws URISyntaxException, IOException {
        underTest.setAutoRestartForNifi(ambariClient, true, true);
        verify(ambariClient).setAutoRestart(NIFI_COMPONENTS, true);
    }

    @Test(expected = DecommissionException.class)
    public void testSetAutoRestartForNifiShouldThrowExceptionWhenTheApiCallFailed() throws URISyntaxException, IOException {
        when(ambariClient.setAutoRestart(NIFI_COMPONENTS, true)).thenThrow(new NotSupportedException());
        underTest.setAutoRestartForNifi(ambariClient, true, true);
        verify(ambariClient).setAutoRestart(NIFI_COMPONENTS, true);
    }

    @Test
    public void testIsNifiPresentInTheClusterShouldReturnTrueWhenNifiIsPresent() {
        Map<String, HostStatus> hostStatusMap = Map.of(HOST_1, createHostStatus(NIFI_MASTER), HOST_2, createHostStatus("SPARK"));
        assertTrue(underTest.isNifiPresentInTheCluster(hostStatusMap));
    }

    @Test
    public void testIsNifiPresentInTheClusterShouldReturnFalseWhenNifiIsNotPresent() {
        Map<String, HostStatus> hostStatusMap = Map.of(HOST_1, createHostStatus("FLINK"), HOST_2, createHostStatus("SPARK"));
        assertFalse(underTest.isNifiPresentInTheCluster(hostStatusMap));
    }

    private HostStatus createHostStatus(String componentName) {
        HostStatus hostStatus = new HostStatus();
        hostStatus.setHostComponentsStatuses(Collections.singletonMap(componentName, new HostComponentStatuses()));
        return hostStatus;
    }

}