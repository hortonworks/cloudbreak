package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaStackHealthDetailsServiceTest {

    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ACCOUNT_ID = "accountId";

    private static final String HOST1 = "host1.domain";

    private static final String HOST2 = "host2.domain";

    private static final String INSTANCE_ID1 = "i-0123456789";

    private static final String INSTANCE_ID2 = "i-9876543210";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaSafeInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    @Mock
    private ExecutorService executorService;

    @InjectMocks
    private FreeIpaStackHealthDetailsService underTest;

    private NodeHealthDetails createNodeHealthDetails(String instanceId, String name,
            InstanceStatus status, List<String> issues) {
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId(instanceId);
        nodeHealthDetails.setName(name);
        nodeHealthDetails.setStatus(status);
        if (issues != null && !issues.isEmpty()) {
            nodeHealthDetails.setIssues(issues);
        }
        return nodeHealthDetails;
    }

    private NodeHealthDetails getGoodDetails1() {
        return createNodeHealthDetails(INSTANCE_ID1, HOST1, InstanceStatus.CREATED, null);
    }

    private NodeHealthDetails getGoodDetails2() {
        return createNodeHealthDetails(INSTANCE_ID2, HOST2, InstanceStatus.CREATED, null);
    }

    private NodeHealthDetails getUnhealthyDetails1() {
        return createNodeHealthDetails(INSTANCE_ID1, HOST1, InstanceStatus.UNHEALTHY, List.of("failed"));
    }

    private NodeHealthDetails getUnhealthyDetails2() {
        return createNodeHealthDetails(INSTANCE_ID2, HOST2, InstanceStatus.UNHEALTHY, List.of("failed"));
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID1);
        instanceMetaData.setDiscoveryFQDN(HOST1);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        return stack;
    }

    private Stack getStackTwoInstances(InstanceMetaData im1, InstanceMetaData im2) {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceGroup.setInstanceMetaData(Set.of(im1, im2));
        return stack;
    }

    private Stack getDeletedStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaData.setInstanceId(INSTANCE_ID1);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN(HOST1);
        return stack;
    }

    private InstanceMetaData getInstance1() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID1);
        instanceMetaData.setDiscoveryFQDN(HOST1);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    private InstanceMetaData getInstance2() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID2);
        instanceMetaData.setDiscoveryFQDN(HOST2);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        lenient().when(freeIpaInstanceHealthDetailsService.createNodeResponseWithStatusAndIssue(any(), any(), anyString())).thenCallRealMethod();
        // Setup the default behavior for executorService.invokeAll
        setupMockExecutorService();
    }

    private void setupMockExecutorService() throws InterruptedException {
        Future<NodeHealthDetails> mockFuture = mock(Future.class);
        List<Future<NodeHealthDetails>> mockFutures = List.of(mockFuture);
        lenient().doReturn(mockFutures).when(executorService).invokeAll(any());
    }

    private void setupMockExecutorServiceWithTwoFutures(NodeHealthDetails details1, NodeHealthDetails details2) throws Exception {
        Future<NodeHealthDetails> mockFuture1 = mock(Future.class);
        Future<NodeHealthDetails> mockFuture2 = mock(Future.class);
        when(mockFuture1.get(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(details1);
        when(mockFuture2.get(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(details2);
        List<Future<NodeHealthDetails>> mockFutures = List.of(mockFuture1, mockFuture2);
        doReturn(mockFutures).when(executorService).invokeAll(any());
    }

    private void setupMockExecutorServiceWithSingleFuture(NodeHealthDetails details) throws Exception {
        Future<NodeHealthDetails> mockFuture = mock(Future.class);
        when(mockFuture.get(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(details);
        List<Future<NodeHealthDetails>> mockFutures = List.of(mockFuture);
        doReturn(mockFutures).when(executorService).invokeAll(any());
    }

    private void verifyBasicMockInteractions() {
        verify(stackService).getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID, ACCOUNT_ID);
    }

    private void verifyExecutorServiceInvocation(int expectedTaskCount) throws InterruptedException {
        ArgumentCaptor<List<Callable<NodeHealthDetails>>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(executorService).invokeAll(taskCaptor.capture());

        List<Callable<NodeHealthDetails>> capturedTasks = taskCaptor.getValue();
        assertEquals(expectedTaskCount, capturedTasks.size());
    }

    private void verifyNoFurtherInteractions() {
        verifyNoMoreInteractions(stackService, executorService);
    }

    @Test
    void testNodeDeletedOnProvider() throws Exception {
        Stack deletedStack = getDeletedStack();
        deletedStack.getAllInstanceMetaDataList().forEach(im -> im.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(deletedStack);
        setupMockExecutorServiceWithSingleFuture(createNodeHealthDetails(INSTANCE_ID1, HOST1, InstanceStatus.DELETED_ON_PROVIDER_SIDE, List.of()));

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.DELETED_ON_PROVIDER_SIDE, response.getStatus());
        assertFalse(response.getNodeHealthDetails().isEmpty());
        assertSame(InstanceStatus.DELETED_ON_PROVIDER_SIDE, response.getNodeHealthDetails().stream().findFirst().get().getStatus());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(1);
        verifyNoFurtherInteractions();
    }

    @Test
    void testHealthySingleNode() throws Exception {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        setupMockExecutorServiceWithSingleFuture(getGoodDetails1());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.AVAILABLE, response.getStatus());
        assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            assertTrue(nodeHealth.getIssues().isEmpty());
            assertEquals(InstanceStatus.CREATED, nodeHealth.getStatus());
        }

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(1);
        verifyNoFurtherInteractions();
    }

    @Test
    void testUnhealthySingleNode() throws Exception {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        setupMockExecutorServiceWithSingleFuture(getUnhealthyDetails1());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            assertFalse(nodeHealth.getIssues().isEmpty());
            assertEquals(InstanceStatus.UNHEALTHY, nodeHealth.getStatus());
        }

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(1);
        verifyNoFurtherInteractions();
    }

    @Test
    void testTwoGoodNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(getGoodDetails1(), getGoodDetails2());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.AVAILABLE, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testOneGoodOneUnhealthyNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(getGoodDetails1(), getUnhealthyDetails2());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testTwoUnhealthyNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(getUnhealthyDetails1(), getUnhealthyDetails2());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testOneStoppedOneGoodNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData im2 = getInstance2();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(getGoodDetails1(), getUnhealthyDetails2());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testTwoStoppedNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.STOPPED);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(
                createNodeHealthDetails(INSTANCE_ID1, HOST1, InstanceStatus.STOPPED, List.of()),
                createNodeHealthDetails(INSTANCE_ID2, HOST2, InstanceStatus.STOPPED, List.of()));

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.STOPPED, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testTwoFailedNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.FAILED);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.FAILED);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(
                createNodeHealthDetails(INSTANCE_ID1, HOST1, InstanceStatus.FAILED, List.of()),
                createNodeHealthDetails(INSTANCE_ID2, HOST2, InstanceStatus.FAILED, List.of()));

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testAllInstancesDeletedOnProviderSide() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        setupMockExecutorServiceWithTwoFutures(
                createNodeHealthDetails(INSTANCE_ID1, HOST1, InstanceStatus.DELETED_ON_PROVIDER_SIDE, List.of()),
                createNodeHealthDetails(INSTANCE_ID2, HOST2, InstanceStatus.DELETED_ON_PROVIDER_SIDE, List.of()));

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.DELETED_ON_PROVIDER_SIDE, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
        response.getNodeHealthDetails().forEach(node ->
                assertEquals(InstanceStatus.DELETED_ON_PROVIDER_SIDE, node.getStatus()));

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verifyNoFurtherInteractions();
    }

    @Test
    void testInterruptedExceptionDuringHealthCheck() throws InterruptedException {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        doThrow(new InterruptedException("Interrupted during health check")).when(executorService).invokeAll(any());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(Status.UNHEALTHY, response.getStatus());

        verifyBasicMockInteractions();
        verify(executorService).invokeAll(any());
        verifyNoMoreInteractions(stackService, executorService);

        // Clear interrupt status for subsequent tests
        Thread.interrupted();
    }

    @Test
    void testExecutionExceptionDuringHealthCheck() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));

        Future<NodeHealthDetails> futureMock1 = mock(Future.class);
        Future<NodeHealthDetails> futureMock2 = mock(Future.class);
        when(futureMock1.get(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(getGoodDetails1());
        when(futureMock2.get(eq(15L), eq(TimeUnit.SECONDS))).thenThrow(new ExecutionException("Exception during future execution", new RuntimeException()));
        List<Future<NodeHealthDetails>> futures = List.of(futureMock1, futureMock2);
        doReturn(futures).when(executorService).invokeAll(any());

        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);

        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
        assertTrue(response.getNodeHealthDetails().stream().anyMatch(node -> node.getStatus() == InstanceStatus.CREATED));
        assertTrue(response.getNodeHealthDetails().stream().anyMatch(node -> node.getStatus() == InstanceStatus.UNREACHABLE));

        verifyBasicMockInteractions();
        verifyExecutorServiceInvocation(2);
        verify(futureMock1).get(eq(15L), eq(TimeUnit.SECONDS));
        verify(futureMock2).get(eq(15L), eq(TimeUnit.SECONDS));
        verifyNoMoreInteractions(stackService, executorService, futureMock1, futureMock2);
    }
}