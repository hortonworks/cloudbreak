package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.domain.Node;
import com.sequenceiq.cloudbreak.ha.service.FlowDistributor;
import com.sequenceiq.cloudbreak.ha.service.NodeService;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.cloudbreak.service.ha.HeartbeatService;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.cleanup.InMemoryCleanup;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldFlowConfig;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.FlowCancelService;

@RunWith(MockitoJUnitRunner.class)
public class HeartbeatServiceTest {

    private static final String MY_ID = "E80C7BD9-61CD-442E-AFDA-C3B30FEDE88F";

    private static final String NODE_1_ID = "5575B7AD-45CB-487D-BE14-E33C913F9394";

    private static final String NODE_2_ID = "854506AC-A0D5-4C98-A47C-70F6251FC604";

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(2018, 1, 1, 0, 0);

    @InjectMocks
    private HeartbeatService heartbeatService;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private NodeService nodeService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private Clock clock;

    @Mock
    private FlowDistributor flowDistributor;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private TransactionService transactionService;

    @Mock
    private HaApplication haApplication;

    @Mock
    private ApplicationFlowInformation applicationFlowInformation;

    @Mock
    private InMemoryCleanup inMemoryCleanup;

    @Mock
    private FlowCancelService flowCancelService;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<List<FlowLog>> flowLogListCaptor;

    @Before
    public void init() throws TransactionExecutionException {
        when(nodeConfig.isNodeIdSpecified()).thenReturn(true);
        when(nodeConfig.getId()).thenReturn(MY_ID);
        ReflectionTestUtils.setField(heartbeatService, "heartbeatThresholdRate", 70000);
        doAnswer(invocation -> {
            try {
                return ((Supplier<?>) invocation.getArgument(0)).get();
            } catch (RuntimeException e) {
                throw new TransactionExecutionException("", e);
            }
        }).when(transactionService).required(any(Supplier.class));
    }

    @Test
    public void testOneNodeTakesAllFlows() {
        List<Node> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            Node node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<Node, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.getRunningFlowIdsSnapshot()).thenReturn(Set.of());

        heartbeatService.scheduledFlowDistribution();

        verify(flowLogService).saveAll(flowLogListCaptor.capture());
        List<FlowLog> updatedFlows = flowLogListCaptor.getValue();
        assertEquals(myNewFlowLogs.size(), updatedFlows.size());
        for (FlowLog updatedFlow : updatedFlows) {
            assertEquals(MY_ID, updatedFlow.getCloudbreakNodeId());
        }

        verify(flow2Handler, times(5)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(5L, allFlowIds.size());
        for (String flowId : suspendedFlows) {
            assertTrue(allFlowIds.contains(flowId));
        }
    }

