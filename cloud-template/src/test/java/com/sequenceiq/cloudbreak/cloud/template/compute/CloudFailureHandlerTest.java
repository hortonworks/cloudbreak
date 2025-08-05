package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class CloudFailureHandlerTest {

    @InjectMocks
    private CloudFailureHandler cloudFailureHandler;

    @Mock
    private Optional<CloudbreakEventService> cloudbreakEventServiceOptional;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ComputeResourceService computeResourceService;

    @BeforeEach
    void init() {
        when(cloudbreakEventServiceOptional.isPresent()).thenReturn(Boolean.TRUE);
        when(cloudbreakEventServiceOptional.get()).thenReturn(cloudbreakEventService);
    }

    @Test
    void rollbackOnExactDoesNotReachThreshold() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 4L);
        Group group = mock(Group.class);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L), group);
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L), group);
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L), group);
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L), group);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L), group);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L), group);
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L), group);
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L), group);
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L), group);
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L), group);

        RolledbackResourcesException rolledbackResourcesException = assertThrows(RolledbackResourcesException.class,
                () -> cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth,
                        scaleContext, resourceBuilderContext), failedResources, allResources, 6));

        assertEquals("Resources are rolled back because successful node count was lower than threshold. 3 nodes are failed. Error reason: failed instance",
                rolledbackResourcesException.getMessage());

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_CREATION_FAILED"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED), any());

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(),
                eq(false), eq(false));
        assertThat(iterableArgumentCaptor.getValue())
                .containsExactly(instance1, instance2, instance3, instance4, instance5, instance6, volume1, volume2, volume4, volume5, volume6);
    }

    @Test
    void dontRollbackWhenExactReachThreshold() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 3L);
        Group group = mock(Group.class);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L), group);
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L), group);
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L), group);
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L), group);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L), group);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L), group);
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L), group);
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L), group);
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L), group);
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L), group);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 6);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_CREATION_FAILED"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED), any());

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(),
                eq(false), eq(false));
        assertThat(iterableArgumentCaptor.getValue())
                .containsExactlyInAnyOrder(instance2, instance3, instance4, volume2, volume4);
    }

    @Test
    void rollbackOnPercentageDoesNotReachThreshold() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.PERCENTAGE, 60L);
        Group group = mock(Group.class);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L), group);
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L), group);
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L), group);
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L), group);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L), group);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L), group);
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L), group);
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L), group);
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L), group);
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L), group);

        RolledbackResourcesException rolledbackResourcesException = assertThrows(RolledbackResourcesException.class,
                () -> cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth,
                        scaleContext, resourceBuilderContext), failedResources, allResources, 6));

        assertEquals("Resources are rolled back because successful node count was lower than threshold. 3 nodes are failed. Error reason: failed instance",
                rolledbackResourcesException.getMessage());

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_CREATION_FAILED"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED), any());

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(),
                eq(false), eq(false));
        assertThat(iterableArgumentCaptor.getValue())
                .containsExactly(instance1, instance2, instance3, instance4, instance5, instance6, volume1, volume2, volume4, volume5, volume6);
    }

    @Test
    void dontRollbackWhenPercentageReachThreshold() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.PERCENTAGE, 40L);
        Group group = mock(Group.class);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L), group);
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L), group);
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L), group);
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L), group);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L), group);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L), group);
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L), group);
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L), group);
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L), group);
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L), group);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 6);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_CREATION_FAILED"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED), any());

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(),
                eq(false), eq(false));
        assertThat(iterableArgumentCaptor.getValue())
                .containsExactly(instance2, instance3, instance4, volume2, volume4);
    }

    @Test
    void dontRollbackOnBestEffort() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.BEST_EFFORT, 0L);
        Group group = mock(Group.class);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L), group);
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L), group);
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L), group);
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L), group);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L), group);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L), group);
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L), group);
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L), group);
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L), group);
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L), group);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 6);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_CREATION_FAILED"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED), any());

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(),
                eq(false), eq(false));
        assertThat(iterableArgumentCaptor.getValue())
                .containsExactly(instance2, instance3, instance4, volume2, volume4);
    }

    @Test
    void testRollbackSameCloudresourceMultipleTimes() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 4L);
        Group group = mock(Group.class);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        CloudResource instance2 = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance2, ResourceStatus.FAILED, "failed instance", 2L), group);
        CloudResource instance3 = newResource("instance-3", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance3, ResourceStatus.FAILED, "failed instance", 3L), group);
        CloudResource instance4 = newResource("instance-4", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        failedResources.put(new CloudResourceStatus(instance4, ResourceStatus.FAILED, "failed instance", 4L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);

        CloudResource volume2 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume2, ResourceStatus.CREATED, 2L), group);
        CloudResource volume3 = newResource("vol-2", ResourceType.AWS_VOLUMESET, CommonStatus.FAILED);
        allResources.put(new CloudResourceStatus(volume3, ResourceStatus.CREATED, 2L), group);
        CloudResource volume4 = newResource("vol-4", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume4, ResourceStatus.CREATED, 4L), group);

        CloudResource instance1 = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance1, ResourceStatus.CREATED, 1L), group);
        CloudResource volume1 = newResource("vol-1", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume1, ResourceStatus.CREATED, 1L), group);
        CloudResource instance5 = newResource("instance-5", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance5, ResourceStatus.CREATED, 5L), group);
        CloudResource volume5 = newResource("vol-5", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume5, ResourceStatus.CREATED, 5L), group);
        CloudResource instance6 = newResource("instance-6", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(instance6, ResourceStatus.CREATED, 6L), group);
        CloudResource volume6 = newResource("vol-6", ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        allResources.put(new CloudResourceStatus(volume6, ResourceStatus.CREATED, 6L), group);

        RolledbackResourcesException rolledbackResourcesException = assertThrows(RolledbackResourcesException.class,
                () -> cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth,
                        scaleContext, resourceBuilderContext), failedResources, allResources, 6));

        assertEquals("Resources are rolled back because successful node count was lower than threshold. 3 nodes are failed. Error reason: failed instance",
                rolledbackResourcesException.getMessage());

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_CREATION_FAILED"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED), any());

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(),
                eq(false), eq(false));
        assertEquals(1,
                iterableArgumentCaptor.getAllValues().stream().flatMap(iterable -> ((Collection<CloudResource>) iterable).stream())
                        .filter(cloudResource -> "vol-2".equals(cloudResource.getName())).count());
    }

    private CloudResource newResource(String s, ResourceType resourceType, CommonStatus created) {
        return CloudResource.builder().withName(s).withType(resourceType).withStatus(created)
                .withParameters(new HashMap<>()).build();
    }

}
