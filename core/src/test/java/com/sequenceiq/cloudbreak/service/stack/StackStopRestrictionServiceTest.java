package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.model.AwsDiskType;

public class StackStopRestrictionServiceTest {

    private final StackStopRestrictionService underTest = new StackStopRestrictionService();

    private TemporaryStorage temporaryStorage = TemporaryStorage.ATTACHED_VOLUMES;

    @Test
    public void infrastructureShouldNotBeStoppableForEphemeralStorage() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable("AWS", groups);

        assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableIfTemporaryStorageIsEphemeralVolumes() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), TemporaryStorage.EPHEMERAL_VOLUMES));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable("AWS", groups);

        assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForMixedStorage() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value(), AwsDiskType.Gp2.value()), temporaryStorage));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable("AWS", groups);

        assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForNonAWSClusters() {
        StopRestrictionReason actual = underTest.isInfrastructureStoppable("GCP", null);
        assertEquals(StopRestrictionReason.NONE, actual);
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

        StopRestrictionReason actual = underTest.isInfrastructureStoppable("AWS", groups);

        assertEquals(StopRestrictionReason.NONE, actual);
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

}