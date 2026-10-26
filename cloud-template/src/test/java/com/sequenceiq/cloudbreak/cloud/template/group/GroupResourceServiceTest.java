package com.sequenceiq.cloudbreak.cloud.template.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GroupResourceServiceTest {

    private static final String AVAILABILITY_ZONE = "us-west2-c";

    private static final String AVAILABILITY_ZONE_A = "us-west2-a";

    private static final String AVAILABILITY_ZONE_B = "us-west2-b";

    private static final String MASTER = "master";

    private static final Long STACK_ID = 111L;

    @Mock
    private ResourceBuilders resourceBuilders;

    @Mock
    private ResourceBuilderContext context;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private Network network;

    @Mock
    private GroupResourceBuilder<ResourceBuilderContext> builder;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private ResourcePollTaskFactory statusCheckFactory;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @InjectMocks
    private GroupResourceService underTest;

    @AfterEach
    void tearDown() {
        InMemoryStateStore.deleteStack(STACK_ID);
    }

    @BeforeEach
    void setup() throws Exception {
        Location location = mock(Location.class);
        AvailabilityZone availabilityZone = mock(AvailabilityZone.class);
        when(availabilityZone.value()).thenReturn(AVAILABILITY_ZONE);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(context.getLocation()).thenReturn(location);
        Variant variant = mock(Variant.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getVariant()).thenReturn(variant);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(resourceBuilders.group(variant)).thenReturn(List.of(builder));
        when(syncPollingScheduler.schedule(any())).thenReturn(List.of());
        when(builder.build(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(5));
    }

    static Object[][] dataForInstanceGroups() {
        return new Object[][]{
                {Map.of("group1", 1, "group2", 3)},
                {Map.of("group1", 0, "group2", 2)},
                {Map.of("group1", 0, "group2", 0)},
                {Map.of()}
        };
    }

    @ParameterizedTest(name = "testBuildResourcesForZonal{index}")
    @MethodSource("dataForInstanceGroups")
    void testBuildResourcesForZonal(Map<String, Integer> groupZones) throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        List<Group> groups = getGroups(groupZones);
        underTest.buildResources(context, auth, groups, network, null);
        for (Map.Entry<String, Integer> entry : groupZones.entrySet()) {
            ArgumentCaptor<Group> argumentCaptor = ArgumentCaptor.forClass(Group.class);
            if (entry.getValue() > 0) {
                IntStream.range(0, entry.getValue()).forEach(value -> {
                    verify(builder).create(eq(context), eq(auth), argumentCaptor.capture(), eq(network),
                            eq(String.format("us-west2-%s-%s", entry.getKey(), value)));
                    assertEquals(entry.getKey(), argumentCaptor.getValue().getName());
                });
            }
        }
        Set<String> groupWithNoZones = groupZones.entrySet().stream().filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(groupWithNoZones)) {
            ArgumentCaptor<Group> argumentCaptor = ArgumentCaptor.forClass(Group.class);
            verify(builder, times(groupWithNoZones.size())).create(eq(context), eq(auth), argumentCaptor.capture(), eq(network), eq(AVAILABILITY_ZONE));
            assertEquals(groupWithNoZones, argumentCaptor.getAllValues().stream().map(group -> group.getName()).collect(Collectors.toSet()));
        } else {
            verify(builder, never()).create(any(), any(), any(), any(), eq(AVAILABILITY_ZONE));
        }
    }

    @ParameterizedTest(name = "testBuildResourcesForZonal{index}")
    @MethodSource("dataForInstanceGroups")
    void testBuildResourcesForNonZonal(Map<String, Integer> groupZones) throws Exception {
        when(builder.isZonalResource()).thenReturn(false);
        List<Group> groups = getGroups(groupZones);
        underTest.buildResources(context, auth, groups, network, null);
        ArgumentCaptor<Group> argumentCaptor = ArgumentCaptor.forClass(Group.class);
        verify(builder, times(groupZones.size())).create(eq(context), eq(auth), argumentCaptor.capture(), eq(network), eq(AVAILABILITY_ZONE));
        assertEquals(groupZones.keySet(), argumentCaptor.getAllValues().stream().map(group -> group.getName()).collect(Collectors.toSet()));
    }

    static Object[][] legacyInstanceGroupNames() {
        return new Object[][]{
                {"stackname-master-111-c"},
                {"stackname-master-111"},
                {"stackname-master"}
        };
    }

    @ParameterizedTest(name = "testBuildMissingZonalResourcesOnlyBuildsNewZones with existing name {0}")
    @MethodSource("legacyInstanceGroupNames")
    void testBuildMissingZonalResourcesOnlyBuildsNewZones(String existingName) throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE_B, AVAILABILITY_ZONE));
        List<CloudResource> existingResources = List.of(instanceGroupResource(existingName, MASTER, AVAILABILITY_ZONE));

        underTest.buildMissingZonalResources(context, auth, List.of(group), network, existingResources);

        verify(builder).create(context, auth, group, network, AVAILABILITY_ZONE_A);
        verify(builder).create(context, auth, group, network, AVAILABILITY_ZONE_B);
        verify(builder, never()).create(any(), any(), any(), any(), eq(AVAILABILITY_ZONE));
    }

    @Test
    void testBuildMissingZonalResourcesTreatsResourceWithoutAvailabilityZoneAsStackZone() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));
        CloudResource legacyResource = instanceGroupResource("stackname-master", MASTER, null);

        underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(legacyResource));

        verify(builder).create(context, auth, group, network, AVAILABILITY_ZONE_A);
        verify(builder, never()).create(any(), any(), any(), any(), eq(AVAILABILITY_ZONE));
        verify(builder, never()).build(any(), any(), any(), any(), any(), eq(legacyResource));
    }

    @Test
    void testBuildMissingZonalResourcesTreatsResourceWithoutGroupAsCoveringTheZone() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));
        List<CloudResource> existingResources = List.of(instanceGroupResource("stackname-master", null, AVAILABILITY_ZONE));

        underTest.buildMissingZonalResources(context, auth, List.of(group), network, existingResources);

        verify(builder).create(context, auth, group, network, AVAILABILITY_ZONE_A);
        verify(builder, never()).create(any(), any(), any(), any(), eq(AVAILABILITY_ZONE));
    }

    @Test
    void testBuildMissingZonalResourcesIgnoresResourcesOfOtherGroupsAndTypes() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A));
        CloudResource otherGroupResource = instanceGroupResource("stackname-worker-111-a", "worker", AVAILABILITY_ZONE_A);
        CloudResource otherTypeResource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("stackname-m-0")
                .withGroup(MASTER)
                .withAvailabilityZone(AVAILABILITY_ZONE_A)
                .build();

        underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(otherGroupResource, otherTypeResource));

        verify(builder).create(context, auth, group, network, AVAILABILITY_ZONE_A);
    }

    @Test
    void testBuildMissingZonalResourcesRebuildsExistingResourcesWhenAllZonesAreCovered() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));
        CloudResource resourceInZoneA = instanceGroupResource("stackname-master-111-a", MASTER, AVAILABILITY_ZONE_A);
        CloudResource resourceInStackZone = instanceGroupResource("stackname-master-111", MASTER, AVAILABILITY_ZONE);
        when(builder.build(eq(context), eq(auth), eq(group), eq(network), any(), eq(resourceInZoneA))).thenReturn(resourceInZoneA);
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(builder, auth, List.of(resourceInZoneA), context, true)).thenReturn(pollTask);
        CloudResourceStatus rebuiltStatus = new CloudResourceStatus(resourceInZoneA, ResourceStatus.CREATED);
        when(syncPollingScheduler.schedule(pollTask)).thenReturn(List.of(rebuiltStatus));

        List<CloudResourceStatus> result = underTest.buildMissingZonalResources(context, auth, List.of(group), network,
                List.of(resourceInZoneA, resourceInStackZone));

        assertTrue(result.contains(rebuiltStatus));
        verify(builder, never()).create(any(), any(), any(), any(), any());
        verify(builder).build(eq(context), eq(auth), eq(group), eq(network), any(), eq(resourceInStackZone));
        verify(resourceNotifier, never()).notifyAllocation(any(), any());
        verify(resourceNotifier).notifyUpdate(eq(resourceInZoneA), any());
        verify(context).addGroupResources(MASTER, List.of(resourceInZoneA));
    }

    @Test
    void testBuildMissingZonalResourcesSkipsNonZonalBuilders() throws Exception {
        when(builder.isZonalResource()).thenReturn(false);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));

        List<CloudResourceStatus> result = underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of());

        assertTrue(result.isEmpty());
        verify(builder, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void testBuildMissingZonalResourcesPersistsAndRegistersBuiltResource() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A));
        CloudResource newResource = instanceGroupResource("stackname-master-111-a", null, AVAILABILITY_ZONE_A);
        CloudResource builtResource = instanceGroupResource("stackname-master-111-a", MASTER, AVAILABILITY_ZONE_A);
        when(builder.create(context, auth, group, network, AVAILABILITY_ZONE_A)).thenReturn(newResource);
        when(builder.build(eq(context), eq(auth), eq(group), eq(network), any(), any())).thenReturn(builtResource);
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(builder, auth, List.of(builtResource), context, true)).thenReturn(pollTask);
        CloudResourceStatus builtStatus = new CloudResourceStatus(builtResource, ResourceStatus.CREATED);
        when(syncPollingScheduler.schedule(pollTask)).thenReturn(List.of(builtStatus));

        List<CloudResourceStatus> result = underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(otherGroupResource()));

        assertEquals(List.of(builtStatus), result);
        ArgumentCaptor<CloudResource> allocatedResource = ArgumentCaptor.forClass(CloudResource.class);
        verify(resourceNotifier).notifyAllocation(allocatedResource.capture(), any());
        assertEquals(MASTER, allocatedResource.getValue().getGroup());
        verify(resourceNotifier).notifyUpdate(eq(builtResource), any());
        verify(context).addGroupResources(MASTER, List.of(builtResource));
    }

    @Test
    void testBuildMissingZonalResourcesTreatsRequestedResourceAsCoveringTheZone() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));
        // successfully built group resources stay REQUESTED in the DB, so the status can't tell a failed build apart
        CloudResource requestedResource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.REQUESTED)
                .withName("stackname-master-111-c")
                .withGroup(MASTER)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(requestedResource));

        verify(builder).create(context, auth, group, network, AVAILABILITY_ZONE_A);
        verify(builder, never()).create(any(), any(), any(), any(), eq(AVAILABILITY_ZONE));
        // building it again recreates it on the provider if a failed build left only the row behind
        verify(builder).build(eq(context), eq(auth), eq(group), eq(network), any(), eq(requestedResource));
    }

    @Test
    void testBuildMissingZonalResourcesSkipsStackWithoutAnyResourceOfTheZonalType() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));
        // stacks provisioned before GCP instance groups were introduced have none of them, so there is nothing to extend
        CloudResource otherTypeResource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("stackname-m-0")
                .withGroup(MASTER)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        List<CloudResourceStatus> result = underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(otherTypeResource));

        assertTrue(result.isEmpty());
        verify(builder, never()).create(any(), any(), any(), any(), any());
        verify(builder, never()).build(any(), any(), any(), any(), any(), any());
        verify(resourceNotifier, never()).notifyAllocation(any(), any());
    }

    @Test
    void testBuildMissingZonalResourcesSkipsCancelledStack() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        InMemoryStateStore.putStack(STACK_ID, PollGroup.CANCELLED);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A, AVAILABILITY_ZONE));

        List<CloudResourceStatus> result = underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(otherGroupResource()));

        assertTrue(result.isEmpty());
        verify(builder, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void testBuildMissingZonalResourcesKeepsResourceForRebuildWhenBuildFails() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        when(builder.resourceType()).thenReturn(ResourceType.GCP_INSTANCE_GROUP);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE_A));
        CloudResource newResource = instanceGroupResource("stackname-master-111-a", null, AVAILABILITY_ZONE_A);
        when(builder.create(context, auth, group, network, AVAILABILITY_ZONE_A)).thenReturn(newResource);
        IllegalStateException buildFailure = new IllegalStateException("insert failed");
        when(builder.build(eq(context), eq(auth), eq(group), eq(network), any(), any())).thenThrow(buildFailure);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> underTest.buildMissingZonalResources(context, auth, List.of(group), network, List.of(otherGroupResource())));

        assertEquals(buildFailure, exception);
        verify(resourceNotifier).notifyAllocation(any(), any());
        verify(builder, never()).delete(any(), any(), any(), any());
        verify(resourceNotifier, never()).notifyDeletion(any(), any());
        verify(context, never()).addGroupResources(any(), any());
    }

    @Test
    void testBuildResourcesDoesNotRollBackWhenBuildFails() throws Exception {
        when(builder.isZonalResource()).thenReturn(true);
        Group group = group(MASTER, Set.of(AVAILABILITY_ZONE));
        CloudResource newResource = instanceGroupResource("stackname-master-111-c", null, AVAILABILITY_ZONE);
        when(builder.create(context, auth, group, network, AVAILABILITY_ZONE)).thenReturn(newResource);
        when(builder.build(eq(context), eq(auth), eq(group), eq(network), any(), any())).thenThrow(new IllegalStateException("insert failed"));

        assertThrows(IllegalStateException.class, () -> underTest.buildResources(context, auth, List.of(group), network, null));

        verify(builder, never()).delete(any(), any(), any(), any());
        verify(resourceNotifier, never()).notifyDeletion(any(), any());
    }

    private Group group(String name, Set<String> availabilityZones) {
        Group group = mock(Group.class);
        GroupNetwork groupNetwork = mock(GroupNetwork.class);
        when(groupNetwork.getAvailabilityZones()).thenReturn(availabilityZones);
        when(group.getNetwork()).thenReturn(groupNetwork);
        when(group.getName()).thenReturn(name);
        return group;
    }

    private CloudResource otherGroupResource() {
        return instanceGroupResource("stackname-worker-111-c", "worker", AVAILABILITY_ZONE);
    }

    private CloudResource instanceGroupResource(String name, String group, String availabilityZone) {
        return CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName(name)
                .withGroup(group)
                .withAvailabilityZone(availabilityZone)
                .build();
    }

    private List<Group> getGroups(Map<String, Integer> groupZones) {
        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : groupZones.entrySet()) {
            Group group = mock(Group.class);
            GroupNetwork groupNetwork = mock(GroupNetwork.class);
            when(groupNetwork.getAvailabilityZones()).thenReturn(IntStream.range(0, entry.getValue())
                    .mapToObj(value -> String.format("us-west2-%s-%s", entry.getKey(), value)).collect(Collectors.toSet()));
            when(group.getNetwork()).thenReturn(groupNetwork);
            when(group.getName()).thenReturn(entry.getKey());
            groups.add(group);
        }
        return groups;
    }
}
