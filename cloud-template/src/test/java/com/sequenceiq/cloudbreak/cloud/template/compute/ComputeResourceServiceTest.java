package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ComputeResourceServiceTest {

    private static final Variant AWS_VARIANT = Variant.variant("AWS");

    private static final int MAX_POLLING_ATTEMPT = 100;

    @InjectMocks
    private ComputeResourceService underTest;

    @Mock
    private ResourceBuilders resourceBuilders;

    @Mock
    private ResourceActionFactory resourceActionFactory;

    @Mock
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Mock
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Mock
    private SyncPollingScheduler<List<CloudVmInstanceStatus>> syncVMPollingScheduler;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private ComputeResourceBuilder<ResourceBuilderContext> computeResourceBuilder1;

    @Mock
    private ComputeResourceBuilder<ResourceBuilderContext> computeResourceBuilder2;

    @Mock
    private CloudFailureHandler cloudFailureHandler;

    @Spy
    private CloudInstanceBatchSplitter cloudInstanceBatchSplitter;

    @Captor
    private ArgumentCaptor<ResourceDeletionCallablePayload> deletionCallableCaptor;

    @BeforeEach
    public void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getVariant()).thenReturn(AWS_VARIANT);
    }

    @Test
    void startInstancesTest() throws Exception {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilders.getStopStartBatchSize(any())).thenReturn(10);
        ComputeResourceBuilder computeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(computeResourceBuilder.isInstanceBuilder()).thenReturn(true);
        when(resourceBuilders.compute(any())).thenReturn(List.of(computeResourceBuilder));
        ResourceStopStartCallable resourceStopStartCallable = mock(ResourceStopStartCallable.class);
        when(resourceActionFactory.buildStopStartCallable(any())).thenReturn(resourceStopStartCallable);
        Future future = mock(Future.class);
        ResourceRequestResult resourceRequestResult = mock(ResourceRequestResult.class);
        when(future.get()).thenReturn(resourceRequestResult);
        when(resourceBuilderExecutor.submit(any(Callable.class))).thenReturn(future);
        CloudInstance cloudInstance1 = mock(CloudInstance.class);
        CloudInstance cloudInstance2 = mock(CloudInstance.class);
        CloudVmInstanceStatus cloudVmInstanceStatus1 = new CloudVmInstanceStatus(cloudInstance1, InstanceStatus.STOPPED);
        CloudVmInstanceStatus cloudVmInstanceStatus2 = new CloudVmInstanceStatus(cloudInstance2, InstanceStatus.STOPPED);
        when(resourceRequestResult.getResult()).thenReturn(List.of(cloudVmInstanceStatus1, cloudVmInstanceStatus2));
        List<CloudInstance> cloudInstanceList = List.of(cloudInstance1, cloudInstance2);
        PollTask pollTask = mock(PollTask.class);
        when(resourcePollTaskFactory.newPollComputeStatusTask(eq(computeResourceBuilder), eq(authenticatedContext), eq(resourceBuilderContext),
                eq(cloudInstanceList))).thenReturn(pollTask);
        underTest.startInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        verify(resourceBuilders, times(1)).getStopStartBatchSize(AWS_VARIANT);
        verify(resourceBuilders, times(1)).compute(AWS_VARIANT);
        verify(computeResourceBuilder, times(1)).isInstanceBuilder();
        ArgumentCaptor<ResourceStopStartCallablePayload> resourceStopStartCallablePayloadArgumentCaptor =
                ArgumentCaptor.forClass(ResourceStopStartCallablePayload.class);
        verify(resourceActionFactory, times(1)).buildStopStartCallable(resourceStopStartCallablePayloadArgumentCaptor.capture());
        ResourceStopStartCallablePayload resourceStopStartCallablePayload = resourceStopStartCallablePayloadArgumentCaptor.getValue();
        assertThat(resourceStopStartCallablePayload.getInstances()).containsExactlyInAnyOrder(cloudInstance1, cloudInstance2);
        verify(resourceBuilderExecutor, times(1)).submit(resourceStopStartCallable);
        verify(syncVMPollingScheduler, times(1)).schedule(pollTask, MAX_POLLING_ATTEMPT);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStopAndStartInstancesWhenCallableThrowExceptionFromTheFutureShouldPropagateException(boolean startOperation) throws Exception {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilders.getStopStartBatchSize(any())).thenReturn(10);
        ComputeResourceBuilder computeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(computeResourceBuilder.isInstanceBuilder()).thenReturn(true);
        when(resourceBuilders.compute(any())).thenReturn(List.of(computeResourceBuilder));
        ResourceStopStartCallable resourceStopStartCallable = mock(ResourceStopStartCallable.class);
        when(resourceActionFactory.buildStopStartCallable(any())).thenReturn(resourceStopStartCallable);
        Future future = mock(Future.class);
        CloudInstance cloudInstance1 = mock(CloudInstance.class);
        CloudInstance cloudInstance2 = mock(CloudInstance.class);
        when(resourceBuilderExecutor.submit(any(Callable.class))).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException("Failed", new CloudConnectorException("Connector Failure")));
        List<CloudInstance> cloudInstanceList = List.of(cloudInstance1, cloudInstance2);

        Executable executable;
        if (startOperation) {
            executable = () -> underTest.startInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        } else {
            executable = () -> underTest.stopInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        }
        assertThrows(CloudConnectorException.class,
                executable,
                "The execution of infrastructure operations failed: Connector Failure");

        verify(resourceBuilders, times(1)).getStopStartBatchSize(AWS_VARIANT);
        verify(resourceBuilders, times(1)).compute(AWS_VARIANT);
        verify(computeResourceBuilder, times(1)).isInstanceBuilder();
        ArgumentCaptor<ResourceStopStartCallablePayload> resourceStopStartCallablePayloadArgumentCaptor =
                ArgumentCaptor.forClass(ResourceStopStartCallablePayload.class);
        verify(resourceActionFactory, times(1)).buildStopStartCallable(resourceStopStartCallablePayloadArgumentCaptor.capture());
        ResourceStopStartCallablePayload resourceStopStartCallablePayload = resourceStopStartCallablePayloadArgumentCaptor.getValue();
        assertThat(resourceStopStartCallablePayload.getInstances()).containsExactlyInAnyOrder(cloudInstance1, cloudInstance2);
        verify(resourceBuilderExecutor, times(1)).submit(resourceStopStartCallable);
        verify(syncVMPollingScheduler, times(0)).schedule(any(), anyInt());
    }

    @Test
    void testResourceDeletionSuccess() throws Exception {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilders.compute(AWS_VARIANT)).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(resourceBuilderContext.getParallelResourceRequest()).thenReturn(2);

        when(computeResourceBuilder1.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        when(computeResourceBuilder2.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);

        givenDeletionResult(FutureResult.SUCCESS, new CloudResourceStatus(cloudResource, ResourceStatus.DELETED));

        List<CloudResourceStatus> cloudResourceStatuses =
                underTest.deleteResources(resourceBuilderContext, authenticatedContext, List.of(instance1, instance2, volume1, volume2), false, true);

        assertThat(cloudResourceStatuses).hasSize(4);
        verify(resourceActionFactory, times(4)).buildDeletionCallable(deletionCallableCaptor.capture());

        List<ResourceDeletionCallablePayload> deletionCallables = deletionCallableCaptor.getAllValues();
        assertThat(deletionCallables).hasSize(4);
        assertEquals(deletionCallables.get(0).getBuilder(), computeResourceBuilder2);
        assertEquals(deletionCallables.get(0).getResource(), volume1);
        assertEquals(deletionCallables.get(1).getBuilder(), computeResourceBuilder2);
        assertEquals(deletionCallables.get(1).getResource(), volume2);
        assertEquals(deletionCallables.get(2).getBuilder(), computeResourceBuilder1);
        assertEquals(deletionCallables.get(2).getResource(), instance1);
        assertEquals(deletionCallables.get(3).getBuilder(), computeResourceBuilder1);
        assertEquals(deletionCallables.get(3).getResource(), instance2);
    }

    @Test
    void testResourceDeletionFailure() throws ExecutionException, InterruptedException {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilders.compute(AWS_VARIANT)).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(resourceBuilderContext.getParallelResourceRequest()).thenReturn(1);
        givenDeletionResult(
                FutureResult.FAILED, new CloudResourceStatus(cloudResource, ResourceStatus.FAILED, "No permission to delete."));

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.deleteResources(resourceBuilderContext, authenticatedContext, List.of(cloudResource), false, true));

        assertEquals("Resource deletion failed. Reason: No permission to delete.", cloudConnectorException.getMessage());
    }

    @Test
    void testBuildResourcesForLaunch() throws ExecutionException, InterruptedException {
        ResourceBuilderContext resourceBuilderContext = new ResourceBuilderContext("name", Location.location(Region.region("region")), 8, true);
        when(cloudResource.getType()).thenReturn(ResourceType.AWS_INSTANCE);
        CloudStack cloudStack = mock(CloudStack.class);
        Group group1 = mock(Group.class);
        Group group2 = mock(Group.class);
        when(cloudStack.getGroups()).thenReturn(List.of(group1, group2));
        InstanceTemplate template = new InstanceTemplate("flavor", "group1", 1L, List.of(), InstanceStatus.CREATE_REQUESTED, null, 1L, "imageId", null, 1L);
        CloudInstance cloudInstance1 = new CloudInstance("instance1", template, null, null, null);
        CloudInstance cloudInstance2 = new CloudInstance("instance2", template, null, null, null);
        when(group1.getInstances()).thenReturn(List.of(cloudInstance1));
        when(group2.getInstances()).thenReturn(List.of(cloudInstance2));
        when(resourceBuilders.getCreateBatchSize(any())).thenReturn(Integer.valueOf(8));
        ResourceCreationCallable resourceCreationCallable = mock(ResourceCreationCallable.class);
        when(resourceActionFactory.buildCreationCallable(any())).thenReturn(resourceCreationCallable);
        Future resourceRequestFuture = mock(Future.class);
        ResourceRequestResult<List<CloudResourceStatus>> resourceRequestResult = new ResourceRequestResult<>(FutureResult.SUCCESS,
                List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));
        when(resourceRequestFuture.get()).thenReturn(resourceRequestResult);
        when(resourceBuilderExecutor.submit(eq(resourceCreationCallable))).thenReturn(resourceRequestFuture);
        List<Boolean> rollbackContextBuildValue = new ArrayList<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                // this is required to assert on the cloudFailureContext.getCtx().isBuild() value, because it gets modified before the mockito verify call
                CloudFailureContext cloudFailureContext = invocation.getArgument(0, CloudFailureContext.class);
                rollbackContextBuildValue.add(cloudFailureContext.getCtx().isBuild());
                return null;
            }
        }).when(cloudFailureHandler).rollbackIfNecessary(any(), anyList(), anyList(), any(), any());

        List<CloudResourceStatus> cloudResourceStatuses = underTest.buildResourcesForLaunch(resourceBuilderContext, authenticatedContext, cloudStack, null);
        assertThat(cloudResourceStatuses).hasSize(2);
        assertThat(cloudResourceStatuses).extracting(cloudResourceStatus -> cloudResourceStatus.getStatus()).containsOnly(ResourceStatus.CREATED);
        verify(resourceActionFactory, times(2)).buildCreationCallable(
                argThat(resourceCreationCallablePayload -> resourceCreationCallablePayload.getContext().isBuild()));
        verify(cloudFailureHandler, times(1)).rollbackIfNecessary(any(), anyList(), anyList(), eq(group1), any());
        verify(cloudFailureHandler, times(1)).rollbackIfNecessary(any(), anyList(), anyList(), eq(group2), any());
        assertThat(rollbackContextBuildValue).containsExactly(Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    void waitForResourceCreationsWithExceptionDuringRollback() throws Exception {
        ResourceBuilderContext resourceBuilderContext = new ResourceBuilderContext("name", Location.location(Region.region("region")), 8, true);
        when(cloudResource.getType()).thenReturn(ResourceType.AWS_INSTANCE);
        CloudStack cloudStack = mock(CloudStack.class);
        Group group1 = mock(Group.class);
        Group group2 = mock(Group.class);
        when(cloudStack.getGroups()).thenReturn(List.of(group1, group2));
        InstanceTemplate template = new InstanceTemplate("flavor", "group1", 1L, List.of(), InstanceStatus.CREATE_REQUESTED, null, 1L, "imageId", null, 1L);
        CloudInstance cloudInstance1 = new CloudInstance("instance1", template, null, null, null);
        CloudInstance cloudInstance2 = new CloudInstance("instance2", template, null, null, null);
        when(group1.getInstances()).thenReturn(List.of(cloudInstance1));
        when(group2.getInstances()).thenReturn(List.of(cloudInstance2));
        when(resourceBuilders.getCreateBatchSize(any())).thenReturn(Integer.valueOf(8));
        ResourceCreationCallable resourceCreationCallable = mock(ResourceCreationCallable.class);
        when(resourceActionFactory.buildCreationCallable(any())).thenReturn(resourceCreationCallable);
        Future resourceRequestFuture = mock(Future.class);
        ResourceRequestResult<List<CloudResourceStatus>> resourceRequestResult = new ResourceRequestResult<>(FutureResult.SUCCESS,
                List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));
        when(resourceRequestFuture.get()).thenReturn(resourceRequestResult);
        when(resourceBuilderExecutor.submit(eq(resourceCreationCallable))).thenReturn(resourceRequestFuture);

        List<CloudResourceStatus> cloudResourceStatuses = List.of(mock(CloudResourceStatus.class));
        Future<ResourceRequestResult<List<CloudResourceStatus>>> future = mock(Future.class);
        List<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = List.of(future);
        doThrow(new RuntimeException("Rollback failed")).when(cloudFailureHandler).rollbackIfNecessary(any(), anyList(), anyList(), any(), anyInt());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            underTest.buildResourcesForLaunch(resourceBuilderContext, authenticatedContext, cloudStack, null);
        });
        assertEquals("Multiple exceptions occurred during resource creation: 1. Rollback failed 2. Rollback failed ", exception.getMessage());
    }

    private Future<ResourceRequestResult<List<CloudResourceStatus>>> givenDeletionResult(FutureResult futureResult, CloudResourceStatus cloudResourceStatus)
            throws ExecutionException, InterruptedException {
        ResourceDeletionCallable resourceDeletionCallable = mock(ResourceDeletionCallable.class);
        when(resourceActionFactory.buildDeletionCallable(any())).thenReturn(resourceDeletionCallable);
        Future<ResourceRequestResult<List<CloudResourceStatus>>> future = mock(Future.class);
        when(resourceBuilderExecutor.submit(resourceDeletionCallable)).thenReturn(future);
        when(future.get()).thenReturn(new ResourceRequestResult<>(futureResult, List.of(cloudResourceStatus)));
        return future;
    }

    private CloudResource newResource(String s, ResourceType resourceType, CommonStatus created) {
        return CloudResource.builder().withName(s).withType(resourceType).withStatus(created)
                .withParameters(new HashMap<>()).build();
    }

}