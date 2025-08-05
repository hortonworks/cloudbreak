package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
import org.mockito.stubbing.Answer;

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
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Mock
    private SyncPollingScheduler<List<CloudVmInstanceStatus>> syncVMPollingScheduler;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private ComputeResourceBuilder<ResourceBuilderContext> computeResourceBuilder1;

    @Mock
    private ComputeResourceBuilder<ResourceBuilderContext> computeResourceBuilder2;

    @Mock
    private CloudFailureHandler cloudFailureHandler;

    @Captor
    private ArgumentCaptor<ResourceDeletionCallablePayload> deletionCallableCaptor;

    @BeforeEach
    public void setUp() {
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getVariant()).thenReturn(AWS_VARIANT);
    }

    @Test
    void startInstancesTest() throws Exception {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilderContext.getResourceBuilderPoolSize()).thenReturn(10);
        ComputeResourceBuilder computeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(computeResourceBuilder.isInstanceBuilder()).thenReturn(true);
        when(resourceBuilders.compute(any())).thenReturn(List.of(computeResourceBuilder));
        when(resourceActionFactory.buildStopStartCallable(any())).thenAnswer(invocation -> {
            ResourceStopStartCallablePayload payload = invocation.getArgument(0, ResourceStopStartCallablePayload.class);
            ResourceStopStartCallable resourceStopStartCallable = mock(ResourceStopStartCallable.class);
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = payload.getInstances().stream()
                    .map(instance -> new CloudVmInstanceStatus(instance, InstanceStatus.STOPPED)).toList();
            when(resourceStopStartCallable.call()).thenReturn(new ResourceRequestResult<>(FutureResult.SUCCESS, cloudVmInstanceStatuses));
            return resourceStopStartCallable;
        });
        CloudInstance cloudInstance1 = mock(CloudInstance.class);
        CloudInstance cloudInstance2 = mock(CloudInstance.class);
        List<CloudInstance> cloudInstanceList = List.of(cloudInstance1, cloudInstance2);
        PollTask pollTask = mock(PollTask.class);
        when(resourcePollTaskFactory.newPollComputeStatusTask(eq(computeResourceBuilder), eq(authenticatedContext), eq(resourceBuilderContext),
                eq(cloudInstanceList))).thenReturn(pollTask);
        underTest.startInstances(resourceBuilderContext, authenticatedContext, cloudInstanceList);
        verify(resourceBuilders, times(1)).compute(AWS_VARIANT);
        verify(computeResourceBuilder, times(1)).isInstanceBuilder();
        ArgumentCaptor<ResourceStopStartCallablePayload> resourceStopStartCallablePayloadArgumentCaptor =
                ArgumentCaptor.forClass(ResourceStopStartCallablePayload.class);
        verify(resourceActionFactory, times(2)).buildStopStartCallable(resourceStopStartCallablePayloadArgumentCaptor.capture());
        List<ResourceStopStartCallablePayload> resourceStopStartCallablePayloads = resourceStopStartCallablePayloadArgumentCaptor.getAllValues();
        assertThat(resourceStopStartCallablePayloads.stream()
                .flatMap(resourceStopStartCallablePayload -> resourceStopStartCallablePayload.getInstances().stream())
                .collect(Collectors.toList())).containsExactlyInAnyOrder(cloudInstance1, cloudInstance2);
        verify(syncVMPollingScheduler, times(1)).schedule(pollTask, MAX_POLLING_ATTEMPT);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStopAndStartInstancesWhenCallableThrowExceptionFromTheFutureShouldPropagateException(boolean startOperation) throws Exception {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilderContext.getResourceBuilderPoolSize()).thenReturn(10);
        ComputeResourceBuilder computeResourceBuilder = mock(ComputeResourceBuilder.class);
        when(computeResourceBuilder.isInstanceBuilder()).thenReturn(true);
        when(resourceBuilders.compute(any())).thenReturn(List.of(computeResourceBuilder));

        when(resourceActionFactory.buildStopStartCallable(any())).thenAnswer(invocation -> {
            ResourceStopStartCallablePayload payload = invocation.getArgument(0, ResourceStopStartCallablePayload.class);
            ResourceStopStartCallable resourceStopStartCallable = mock(ResourceStopStartCallable.class);
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = payload.getInstances().stream()
                    .map(instance -> new CloudVmInstanceStatus(instance, InstanceStatus.STOPPED)).toList();
            when(resourceStopStartCallable.call()).thenThrow(new CloudConnectorException("Connector Failure"));
            return resourceStopStartCallable;
        });

        CloudInstance cloudInstance1 = mock(CloudInstance.class);
        CloudInstance cloudInstance2 = mock(CloudInstance.class);
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

        verify(resourceBuilders, times(1)).compute(AWS_VARIANT);
        verify(computeResourceBuilder, times(1)).isInstanceBuilder();
        ArgumentCaptor<ResourceStopStartCallablePayload> resourceStopStartCallablePayloadArgumentCaptor =
                ArgumentCaptor.forClass(ResourceStopStartCallablePayload.class);
        verify(resourceActionFactory, times(2)).buildStopStartCallable(resourceStopStartCallablePayloadArgumentCaptor.capture());
        List<CloudInstance> cloudInstances = resourceStopStartCallablePayloadArgumentCaptor.getAllValues().stream()
                .flatMap(payload -> payload.getInstances().stream())
                .toList();
        assertThat(cloudInstances).containsExactlyInAnyOrder(cloudInstance1, cloudInstance2);
        verify(syncVMPollingScheduler, times(0)).schedule(any(), anyInt());
    }

    @Test
    void testResourceDeletionSuccess() {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilderContext.getResourceBuilderPoolSize()).thenReturn(10);
        when(resourceBuilders.compute(AWS_VARIANT)).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(resourceBuilderContext.getResourceBuilderPoolSize()).thenReturn(2);
        when(resourceActionFactory.buildDeletionCallable(any())).thenAnswer(invocation -> {
            ResourceDeletionCallablePayload payload = invocation.getArgument(0, ResourceDeletionCallablePayload.class);
            ResourceDeletionCallable deletionCallable = mock(ResourceDeletionCallable.class);
            ResourceRequestResult<List<CloudResourceStatus>> resourceRequestResult = new ResourceRequestResult<>(FutureResult.SUCCESS,
                    List.of(new CloudResourceStatus(payload.getResource(), ResourceStatus.DELETED, payload.getResource().getPrivateId())));
            when(deletionCallable.call()).thenReturn(resourceRequestResult);
            return deletionCallable;
        });

        when(computeResourceBuilder1.resourceType()).thenReturn(ResourceType.AWS_INSTANCE);
        when(computeResourceBuilder2.resourceType()).thenReturn(ResourceType.AWS_VOLUMESET);

        CloudResource instance1 = newResource("instance-1", "group1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        CloudResource instance2 = newResource("instance-2", "group1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        CloudResource volume1 = newResource("vol-1", "group1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        CloudResource volume2 = newResource("vol-2", "group1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);

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
    void testResourceDeletionFailure() {
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        when(resourceBuilders.compute(AWS_VARIANT)).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(resourceBuilderContext.getResourceBuilderPoolSize()).thenReturn(1);
        CloudResource cloudResource = mock(CloudResource.class);
        when(resourceActionFactory.buildDeletionCallable(any())).thenAnswer(invocation -> {
            ResourceDeletionCallablePayload payload = invocation.getArgument(0, ResourceDeletionCallablePayload.class);
            ResourceDeletionCallable deletionCallable = mock(ResourceDeletionCallable.class);
            ResourceRequestResult<List<CloudResourceStatus>> resourceRequestResult = new ResourceRequestResult<>(FutureResult.FAILED,
                    List.of(new CloudResourceStatus(payload.getResource(), ResourceStatus.FAILED, "No permission to delete.",
                            payload.getResource().getPrivateId())));
            when(deletionCallable.call()).thenReturn(resourceRequestResult);
            return deletionCallable;
        });

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.deleteResources(resourceBuilderContext, authenticatedContext, List.of(cloudResource), false, true));

        assertEquals("Resource deletion failed. Reason: No permission to delete.", cloudConnectorException.getMessage());
    }

    @Test
    void testBuildResourcesForLaunch() {
        ResourceBuilderContext resourceBuilderContext = new ResourceBuilderContext("name", Location.location(Region.region("region")), 8, true);
        CloudStack cloudStack = mock(CloudStack.class);
        Group group1 = mock(Group.class);
        Group group2 = mock(Group.class);
        when(cloudStack.getGroups()).thenReturn(List.of(group1, group2));
        InstanceTemplate template = new InstanceTemplate("flavor", "group1", 1L, List.of(), InstanceStatus.CREATE_REQUESTED, null, 1L, "imageId", null, 1L);
        CloudInstance cloudInstance1 = new CloudInstance("instance1", template, null, null, null);
        CloudInstance cloudInstance2 = new CloudInstance("instance2", template, null, null, null);
        when(group1.getInstances()).thenReturn(List.of(cloudInstance1));
        when(group2.getInstances()).thenReturn(List.of(cloudInstance2));
        when(group1.getName()).thenReturn("group1");
        when(group2.getName()).thenReturn("group2");
        when(resourceActionFactory.buildCreationCallable(any())).thenAnswer(invocation -> {
            ResourceCreationCallablePayload payload = invocation.getArgument(0, ResourceCreationCallablePayload.class);
            ResourceCreationCallable creationCallable = mock(ResourceCreationCallable.class);
            List<CloudResourceStatus> cloudResourceStatuses = payload.getInstances().stream().map(instance -> new CloudResourceStatus(
                    newResource(instance.getInstanceId(), instance.getTemplate().getGroupName(), ResourceType.AWS_INSTANCE, CommonStatus.CREATED),
                    ResourceStatus.CREATED)).toList();
            when(creationCallable.call()).thenReturn(new ResourceRequestResult<>(FutureResult.SUCCESS, cloudResourceStatuses));
            return creationCallable;
        });
        List<Boolean> rollbackContextBuildValue = new ArrayList<>();
        doAnswer((Answer<Void>) invocation -> {
            // this is required to assert on the cloudFailureContext.getCtx().isBuild() value, because it gets modified before the mockito verify call
            CloudFailureContext cloudFailureContext = invocation.getArgument(0, CloudFailureContext.class);
            rollbackContextBuildValue.add(cloudFailureContext.getCtx().isBuild());
            return null;
        }).when(cloudFailureHandler).rollbackIfNecessary(any(), anyMap(), anyMap(), any());

        List<CloudResourceStatus> cloudResourceStatuses = underTest.buildResourcesForLaunch(resourceBuilderContext, authenticatedContext, cloudStack, null);
        assertThat(cloudResourceStatuses).hasSize(2);
        assertThat(cloudResourceStatuses).extracting(cloudResourceStatus -> cloudResourceStatus.getStatus()).containsOnly(ResourceStatus.CREATED);
        verify(resourceActionFactory, times(2)).buildCreationCallable(
                argThat(resourceCreationCallablePayload -> resourceCreationCallablePayload.getContext().isBuild()));
        verify(cloudFailureHandler, times(1)).rollbackIfNecessary(any(), anyMap(), anyMap(), any());
        assertThat(rollbackContextBuildValue).containsExactly(Boolean.FALSE);
    }

    @Test
    void waitForResourceCreationsWithExceptionDuringRollback() {
        ResourceBuilderContext resourceBuilderContext = new ResourceBuilderContext("name", Location.location(Region.region("region")), 8, true);
        CloudStack cloudStack = mock(CloudStack.class);
        Group group1 = mock(Group.class);
        when(group1.getName()).thenReturn("group1");
        Group group2 = mock(Group.class);
        when(group2.getName()).thenReturn("group2");
        when(cloudStack.getGroups()).thenReturn(List.of(group1, group2));
        InstanceTemplate template = new InstanceTemplate("flavor", "group1", 1L, List.of(), InstanceStatus.CREATE_REQUESTED, null, 1L, "imageId", null, 1L);
        CloudInstance cloudInstance1 = new CloudInstance("instance1", template, null, null, null);
        CloudInstance cloudInstance2 = new CloudInstance("instance2", template, null, null, null);
        when(group1.getInstances()).thenReturn(List.of(cloudInstance1));
        when(group2.getInstances()).thenReturn(List.of(cloudInstance2));

        when(resourceActionFactory.buildCreationCallable(any())).thenAnswer(invocation -> {
            ResourceCreationCallablePayload payload = invocation.getArgument(0, ResourceCreationCallablePayload.class);
            ResourceCreationCallable creationCallable = mock(ResourceCreationCallable.class);
            List<CloudResourceStatus> cloudResourceStatuses = payload.getInstances().stream().map(instance -> new CloudResourceStatus(
                    newResource(instance.getInstanceId(), instance.getTemplate().getGroupName(), ResourceType.AWS_INSTANCE, CommonStatus.CREATED),
                    ResourceStatus.CREATED)).toList();
            when(creationCallable.call()).thenReturn(new ResourceRequestResult<>(FutureResult.SUCCESS, cloudResourceStatuses));
            return creationCallable;
        });

        doThrow(new RuntimeException("Rollback failed")).when(cloudFailureHandler).rollbackIfNecessary(any(), anyMap(), anyMap(), any());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            underTest.buildResourcesForLaunch(resourceBuilderContext, authenticatedContext, cloudStack, null);
        });
        assertEquals("Rollback failed", exception.getMessage());
    }

    private CloudResource newResource(String s, String group, ResourceType resourceType, CommonStatus created) {
        return CloudResource.builder().withGroup(group).withName(s).withType(resourceType).withStatus(created)
                .withParameters(new HashMap<>()).build();
    }

}