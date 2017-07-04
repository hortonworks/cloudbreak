package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.repository.CloudbreakNodeRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.ha.FlowDistributor;
import com.sequenceiq.cloudbreak.service.ha.HeartbeatService;

@RunWith(MockitoJUnitRunner.class)
public class HeartbeatServiceTest {

    private static final String MY_ID = "E80C7BD9-61CD-442E-AFDA-C3B30FEDE88F";

    private static final String NODE_1_ID = "5575B7AD-45CB-487D-BE14-E33C913F9394";

    private static final String NODE_2_ID = "854506AC-A0D5-4C98-A47C-70F6251FC604";

    @InjectMocks
    private HeartbeatService heartbeatService;

    @Mock
    private CloudbreakNodeRepository cloudbreakNodeRepository;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private Clock clock;

    @Mock
    private FlowDistributor flowDistributor;

    @Mock
    private FlowRegister runningFlows;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<List<FlowLog>> flowLogListCaptor;

    @Before
    public void init() {
        ReflectionTestUtils.setField(heartbeatService, "uuid", MY_ID);
    }

    @Test
    public void testOneNodeTakesAllFlows() {
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            CloudbreakNode node = clusterNodes.get(i);
            node.setLastUpdated(50_000);
        }
        when(cloudbreakNodeRepository.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTime()).thenReturn(200_000L);

        // all flows that need to be re-distributed
        List<String> suspendedFlows = new ArrayList<>();

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        suspendedFlows.addAll(node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogRepository.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogRepository.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogRepository.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

        heartbeatService.scheduledFlowDistribution();

        verify(flowLogRepository).save(flowLogListCaptor.capture());
        List<FlowLog> updatedFlows = flowLogListCaptor.getValue();
        assertEquals(myNewFlowLogs.size(), updatedFlows.size());
        for (FlowLog updatedFlow : updatedFlows) {
            assertEquals(MY_ID, updatedFlow.getCloudbreakNodeId());
        }

        verify(flow2Handler, times(5)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(5, allFlowIds.size());
        for (String flowId : suspendedFlows) {
            assertTrue(allFlowIds.contains(flowId));
        }
    }

    @Test
    public void testDistributionConcurrency() {
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000); // myself
        clusterNodes.get(1).setLastUpdated(50_000); // failed node
        clusterNodes.get(2).setLastUpdated(200_000); // active node

        when(cloudbreakNodeRepository.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTime()).thenReturn(200_000L);

        // all flows that need to be re-distributed
        List<String> suspendedFlows = new ArrayList<>();

        List<FlowLog> node1FlowLogs = getFlowLogs(3, 5000);
        suspendedFlows.addAll(node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogRepository.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(2)));
        distribution.computeIfAbsent(clusterNodes.get(2), v -> new ArrayList<>()).
                addAll(Collections.singletonList(suspendedFlows.get(1)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(0))).collect(Collectors.toList()));
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(2))).collect(Collectors.toList()));
        when(flowLogRepository.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

        when(flowLogRepository.save(anyCollection())).thenThrow(new OptimisticLockingFailureException("Someone already distributed the flows.."));

        heartbeatService.scheduledFlowDistribution();

        verify(flow2Handler, times(2)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(2, allFlowIds.size());
        for (FlowLog flowLog : myNewFlowLogs) {
            assertTrue(allFlowIds.contains(flowLog.getFlowId()));
        }
    }

    @Test
    public void testDistributionConcurrencyWithDifferentFlows() {
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000); // myself
        clusterNodes.get(1).setLastUpdated(50_000); // failed node
        clusterNodes.get(2).setLastUpdated(200_000); // active node

        when(cloudbreakNodeRepository.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTime()).thenReturn(200_000L);

        // all flows that need to be re-distributed
        List<String> suspendedFlows = new ArrayList<>();

        List<FlowLog> node1FlowLogs = getFlowLogs(3, 5000);
        suspendedFlows.addAll(node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogRepository.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(2)));
        distribution.computeIfAbsent(clusterNodes.get(2), v -> new ArrayList<>()).
                addAll(Collections.singletonList(suspendedFlows.get(1)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(0))).collect(Collectors.toList()));
        when(flowLogRepository.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

        when(flowLogRepository.save(anyCollection())).thenThrow(new OptimisticLockingFailureException("Someone already distributed the flows.."));

        heartbeatService.scheduledFlowDistribution();

        verify(flow2Handler, times(1)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(1, allFlowIds.size());
        for (FlowLog flowLog : myNewFlowLogs) {
            assertTrue(allFlowIds.contains(flowLog.getFlowId()));
        }
    }

    private List<CloudbreakNode> getClusterNodes() {
        List<CloudbreakNode> nodes = new ArrayList<>();
        nodes.add(new CloudbreakNode(MY_ID));
        nodes.add(new CloudbreakNode(NODE_1_ID));
        nodes.add(new CloudbreakNode(NODE_2_ID));
        return nodes;
    }

    private List<FlowLog> getFlowLogs(int flowCount, int from) {
        List<FlowLog> flows = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        int flowId = random.nextInt(5000) + from;
        long stackId = random.nextLong();
        for (int i = 0; i < flowCount; i++) {
            for (int j = 0; j < random.nextInt(99) + 1; j++) {
                flows.add(new FlowLog(stackId + i, "" + flowId + i, "RUNNING", false));
            }
        }
        return flows;
    }
}
