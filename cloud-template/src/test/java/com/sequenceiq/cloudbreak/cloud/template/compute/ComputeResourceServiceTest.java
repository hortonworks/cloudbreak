package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@ExtendWith(MockitoExtension.class)
class ComputeResourceServiceTest {

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

    @Test
    public void startInstancesTest() throws Exception {
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
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Variant variant = Variant.variant("AWS");
        when(cloudContext.getVariant()).thenReturn(variant);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());
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
        verify(resourceBuilders, times(1)).getStopStartBatchSize(variant);
        verify(resourceBuilders, times(1)).compute(variant);
        verify(computeResourceBuilder, times(1)).isInstanceBuilder();
        ArgumentCaptor<ResourceStopStartCallablePayload> resourceStopStartCallablePayloadArgumentCaptor =
                ArgumentCaptor.forClass(ResourceStopStartCallablePayload.class);
        verify(resourceActionFactory, times(1)).buildStopStartCallable(resourceStopStartCallablePayloadArgumentCaptor.capture());
        ResourceStopStartCallablePayload resourceStopStartCallablePayload = resourceStopStartCallablePayloadArgumentCaptor.getValue();
        assertThat(resourceStopStartCallablePayload.getInstances()).containsExactlyInAnyOrder(cloudInstance1, cloudInstance2);
        verify(resourceBuilderExecutor, times(1)).submit(resourceStopStartCallable);
        verify(syncVMPollingScheduler, times(1)).schedule(pollTask);

    }
}