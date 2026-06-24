package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
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

    @Mock
    private PersistenceNotifier persistenceNotifier;

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

    @Test
    void forceDeleteOnTerminationForVolumeSetsCreatedDuringFailedOperation() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 1L);
        Group group = mock(Group.class);

        CloudResource failedInstance = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        CloudResource preservableVolume = newVolumeResource("vol-2", false);
        CloudResource alreadyDeletableVolume = newVolumeResource("vol-2-extra", true);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "failed instance", 2L), group);

        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(preservableVolume, ResourceStatus.CREATED, 2L), group);
        allResources.put(new CloudResourceStatus(alreadyDeletableVolume, ResourceStatus.CREATED, 2L), group);
        // a successful node so that the EXACT threshold is reached and only the failed node is rolled back
        allResources.put(new CloudResourceStatus(newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED), ResourceStatus.CREATED, 1L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 2);

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(), eq(false), eq(false));
        assertThat(iterableArgumentCaptor.getValue()).containsExactlyInAnyOrder(failedInstance, preservableVolume, alreadyDeletableVolume);
        assertThat(deleteOnTermination(preservableVolume)).isTrue();
        assertThat(deleteOnTermination(alreadyDeletableVolume)).isTrue();
        // only the volume whose flag was actually flipped is persisted; the already-deletable one is left untouched
        ArgumentCaptor<List<CloudResource>> persistedCaptor = ArgumentCaptor.forClass(List.class);
        verify(persistenceNotifier, times(1)).notifyUpdates(persistedCaptor.capture(), eq(auth.getCloudContext()));
        assertThat(persistedCaptor.getValue()).containsExactly(preservableVolume);
    }

    @Test
    void forceDeleteVolumeOnNonRepairUpscaleRegardlessOfDiscoveryFqdn() {
        // non-repair upscale: a failed node's volume set is deleted regardless of its discoveryFQDN, even when that FQDN is still a desired node.
        // There is no FQDN special-casing — outside a repair every rolled-back node is a new node that never came up, so its disk holds no data.
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 1L);
        Group group = groupWithTargetFqdn("host-2.example.com");

        CloudResource failedInstance = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        CloudResource failedNodeVolume = newVolumeResourceWithFqdn("vol-2", false, "host-2.example.com");

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "failed instance", 2L), group);

        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(failedNodeVolume, ResourceStatus.CREATED, 2L), group);
        allResources.put(new CloudResourceStatus(newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED), ResourceStatus.CREATED, 1L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 2);

        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth), any(), eq(false), eq(false));
        assertThat(deleteOnTermination(failedNodeVolume)).isTrue();
        verify(persistenceNotifier, times(1)).notifyUpdates(any(), eq(auth.getCloudContext()));
    }

    @Test
    void preserveAllVolumesDuringRepairRollback() {
        // repair=true: never force-delete, since the operation exists to reattach the preserved disk to the rebuilt node (preserve even on a failed rebuild)
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 1L, true);
        Group group = mock(Group.class);

        CloudResource failedInstance = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        CloudResource reattachVolume = newVolumeResource("vol-2", false);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "failed instance", 2L), group);

        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(reattachVolume, ResourceStatus.CREATED, 2L), group);
        allResources.put(new CloudResourceStatus(newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED), ResourceStatus.CREATED, 1L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 2);

        // the flag is left false, so the provider's preserve logic keeps the disk (DETACHED) for reattachment; nothing is persisted
        assertThat(deleteOnTermination(reattachVolume)).isFalse();
        verify(persistenceNotifier, never()).notifyUpdates(any(), any());
    }

    @Test
    void partialRollbackDoesNotTouchSurvivingNodeVolumeSet() {
        // EXACT threshold of 1 with 1 failed + 1 successful node => only the failed node is rolled back
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 1L);
        Group group = mock(Group.class);

        CloudResource failedInstance = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        CloudResource failedNodeVolume = newVolumeResource("vol-2", false);
        CloudResource survivingInstance = newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED);
        CloudResource survivingNodeVolume = newVolumeResource("vol-1", false);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "failed instance", 2L), group);

        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(failedNodeVolume, ResourceStatus.CREATED, 2L), group);
        allResources.put(new CloudResourceStatus(survivingInstance, ResourceStatus.CREATED, 1L), group);
        allResources.put(new CloudResourceStatus(survivingNodeVolume, ResourceStatus.CREATED, 1L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 2);

        ArgumentCaptor<Iterable> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(computeResourceService, times(1)).deleteResources(eq(resourceBuilderContext), eq(auth),
                iterableArgumentCaptor.capture(), eq(false), eq(false));
        // only the failed node's resources are rolled back; the surviving node is untouched
        assertThat(iterableArgumentCaptor.getValue()).containsExactlyInAnyOrder(failedInstance, failedNodeVolume);
        assertThat(deleteOnTermination(failedNodeVolume)).isTrue();
        assertThat(deleteOnTermination(survivingNodeVolume)).isFalse();
    }

    @Test
    void fireCleanupEventForResourcesDeletedDuringRollback() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 1L);
        Group group = mock(Group.class);

        CloudResource failedInstance = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);
        CloudResource volume = newVolumeResource("vol-2", false);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "failed instance", 2L), group);

        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(volume, ResourceStatus.CREATED, 2L), group);
        allResources.put(new CloudResourceStatus(newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED), ResourceStatus.CREATED, 1L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);

        when(computeResourceService.deleteResources(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(new CloudResourceStatus(failedInstance, ResourceStatus.DELETED),
                        new CloudResourceStatus(volume, ResourceStatus.DELETED)));

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 2);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_ROLLBACK_CLEANUP"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_ROLLBACK_CLEANUP), any());
    }

    @Test
    void doNotFireCleanupEventWhenNothingWasDeleted() {
        ScaleContext scaleContext = new ScaleContext(true, AdjustmentType.EXACT, 1L);
        Group group = mock(Group.class);

        CloudResource failedInstance = newResource("instance-2", ResourceType.AWS_INSTANCE, CommonStatus.FAILED);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "failed instance", 2L), group);

        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(newResource("instance-1", ResourceType.AWS_INSTANCE, CommonStatus.CREATED), ResourceStatus.CREATED, 1L), group);

        AuthenticatedContext auth = new AuthenticatedContext(mock(CloudContext.class), mock(CloudCredential.class));
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);

        when(computeResourceService.deleteResources(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "could not delete")));

        cloudFailureHandler.rollbackIfNecessary(new CloudFailureContext(auth, scaleContext, resourceBuilderContext), failedResources, allResources, 2);

        verify(cloudbreakEventService, never()).fireCloudbreakEvent(anyLong(), eq("CLOUD_PROVIDER_RESOURCE_ROLLBACK_CLEANUP"),
                eq(ResourceEvent.CLOUD_PROVIDER_RESOURCE_ROLLBACK_CLEANUP), any());
    }

    private CloudResource newResource(String s, ResourceType resourceType, CommonStatus created) {
        return CloudResource.builder().withName(s).withType(resourceType).withStatus(created)
                .withParameters(new HashMap<>()).build();
    }

    private CloudResource newVolumeResource(String name, boolean deleteOnTermination) {
        CloudResource resource = newResource(name, ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resource.putParameter(CloudResource.ATTRIBUTES,
                new VolumeSetAttributes("az", deleteOnTermination, "", "", List.of(), null));
        return resource;
    }

    private CloudResource newVolumeResourceWithFqdn(String name, boolean deleteOnTermination, String discoveryFqdn) {
        CloudResource resource = newResource(name, ResourceType.AWS_VOLUMESET, CommonStatus.CREATED);
        resource.putParameter(CloudResource.ATTRIBUTES,
                new VolumeSetAttributes("az", deleteOnTermination, "", "", List.of(), discoveryFqdn));
        return resource;
    }

    private Group groupWithTargetFqdn(String fqdn) {
        InstanceTemplate template = mock(InstanceTemplate.class);
        lenient().when(template.getPrivateId()).thenReturn(1L);
        CloudInstance instance = mock(CloudInstance.class);
        lenient().when(instance.getTemplate()).thenReturn(template);
        lenient().when(instance.getStringParameter(CloudInstance.FQDN)).thenReturn(fqdn);
        Group group = mock(Group.class);
        lenient().when(group.getInstances()).thenReturn(List.of(instance));
        return group;
    }

    private boolean deleteOnTermination(CloudResource resource) {
        return Boolean.TRUE.equals(resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDeleteOnTermination());
    }

}