    @Test
    public void testOneNodeTakesAllFlowsWithCleanup() {
        List<Node> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            Node node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs)).thenReturn(Collections.emptySet());

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs).thenReturn(Collections.emptySet());

        Map<Node, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.getRunningFlowIdsSnapshot()).thenReturn(Set.of());

        heartbeatService.scheduledFlowDistribution();

        verify(flowLogService).saveAll(flowLogListCaptor.capture());
        List<FlowLog> updatedFlows = flowLogListCaptor.getValue();
        assertEquals(myNewFlowLogs.size(), updatedFlows.size());
        for (FlowLog updatedFlow : updatedFlows) {
            assertEquals(MY_ID, updatedFlow.getCloudbreakNodeId());
        }

        verify(flow2Handler, times(5)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(5L, allFlowIds.size());
        for (String flowId : suspendedFlows) {
            assertTrue(allFlowIds.contains(flowId));
        }
    }

    @Test
    public void testOneNodeTakesAllFlowsWithInvalidFlows() {
        List<Node> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            Node node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<Node, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.getRunningFlowIdsSnapshot()).thenReturn(Set.of());

        List<Long> stackIds = myNewFlowLogs.stream().map(FlowLog::getResourceId).distinct().collect(Collectors.toList());
        when(haApplication.getDeletingResources(anySet())).thenReturn(Set.of(stackIds.get(0), stackIds.get(2)));
        doReturn(Collections.singletonList(HelloWorldFlowConfig.class)).when(applicationFlowInformation).getTerminationFlow();
        List<FlowLog> invalidFlowLogs = myNewFlowLogs.stream()
                .filter(fl -> fl.getResourceId().equals(stackIds.get(0)) || fl.getResourceId().equals(stackIds.get(2))).collect(Collectors.toList());

        heartbeatService.scheduledFlowDistribution();

        verify(flowLogService).saveAll(flowLogListCaptor.capture());
        List<FlowLog> updatedFlows = flowLogListCaptor.getValue();
        assertEquals(myNewFlowLogs.size(), updatedFlows.size());
        for (FlowLog updatedFlow : updatedFlows) {
            if (invalidFlowLogs.contains(updatedFlow)) {
                assertEquals(StateStatus.SUCCESSFUL, updatedFlow.getStateStatus());
                assertNull(updatedFlow.getCloudbreakNodeId());
            } else {
                assertEquals(MY_ID, updatedFlow.getCloudbreakNodeId());
            }
        }

        verify(flow2Handler, times(5)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(5L, allFlowIds.size());
        for (String flowId : suspendedFlows) {
            assertTrue(allFlowIds.contains(flowId));
        }
    }

    @Test
    public void testOneNodeTakesAllFlowsWithTerminationFlowShouldBeDistributed() {
        List<Node> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            Node node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        node1FlowLogs.forEach(fl -> fl.setFlowType(ClassValue.of(HelloWorldFlowConfig.class)));
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        node2FlowLogs.forEach(fl -> fl.setFlowType(ClassValue.of(HelloWorldFlowConfig.class)));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<Node, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.getRunningFlowIdsSnapshot()).thenReturn(Set.of());

        List<Long> stackIds = myNewFlowLogs.stream().map(FlowLog::getResourceId).distinct().collect(Collectors.toList());
        when(haApplication.getDeletingResources(anySet())).thenReturn(Set.of(stackIds.get(0), stackIds.get(2)));
        doReturn(Collections.singletonList(HelloWorldFlowConfig.class)).when(applicationFlowInformation).getTerminationFlow();

        heartbeatService.scheduledFlowDistribution();

        verify(flowLogService).saveAll(flowLogListCaptor.capture());
        List<FlowLog> updatedFlows = flowLogListCaptor.getValue();
        assertEquals(myNewFlowLogs.size(), updatedFlows.size());
        for (FlowLog updatedFlow : updatedFlows) {
            assertEquals(MY_ID, updatedFlow.getCloudbreakNodeId());
        }

        verify(flow2Handler, times(5)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(5L, allFlowIds.size());
        for (String flowId : suspendedFlows) {
            assertTrue(allFlowIds.contains(flowId));
        }
    }

    @Test
    public void testDistributionConcurrency() {
        List<Node> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        clusterNodes.get(1).setLastUpdated(50_000L); // failed node
        clusterNodes.get(2).setLastUpdated(200_000L); // active node

        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(3, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Map<Node, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(2)));
        distribution.computeIfAbsent(clusterNodes.get(2), v -> new ArrayList<>()).
                addAll(Collections.singletonList(suspendedFlows.get(1)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(0))).collect(Collectors.toList()));
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(2))).collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.getRunningFlowIdsSnapshot()).thenReturn(Set.of());

        when(flowLogService.saveAll(anyCollection())).thenThrow(new OptimisticLockingFailureException("Someone already distributed the flows.."));

        heartbeatService.scheduledFlowDistribution();

        verify(flow2Handler, times(2)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(2L, allFlowIds.size());
        for (FlowLog flowLog : myNewFlowLogs) {
            assertTrue(allFlowIds.contains(flowLog.getFlowId()));
        }
    }

    @Test
    public void testDistributionConcurrencyWithDifferentFlows() {
        List<Node> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        clusterNodes.get(1).setLastUpdated(50_000L); // failed node
        clusterNodes.get(2).setLastUpdated(200_000L); // active node

        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(3, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Map<Node, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(2)));
        distribution.computeIfAbsent(clusterNodes.get(2), v -> new ArrayList<>()).
                addAll(Collections.singletonList(suspendedFlows.get(1)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        List<FlowLog> myNewFlowLogs = node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(0))).collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(new HashSet<>(myNewFlowLogs));

        when(runningFlows.getRunningFlowIdsSnapshot()).thenReturn(Set.of());

        when(flowLogService.saveAll(anyCollection())).thenThrow(new OptimisticLockingFailureException("Someone already distributed the flows.."));

        heartbeatService.scheduledFlowDistribution();

        verify(flow2Handler, times(1)).restartFlow(stringCaptor.capture());
        List<String> allFlowIds = stringCaptor.getAllValues();
        assertEquals(1L, allFlowIds.size());
        for (FlowLog flowLog : myNewFlowLogs) {
            assertTrue(allFlowIds.contains(flowLog.getFlowId()));
        }
    }

    @Test
    public void testHeartbeatWhenEverytingWorks() {
        class TestRetry implements Retry {

            @Override
            public void testWith2SecDelayMax5Times(Runnable action) throws ActionFailedException {
            }

            @Override
            public <T> T testWith2SecDelayMax5Times(Supplier<T> action) {
                return (T) Boolean.TRUE;
            }

            @Override
            public <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax5Times(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax5TimesWithCheckRetriable(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax3Times(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWithoutRetry(Supplier<T> action) throws ActionFailedException {
                return null;
            }
        }

        Set<FlowLog> flowLogs = new HashSet<>(getFlowLogs(2, 5000));

        ReflectionTestUtils.setField(heartbeatService, "retryService", new TestRetry());

        // Mock InMemoryStateStore for check method execution success
        Set<Long> myStackIds = flowLogs.stream().map(FlowLog::getResourceId).collect(Collectors.toSet());
        for (Long myStackId : myStackIds) {
            InMemoryStateStore.putStack(myStackId, PollGroup.POLLABLE);
        }

        heartbeatService.heartbeat();

        // In case of exception the instance should terminate the flows which are in running state
        for (Long myStackId : myStackIds) {
            assertEquals(PollGroup.POLLABLE, InMemoryStateStore.getStack(myStackId));
        }
        // There was no action on InMemoryStateStore
        verify(flowLogService, times(0)).findAllByCloudbreakNodeId(anyString());
    }

    @Test
    public void testHeartbeatWhenInstanceCanNotReachTheDatabase() {
        class TestRetryWithFail implements Retry {

            @Override
            public void testWith2SecDelayMax5Times(Runnable action) throws ActionFailedException {
            }

            @Override
            public <T> T testWith2SecDelayMax5Times(Supplier<T> action) {
                throw new ActionFailedException("Test failed");
            }

            @Override
            public <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax5Times(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax5TimesWithCheckRetriable(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWith1SecDelayMax3Times(Supplier<T> action) throws ActionFailedException {
                return null;
            }

            @Override
            public <T> T testWithoutRetry(Supplier<T> action) throws ActionFailedException {
                return null;
            }
        }

        // When the cloudbreak instance can not reach the db
        ReflectionTestUtils.setField(heartbeatService, "retryService", new TestRetryWithFail());

        heartbeatService.heartbeat();

        verify(inMemoryCleanup, times(1)).cancelEveryFlowWithoutDbUpdate();
    }

    private List<Node> getClusterNodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(MY_ID));
        nodes.add(new Node(NODE_1_ID));
        nodes.add(new Node(NODE_2_ID));

        IntStream.range(0, 3).forEach(i -> nodes.get(i).setLastUpdated(BASE_DATE_TIME.plusMinutes(i).toEpochSecond(ZoneOffset.UTC)));
        return nodes;
    }

    private List<FlowLog> getFlowLogs(int flowCount, int from) {
        List<FlowLog> flows = new ArrayList<>();
        Random random = new SecureRandom();
        int flowId = random.nextInt(5000) + from;
        long stackId = random.nextInt(5000) + from;
        for (int i = 0; i < flowCount; i++) {
            for (int j = 0; j < random.nextInt(99) + 1; j++) {
                FlowLog flowLog = new FlowLog(stackId + i, "" + flowId + i, "RUNNING",
                        false, StateStatus.PENDING, OperationType.UNKNOWN);
                flowLog.setFlowType(ClassValue.of(FlowConfiguration.class));
                flows.add(flowLog);
            }
        }
        return flows;
    }

    @Test
    public void testDistributeFlows() throws TransactionExecutionException {
        ReflectionTestUtils.setField(heartbeatService, "heartbeatThresholdRate", 70);

        List<Node> clusterNodes = getClusterNodes();
        when(nodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(BASE_DATE_TIME.plusMinutes(clusterNodes.size()).toEpochSecond(ZoneOffset.UTC));

        Set<FlowLog> failedFLowLogs1 = new HashSet<>(getFlowLogs(2, 5000));
        Set<FlowLog> failedFlowLogs2 = new HashSet<>(getFlowLogs(2, 5000));
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(failedFLowLogs1);
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(failedFlowLogs2);

        Map<Node, List<String>> flowDistribution = createFlowDistribution(failedFLowLogs1, failedFlowLogs2);
        when(flowDistributor.distribute(anyList(), eq(clusterNodes.subList(2, clusterNodes.size())))).thenReturn(flowDistribution);

        Set<Long> deletedResources = failedFLowLogs1.stream()
                .map(FlowLog::getResourceId)
                .collect(Collectors.toSet());
        when(haApplication.getDeletingResources(anySet())).thenReturn(deletedResources);
        doReturn(Collections.singletonList(HelloWorldFlowConfig.class)).when(applicationFlowInformation).getTerminationFlow();

        List<Node> nodes = heartbeatService.distributeFlows();

        ArgumentCaptor<Collection<FlowLog>> updatedLogsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(flowLogService, times(1)).saveAll(updatedLogsCaptor.capture());
        Collection<FlowLog> updatedFlowLogs = updatedLogsCaptor.getValue();
        assertTrue(updatedFlowLogs.stream()
                .map(FlowLog::getStateStatus)
                .allMatch(StateStatus.SUCCESSFUL::equals));

        assertEquals(clusterNodes.subList(0, 2), nodes);
    }

    private Map<Node, List<String>> createFlowDistribution(Set<FlowLog> failedFLowLogs1, Set<FlowLog> failedFlowLogs2) {
        Map<Node, List<String>> distribution = new HashMap<>();

        distribution.put(new Node(UUID.randomUUID().toString()), failedFLowLogs1.stream().map(FlowLog::getFlowId).collect(Collectors.toList()));
        return distribution;
    }
}
