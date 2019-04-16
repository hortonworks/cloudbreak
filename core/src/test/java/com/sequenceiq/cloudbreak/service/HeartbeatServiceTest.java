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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.ha.FlowDistributor;
import com.sequenceiq.cloudbreak.service.ha.HeartbeatService;
import com.sequenceiq.cloudbreak.service.node.CloudbreakNodeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class HeartbeatServiceTest {

    private static final String MY_ID = "E80C7BD9-61CD-442E-AFDA-C3B30FEDE88F";

    private static final String NODE_1_ID = "5575B7AD-45CB-487D-BE14-E33C913F9394";

    private static final String NODE_2_ID = "854506AC-A0D5-4C98-A47C-70F6251FC604";

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(2018, 1, 1, 0, 0);

    @InjectMocks
    private HeartbeatService heartbeatService;

    @Mock
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Mock
    private CloudbreakNodeService cloudbreakNodeService;

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
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<List<FlowLog>> flowLogListCaptor;

    @Before
    public void init() throws TransactionExecutionException {
        when(cloudbreakNodeConfig.isNodeIdSpecified()).thenReturn(true);
        when(cloudbreakNodeConfig.getId()).thenReturn(MY_ID);
        ReflectionTestUtils.setField(heartbeatService, "heartbeatThresholdRate", 70000);
        doAnswer(invocation -> {
            try {
                return ((Supplier<?>) invocation.getArgument(0)).get();
            } catch (RuntimeException e) {
                throw new TransactionExecutionException("", e);
            }
        }).when(transactionService).required(any());
    }

    @Test
    public void testOneNodeTakesAllFlows() {
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            CloudbreakNode node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

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
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            CloudbreakNode node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs)).thenReturn(Collections.emptySet());

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs).thenReturn(Collections.emptySet());

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

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
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            CloudbreakNode node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

        List<Long> stackIds = myNewFlowLogs.stream().map(FlowLog::getStackId).distinct().collect(Collectors.toList());
        List<Object[]> statusResponse = new ArrayList<>();
        statusResponse.add(new Object[]{stackIds.get(0), Status.DELETE_IN_PROGRESS});
        statusResponse.add(new Object[]{stackIds.get(2), Status.DELETE_IN_PROGRESS});
        when(stackService.getStatuses(any())).thenReturn(statusResponse);

        List<FlowLog> invalidFlowLogs = myNewFlowLogs.stream()
                .filter(fl -> fl.getStackId().equals(stackIds.get(0)) || fl.getStackId().equals(stackIds.get(2))).collect(Collectors.toList());

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
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        // set all nodes to failed except myself
        for (int i = 1; i < clusterNodes.size(); i++) {
            CloudbreakNode node = clusterNodes.get(i);
            node.setLastUpdated(50_000L);
        }
        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(2, 5000);
        node1FlowLogs.forEach(fl -> fl.setFlowType(StackTerminationFlowConfig.class));
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Set<FlowLog> node2FlowLogs = new HashSet<>(getFlowLogs(3, 3000));
        node2FlowLogs.forEach(fl -> fl.setFlowType(StackTerminationFlowConfig.class));
        suspendedFlows.addAll(node2FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(NODE_2_ID)).thenReturn(node2FlowLogs);

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(1), suspendedFlows.get(2),
                        suspendedFlows.get(3), suspendedFlows.get(4)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs);
        myNewFlowLogs.addAll(node2FlowLogs);
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

        List<Long> stackIds = myNewFlowLogs.stream().map(FlowLog::getStackId).distinct().collect(Collectors.toList());
        List<Object[]> statusResponse = new ArrayList<>();
        statusResponse.add(new Object[]{stackIds.get(0), Status.DELETE_IN_PROGRESS});
        statusResponse.add(new Object[]{stackIds.get(2), Status.DELETE_IN_PROGRESS});
        when(stackService.getStatuses(any())).thenReturn(statusResponse);

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
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        clusterNodes.get(1).setLastUpdated(50_000L); // failed node
        clusterNodes.get(2).setLastUpdated(200_000L); // active node

        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(3, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(2)));
        distribution.computeIfAbsent(clusterNodes.get(2), v -> new ArrayList<>()).
                addAll(Collections.singletonList(suspendedFlows.get(1)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        Set<FlowLog> myNewFlowLogs = new HashSet<>();
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(0))).collect(Collectors.toList()));
        myNewFlowLogs.addAll(node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(2))).collect(Collectors.toList()));
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(myNewFlowLogs);

        when(runningFlows.get(any())).thenReturn(null);

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
        List<CloudbreakNode> clusterNodes = getClusterNodes();
        clusterNodes.get(0).setLastUpdated(200_000L); // myself
        clusterNodes.get(1).setLastUpdated(50_000L); // failed node
        clusterNodes.get(2).setLastUpdated(200_000L); // active node

        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(200_000L);

        // all flows that need to be re-distributed

        List<FlowLog> node1FlowLogs = getFlowLogs(3, 5000);
        List<String> suspendedFlows = node1FlowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(new HashSet<>(node1FlowLogs));

        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();
        distribution.computeIfAbsent(clusterNodes.get(0), v -> new ArrayList<>()).
                addAll(Arrays.asList(suspendedFlows.get(0), suspendedFlows.get(2)));
        distribution.computeIfAbsent(clusterNodes.get(2), v -> new ArrayList<>()).
                addAll(Collections.singletonList(suspendedFlows.get(1)));
        when(flowDistributor.distribute(any(), any())).thenReturn(distribution);

        List<FlowLog> myNewFlowLogs = node1FlowLogs.stream().filter(fl -> fl.getFlowId().equalsIgnoreCase(suspendedFlows.get(0))).collect(Collectors.toList());
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(new HashSet<>(myNewFlowLogs));

        when(runningFlows.get(any())).thenReturn(null);

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
            public Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) {
                return Boolean.TRUE;
            }

            @Override
            public <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionWentFailException {
                return null;
            }
        }

        Set<FlowLog> flowLogs = new HashSet<>(getFlowLogs(2, 5000));

        ReflectionTestUtils.setField(heartbeatService, "retryService", new TestRetry());

        // Mock InMemoryStateStore for check method execution success
        Set<Long> myStackIds = flowLogs.stream().map(FlowLog::getStackId).collect(Collectors.toSet());
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
            public Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) {
                throw new ActionWentFailException("Test failed");
            }

            @Override
            public <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionWentFailException {
                return null;
            }
        }

        Set<FlowLog> flowLogs = new HashSet<>(getFlowLogs(2, 5000));

        // When the cloudbreak instance can not reach the db
        ReflectionTestUtils.setField(heartbeatService, "retryService", new TestRetryWithFail());

        // Mock InMemoryStateStore for check method execution success
        Set<Long> myStackIds = flowLogs.stream().map(FlowLog::getStackId).collect(Collectors.toSet());
        for (Long myStackId : myStackIds) {
            InMemoryStateStore.putStack(myStackId, PollGroup.POLLABLE);
        }

        heartbeatService.heartbeat();

        // In case of exception the instance should terminate the flows which are in running state
        for (Long myStackId : myStackIds) {
            assertEquals(PollGroup.CANCELLED, InMemoryStateStore.getStack(myStackId));
        }
    }

    private List<CloudbreakNode> getClusterNodes() {
        List<CloudbreakNode> nodes = new ArrayList<>();
        nodes.add(new CloudbreakNode(MY_ID));
        nodes.add(new CloudbreakNode(NODE_1_ID));
        nodes.add(new CloudbreakNode(NODE_2_ID));

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
                FlowLog flowLog = new FlowLog(stackId + i, "" + flowId + i, "RUNNING", false, StateStatus.PENDING);
                flowLog.setFlowType(StackCreationFlowConfig.class);
                flows.add(flowLog);
            }
        }
        return flows;
    }

    @Test
    public void testDistributeFlows() throws TransactionExecutionException {
        ReflectionTestUtils.setField(heartbeatService, "heartbeatThresholdRate", 70);

        List<CloudbreakNode> clusterNodes = getClusterNodes();
        when(cloudbreakNodeService.findAll()).thenReturn(clusterNodes);
        when(clock.getCurrentTimeMillis()).thenReturn(BASE_DATE_TIME.plusMinutes(clusterNodes.size()).toEpochSecond(ZoneOffset.UTC));

        Set<FlowLog> failedFLowLogs1 = new HashSet<>(getFlowLogs(2, 5000));
        Set<FlowLog> failedFlowLogs2 = new HashSet<>(getFlowLogs(2, 5000));
        when(flowLogService.findAllByCloudbreakNodeId(MY_ID)).thenReturn(failedFLowLogs1);
        when(flowLogService.findAllByCloudbreakNodeId(NODE_1_ID)).thenReturn(failedFlowLogs2);

        Map<CloudbreakNode, List<String>> flowDistribution = createFlowDistribution(failedFLowLogs1, failedFlowLogs2);
        when(flowDistributor.distribute(anyList(), eq(clusterNodes.subList(2, clusterNodes.size())))).thenReturn(flowDistribution);

        List<Object[]> stackStatuses = failedFLowLogs1.stream()
                .map(FlowLog::getStackId)
                .map(stackId -> new Object[]{stackId, Status.DELETE_IN_PROGRESS})
                .collect(Collectors.toList());
        when(stackService.getStatuses(anySet())).thenReturn(stackStatuses);

        List<CloudbreakNode> cloudbreakNodes = heartbeatService.distributeFlows();

        ArgumentCaptor<Collection<FlowLog>> updatedLogsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(flowLogService, times(1)).saveAll(updatedLogsCaptor.capture());
        Collection<FlowLog> updatedFlowLogs = updatedLogsCaptor.getValue();
        assertTrue(updatedFlowLogs.stream()
                .map(FlowLog::getStateStatus)
                .allMatch(StateStatus.SUCCESSFUL::equals));

        assertEquals(clusterNodes.subList(0, 2), cloudbreakNodes);
    }

    private Map<CloudbreakNode, List<String>> createFlowDistribution(Set<FlowLog> failedFLowLogs1, Set<FlowLog> failedFlowLogs2) {
        Map<CloudbreakNode, List<String>> distribution = new HashMap<>();

        distribution.put(new CloudbreakNode(UUID.randomUUID().toString()), failedFLowLogs1.stream().map(FlowLog::getFlowId).collect(Collectors.toList()));
        return distribution;
    }
}
