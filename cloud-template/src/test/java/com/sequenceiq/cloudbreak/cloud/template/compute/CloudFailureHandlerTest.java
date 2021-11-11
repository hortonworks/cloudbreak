package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class CloudFailureHandlerTest {

    @InjectMocks
    private CloudFailureHandler cloudFailureHandler;

    @Mock
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Spy
    private ResourceActionFactory resourceActionFactory;

    @Test
    void rollbackOnExactDoesNotReachThreshold() throws Exception {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 4L);
        Group group = mock(Group.class);

        List<CloudResourceStatus> failuresList = new ArrayList<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L));
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L));
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L));

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        List<CloudResourceStatus> resourceStatuses = new ArrayList<>(failuresList);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L));
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L));

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L));
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L));
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L));
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L));
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L));
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L));
        ResourceBuilders resourceBuilders = mock(ResourceBuilders.class);

        ArrayList<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = new ArrayList<>();
        ComputeResourceBuilder instanceResourceBuilder = mock(ComputeResourceBuilder.class);
        when(instanceResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        computeResourceBuilders.add(instanceResourceBuilder);
        ComputeResourceBuilder volumeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(volumeResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        computeResourceBuilders.add(volumeResourceBuilder);
        when(resourceBuilders.compute(any())).thenReturn(computeResourceBuilders);

        ArgumentCaptor<ResourceDeletionCallable> callableArgumentCaptor = ArgumentCaptor.forClass(ResourceDeletionCallable.class);
        when(resourceBuilderExecutor.submit(callableArgumentCaptor.capture()))
                .thenAnswer(invocation -> ((Callable) invocation.getArgument(0)).call());

        RolledbackResourcesException rolledbackResourcesException = assertThrows(RolledbackResourcesException.class,
                () -> cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth,
                        scaleContext, resourceBuilderContext), failuresList, resourceStatuses, group, resourceBuilders, 6));

        assertEquals("Resources are rolled back because successful node count was lower than threshold. 3 nodes are failed. Error reason: failed instance",
                rolledbackResourcesException.getMessage());

        verifyDeleteAll(instanceResourceBuilder, resourceBuilderContext, auth, instance1, instance5, instance6);
        verifyDeleteAll(volumeResourceBuilder, resourceBuilderContext, auth, volume1, volume2, volume4, volume5, volume6);

        verify(resourceBuilderExecutor, times(11)).submit(any(Callable.class));
    }

    @Test
    void dontRollbackWhenExactReachThreshold() throws Exception {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 3L);
        Group group = mock(Group.class);

        ArrayList<CloudResourceStatus> failuresList = new ArrayList<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L));
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L));
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L));

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        ArrayList<CloudResourceStatus> resourceStatuses = new ArrayList<>(failuresList);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L));
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L));

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L));
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L));
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L));
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L));
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L));
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L));
        ResourceBuilders resourceBuilders = mock(ResourceBuilders.class);

        ArrayList<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = new ArrayList<>();
        ComputeResourceBuilder instanceResourceBuilder = mock(ComputeResourceBuilder.class);
        when(instanceResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        computeResourceBuilders.add(instanceResourceBuilder);
        ComputeResourceBuilder volumeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(volumeResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        computeResourceBuilders.add(volumeResourceBuilder);
        when(resourceBuilders.compute(any())).thenReturn(computeResourceBuilders);

        ArgumentCaptor<ResourceDeletionCallable> callableArgumentCaptor = ArgumentCaptor.forClass(ResourceDeletionCallable.class);
        when(resourceBuilderExecutor.submit(callableArgumentCaptor.capture()))
                .thenAnswer(invocation -> ((Callable) invocation.getArgument(0)).call());

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failuresList, resourceStatuses, group,
                resourceBuilders, 6);

        verifyDeleteAll(volumeResourceBuilder, resourceBuilderContext, auth, volume2, volume4);

        verify(resourceBuilderExecutor, times(5)).submit(any(Callable.class));
    }

    @Test
    void rollbackOnPercentageDoesNotReachThreshold() throws Exception {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.PERCENTAGE, 60L);
        Group group = mock(Group.class);

        ArrayList<CloudResourceStatus> failuresList = new ArrayList<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L));
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L));
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L));

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        ArrayList<CloudResourceStatus> resourceStatuses = new ArrayList<>(failuresList);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L));
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L));

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L));
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L));
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L));
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L));
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L));
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L));
        ResourceBuilders resourceBuilders = mock(ResourceBuilders.class);

        ArrayList<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = new ArrayList<>();
        ComputeResourceBuilder instanceResourceBuilder = mock(ComputeResourceBuilder.class);
        when(instanceResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        computeResourceBuilders.add(instanceResourceBuilder);
        ComputeResourceBuilder volumeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(volumeResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        computeResourceBuilders.add(volumeResourceBuilder);
        when(resourceBuilders.compute(any())).thenReturn(computeResourceBuilders);

        ArgumentCaptor<ResourceDeletionCallable> callableArgumentCaptor = ArgumentCaptor.forClass(ResourceDeletionCallable.class);
        when(resourceBuilderExecutor.submit(callableArgumentCaptor.capture()))
                .thenAnswer(invocation -> ((Callable) invocation.getArgument(0)).call());

        RolledbackResourcesException rolledbackResourcesException = assertThrows(RolledbackResourcesException.class,
                () -> cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth,
                        scaleContext, resourceBuilderContext), failuresList, resourceStatuses, group, resourceBuilders, 6));

        assertEquals("Resources are rolled back because successful node count was lower than threshold. 3 nodes are failed. Error reason: failed instance",
                rolledbackResourcesException.getMessage());

        verifyDeleteAll(instanceResourceBuilder, resourceBuilderContext, auth, instance1, instance5, instance6);
        verifyDeleteAll(volumeResourceBuilder, resourceBuilderContext, auth, volume1, volume2, volume4, volume5, volume6);

        verify(resourceBuilderExecutor, times(11)).submit(any(Callable.class));
    }

    @Test
    void dontRollbackWhenPercentageReachThreshold() throws Exception {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.PERCENTAGE, 40L);
        Group group = mock(Group.class);

        ArrayList<CloudResourceStatus> failuresList = new ArrayList<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L));
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L));
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L));

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        ArrayList<CloudResourceStatus> resourceStatuses = new ArrayList<>(failuresList);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L));
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L));

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L));
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L));
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L));
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L));
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L));
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L));
        ResourceBuilders resourceBuilders = mock(ResourceBuilders.class);

        ArrayList<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = new ArrayList<>();
        ComputeResourceBuilder instanceResourceBuilder = mock(ComputeResourceBuilder.class);
        when(instanceResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        computeResourceBuilders.add(instanceResourceBuilder);
        ComputeResourceBuilder volumeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(volumeResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        computeResourceBuilders.add(volumeResourceBuilder);
        when(resourceBuilders.compute(any())).thenReturn(computeResourceBuilders);

        ArgumentCaptor<ResourceDeletionCallable> callableArgumentCaptor = ArgumentCaptor.forClass(ResourceDeletionCallable.class);
        when(resourceBuilderExecutor.submit(callableArgumentCaptor.capture()))
                .thenAnswer(invocation -> ((Callable) invocation.getArgument(0)).call());

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failuresList, resourceStatuses, group,
                resourceBuilders, 6);

        verifyDeleteAll(volumeResourceBuilder, resourceBuilderContext, auth, volume2, volume4);

        verify(resourceBuilderExecutor, times(5)).submit(any(Callable.class));
    }

    @Test
    void dontRollbackOnBestEffort() throws Exception {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.BEST_EFFORT, 0L);
        Group group = mock(Group.class);

        ArrayList<CloudResourceStatus> failuresList = new ArrayList<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L));
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L));
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failuresList.add(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L));

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        ArrayList<CloudResourceStatus> resourceStatuses = new ArrayList<>(failuresList);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L));
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L));

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L));
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L));
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L));
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L));
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L));
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resourceStatuses.add(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L));
        ResourceBuilders resourceBuilders = mock(ResourceBuilders.class);

        ArrayList<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = new ArrayList<>();
        ComputeResourceBuilder instanceResourceBuilder = mock(ComputeResourceBuilder.class);
        when(instanceResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        computeResourceBuilders.add(instanceResourceBuilder);
        ComputeResourceBuilder volumeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(volumeResourceBuilder.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        computeResourceBuilders.add(volumeResourceBuilder);
        when(resourceBuilders.compute(any())).thenReturn(computeResourceBuilders);

        ArgumentCaptor<ResourceDeletionCallable> callableArgumentCaptor = ArgumentCaptor.forClass(ResourceDeletionCallable.class);
        when(resourceBuilderExecutor.submit(callableArgumentCaptor.capture()))
                .thenAnswer(invocation -> ((Callable) invocation.getArgument(0)).call());

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failuresList, resourceStatuses, group,
                resourceBuilders, 6);

        verifyDeleteAll(volumeResourceBuilder, resourceBuilderContext, auth, volume2, volume4);

        verify(resourceBuilderExecutor, times(5)).submit(any(Callable.class));
    }

    private void verifyDeleteAll(ComputeResourceBuilder computeResourceBuilder, ResourceBuilderContext resourceBuilderContext, AuthenticatedContext auth,
            CloudResource... cloudResources) throws Exception {
        for (CloudResource cloudResource : cloudResources) {
            verify(computeResourceBuilder).delete(resourceBuilderContext, auth, cloudResource);
        }
    }

    private CloudResource newResource(String s, ResourceType awsVolumeset, CommonStatus created) {
        return CloudResource.builder().name(s).type(awsVolumeset).status(created)
                .params(new HashMap<>()).build();
    }

}