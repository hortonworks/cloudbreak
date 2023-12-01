package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@ExtendWith(MockitoExtension.class)
class ComputeResourceServiceTest {

    private static final Variant AWS_VARIANT = Variant.variant("AWS");

    @InjectMocks
    private ComputeResourceService computeResourceService;

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
    private ResourceBuilderContext resourceBuilderContext;

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

    @Captor
    private ArgumentCaptor<ResourceDeletionCallablePayload> deletionCallableCaptor;

    @BeforeEach
    public void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getVariant()).thenReturn(AWS_VARIANT);
    }

    @Test
    void startInstancesTest() throws Exception {
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
        computeResourceService.startInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        verify(resourceBuilders, times(1)).getStopStartBatchSize(AWS_VARIANT);
        verify(resourceBuilders, times(1)).compute(AWS_VARIANT);
        verify(computeResourceBuilder, times(1)).isInstanceBuilder();
        ArgumentCaptor<ResourceStopStartCallablePayload> resourceStopStartCallablePayloadArgumentCaptor =
                ArgumentCaptor.forClass(ResourceStopStartCallablePayload.class);
        verify(resourceActionFactory, times(1)).buildStopStartCallable(resourceStopStartCallablePayloadArgumentCaptor.capture());
        ResourceStopStartCallablePayload resourceStopStartCallablePayload = resourceStopStartCallablePayloadArgumentCaptor.getValue();
        assertThat(resourceStopStartCallablePayload.getInstances()).containsExactlyInAnyOrder(cloudInstance1, cloudInstance2);
        verify(resourceBuilderExecutor, times(1)).submit(resourceStopStartCallable);
        verify(syncVMPollingScheduler, times(1)).schedule(pollTask);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStopAndStartInstancesWhenCallableThrowExceptionFromTheFutureShouldPropagateException(boolean startOperation) throws Exception {
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
            executable = () -> computeResourceService.startInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        } else {
            executable = () -> computeResourceService.stopInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        }
        Assertions.assertThrows(CloudConnectorException.class,
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
        verify(syncVMPollingScheduler, times(0)).schedule(any());
    }

    @Test
    void testResourceDeletionSuccess() throws ExecutionException, InterruptedException {
        when(resourceBuilders.compute(AWS_VARIANT)).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(resourceBuilderContext.getParallelResourceRequest()).thenReturn(2);

        givenDeletionResult(FutureResult.SUCCESS, new CloudResourceStatus(cloudResource, ResourceStatus.DELETED));

        List<CloudResourceStatus> cloudResourceStatuses =
                computeResourceService.deleteResources(resourceBuilderContext, authenticatedContext, List.of(cloudResource), false);

        assertThat(cloudResourceStatuses).hasSize(2);
        verify(resourceActionFactory, times(2)).buildDeletionCallable(deletionCallableCaptor.capture());

        List<ResourceDeletionCallablePayload> deletionCallables = deletionCallableCaptor.getAllValues();
        assertThat(deletionCallables).hasSize(2);
        assertEquals(deletionCallables.get(0).getBuilder(), computeResourceBuilder2);
        assertEquals(deletionCallables.get(1).getBuilder(), computeResourceBuilder1);
    }

    @Test
    void testResourceDeletionFailure() throws ExecutionException, InterruptedException {
        when(resourceBuilders.compute(AWS_VARIANT)).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(resourceBuilderContext.getParallelResourceRequest()).thenReturn(1);
        givenDeletionResult(
                FutureResult.FAILED, new CloudResourceStatus(cloudResource, ResourceStatus.FAILED, "No permission to delete."));

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> computeResourceService.deleteResources(resourceBuilderContext, authenticatedContext, List.of(cloudResource), false));

        assertEquals("Resource deletion failed. Reason: No permission to delete.", cloudConnectorException.getMessage());
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
}