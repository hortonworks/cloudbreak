package com.sequenceiq.cloudbreak.cloud.template.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.BeforeEach;
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
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GroupResourceServiceTest {

    private static final String AVAILABILITY_ZONE = "us-west2-c";

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

    @InjectMocks
    private GroupResourceService underTest;

    @BeforeEach
    void setup() {
        Location location = mock(Location.class);
        AvailabilityZone availabilityZone = mock(AvailabilityZone.class);
        when(availabilityZone.value()).thenReturn(AVAILABILITY_ZONE);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(context.getLocation()).thenReturn(location);
        Variant variant = mock(Variant.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getVariant()).thenReturn(variant);
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(resourceBuilders.group(variant)).thenReturn(List.of(builder));
    }

    static Object [] [] dataForInstanceGroups() {
        return new Object[] [] {
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
