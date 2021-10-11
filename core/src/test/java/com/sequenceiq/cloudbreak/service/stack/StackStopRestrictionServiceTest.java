package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
public class StackStopRestrictionServiceTest {

    private final TemporaryStorage temporaryStorage = TemporaryStorage.ATTACHED_VOLUMES;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @InjectMocks
    private StackStopRestrictionService underTest;

    @Test
    public void infrastructureShouldNotBeStoppableForEphemeralStorageBefore248CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-XXb"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForEphemeralStorageAfter248CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-XXb"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldNotBeStoppableIfTemporaryStorageIsEphemeralVolumesBefore248CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of("ebs"), TemporaryStorage.EPHEMERAL_VOLUMES));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-bXX"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUME_CACHING, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableIfTemporaryStorageIsEphemeralVolumesAfter248CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of("ebs"), TemporaryStorage.EPHEMERAL_VOLUMES));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-bXX"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForMixedStorage() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value(), AwsDiskType.Gp2.value()), temporaryStorage));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, underTest.isInfrastructureStoppable(createStack("AWS", groups)));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.NONE, underTest.isInfrastructureStoppable(createStack("AWS", groups)));
    }

    @Test
    public void infrastructureShouldBeStoppableForNonAWSClusters() {
        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("GCP", null));
        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForValidInstanceGroups() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        InstanceGroup master = createGroup(List.of("ebs"), temporaryStorage);
        master.getTemplate().setAttributes(
                new JsonToString().convertToEntityAttribute(
                        "{\"sshLocation\":\"0.0.0.0/0\",\"encrypted\":false}"));
        groups.add(master);

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.NONE, underTest.isInfrastructureStoppable(createStack("AWS", groups)));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.NONE, underTest.isInfrastructureStoppable(createStack("AWS", groups)));
    }

    private InstanceGroup createGroup(List<String> volumeTypes, TemporaryStorage temporaryStorage) {
        InstanceGroup group = new InstanceGroup();
        Template template = new Template();
        template.setTemporaryStorage(temporaryStorage);
        group.setTemplate(template);
        template.setVolumeTemplates(Sets.newHashSet());
        for (String volumeType: volumeTypes) {
            VolumeTemplate volumeTemplate = new VolumeTemplate();
            volumeTemplate.setVolumeType(volumeType);
            template.getVolumeTemplates().add(volumeTemplate);
        }
        return group;
    }

    private Stack createStack(String cloudProvider, Set<InstanceGroup> groups) {
        Stack stack = new Stack();
        stack.setCloudPlatform(cloudProvider);
        stack.setId(42L);
        stack.setInstanceGroups(groups);
        return stack;
    }

}