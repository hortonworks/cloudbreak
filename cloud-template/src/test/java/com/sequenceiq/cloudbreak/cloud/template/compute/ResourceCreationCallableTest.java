package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ResourceCreationCallableTest {

    private static final long STACK_ID = 1L;

    @Mock
    private ResourceBuilders resourceBuilders;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private Group group;

    @Mock
    private ResourceBuilderContext context;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private CloudStack cloudStack;

    private CloudInstance cloudInstance;

    private ResourceCreationCallable underTest;

    @BeforeEach
    void setUp() {
        cloudInstance = cloudInstance();
        ResourceCreationCallablePayload payload = new ResourceCreationCallablePayload(
                List.of(cloudInstance), group, context, auth, cloudStack);
        underTest = new ResourceCreationCallable(payload, resourceBuilders, syncPollingScheduler, resourcePollTaskFactory, persistenceNotifier);
    }

    @Test
    void testResourceCreation() throws Exception {
        InMemoryStateStore.putStack(STACK_ID, PollGroup.POLLABLE);
        when(auth.getCloudContext()).thenReturn(CloudContext.Builder.builder().withVariant("AWS").withId(STACK_ID).build());
        ComputeResourceBuilder computeResourceBuilder1 = mock(ComputeResourceBuilder.class);
        CloudResource resource1 = cloudResource("resource1");
        when(computeResourceBuilder1.create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any()))
                .thenReturn(List.of(resource1));
        ComputeResourceBuilder computeResourceBuilder2 = mock(ComputeResourceBuilder.class);
        CloudResource resource2 = cloudResource("resource2");
        when(computeResourceBuilder2.create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any()))
                .thenReturn(List.of(resource2));
        when(resourceBuilders.compute(eq(Variant.variant("AWS")))).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(syncPollingScheduler.schedule(any(), anyInt(), anyInt(), anyInt())).thenReturn(List.of(new CloudResourceStatus(resource1, ResourceStatus.FAILED)));
        when(syncPollingScheduler.schedule(any(), anyInt(), anyInt(), anyInt())).thenReturn(List.of(new CloudResourceStatus(resource2, ResourceStatus.CREATED)));

        List<CloudResourceStatus> resourceStatuses = underTest.call();

        assertThat(resourceStatuses).hasSize(2);
        assertThat(resourceStatuses).allMatch(resourceStatus -> ResourceStatus.CREATED.equals(resourceStatus.getStatus()));
        verify(resourceBuilders, times(1)).compute(eq(Variant.variant("AWS")));
        verify(computeResourceBuilder1, times(1)).create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any());
        verify(computeResourceBuilder2, times(1)).create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any());
    }

    @Test
    void testResourceCreationWhenFirtsBuilderFails() throws Exception {
        InMemoryStateStore.putStack(STACK_ID, PollGroup.POLLABLE);
        when(auth.getCloudContext()).thenReturn(CloudContext.Builder.builder().withVariant("AWS").withId(STACK_ID).build());
        ComputeResourceBuilder computeResourceBuilder1 = mock(ComputeResourceBuilder.class);
        CloudResource resource1 = cloudResource("resource1");
        when(computeResourceBuilder1.create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any()))
                .thenReturn(List.of(resource1));
        ComputeResourceBuilder computeResourceBuilder2 = mock(ComputeResourceBuilder.class);
        when(resourceBuilders.compute(eq(Variant.variant("AWS")))).thenReturn(List.of(computeResourceBuilder1, computeResourceBuilder2));
        when(syncPollingScheduler.schedule(any(), anyInt(), anyInt(), anyInt())).thenReturn(List.of(new CloudResourceStatus(resource1, ResourceStatus.FAILED)));

        List<CloudResourceStatus> resourceStatuses = underTest.call();

        assertThat(resourceStatuses).hasSize(1);
        assertThat(resourceStatuses).extracting(CloudResourceStatus::getStatus).containsExactly(ResourceStatus.FAILED);
        verify(resourceBuilders, times(1)).compute(eq(Variant.variant("AWS")));
        verify(computeResourceBuilder1, times(1)).create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any());
        verify(computeResourceBuilder2, never()).create(eq(context), eq(cloudInstance), eq(STACK_ID), eq(auth), eq(group), any());
    }

    private CloudInstance cloudInstance() {
        return new CloudInstance("instanceId", instanceTemplate(), null, null, null);
    }

    private InstanceTemplate instanceTemplate() {
        return new InstanceTemplate(null, null, STACK_ID, List.of(), null, null, null, null, null, null);
    }

    private CloudResource cloudResource(String name) {
        return CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName(name).build();
    }
}