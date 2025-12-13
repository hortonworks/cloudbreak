package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType.HDD;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType.LOCAL_SSD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupEphemeralVolumeChecker;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.AwsDiskType;

public class InstanceGroupEphemeralVolumeCheckerTest {

    private InstanceGroupEphemeralVolumeChecker underTest;

    @BeforeEach
    public void setUp() {
        underTest = new InstanceGroupEphemeralVolumeChecker();
    }

    @Test
    public void testIsInstanceGroupEphemeralVolumesOnly() {
        InstanceGroup ig = createGroup(List.of(Pair.of("ebs", VolumeUsageType.GENERAL)),
                InstanceGroupType.CORE, "worker");
        assertFalse(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(AwsDiskType.Ephemeral.value(), VolumeUsageType.GENERAL),
                        Pair.of(AwsDiskType.Gp2.value(), VolumeUsageType.GENERAL)),
                InstanceGroupType.CORE, "worker");
        assertFalse(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(AwsDiskType.Ephemeral.value(), VolumeUsageType.GENERAL)),
                InstanceGroupType.CORE, "worker");
        assertTrue(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(AwsDiskType.Ephemeral.value(), VolumeUsageType.GENERAL),
                        Pair.of(AwsDiskType.Gp2.value(), VolumeUsageType.DATABASE)),
                InstanceGroupType.GATEWAY, "gateway");
        assertTrue(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(AwsDiskType.Ephemeral.value(), VolumeUsageType.GENERAL),
                        Pair.of(AwsDiskType.Gp2.value(), VolumeUsageType.DATABASE),
                        Pair.of(AwsDiskType.Gp2.value(), VolumeUsageType.GENERAL)),
                InstanceGroupType.GATEWAY, "gateway");
        assertFalse(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));
    }

    @Test
    public void testIsInstanceGroupLocalSSDVolumesOnly() {
        InstanceGroup ig = createGroup(List.of(Pair.of(LOCAL_SSD.value(), VolumeUsageType.GENERAL),
                        Pair.of(HDD.value(), VolumeUsageType.GENERAL)),
                InstanceGroupType.CORE, "worker");
        assertFalse(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(LOCAL_SSD.value(), VolumeUsageType.GENERAL)),
                InstanceGroupType.CORE, "worker");
        assertTrue(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(LOCAL_SSD.value(), VolumeUsageType.GENERAL),
                        Pair.of(HDD.value(), VolumeUsageType.DATABASE)),
                InstanceGroupType.GATEWAY, "gateway");
        assertTrue(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));

        ig = createGroup(List.of(Pair.of(LOCAL_SSD.value(), VolumeUsageType.GENERAL),
                        Pair.of(HDD.value(), VolumeUsageType.DATABASE),
                        Pair.of(HDD.value(), VolumeUsageType.GENERAL)),
                InstanceGroupType.GATEWAY, "gateway");
        assertFalse(underTest.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(ig));
    }

    private InstanceGroup createGroup(List<Pair<String, VolumeUsageType>> volumeTypes, InstanceGroupType groupType, String groupName) {
        InstanceGroup group = new InstanceGroup();
        group.setInstanceGroupType(groupType);
        group.setGroupName(groupName);
        Template template = new Template();
        group.setTemplate(template);
        template.setVolumeTemplates(Sets.newHashSet());
        for (Pair<String, VolumeUsageType> volumeType: volumeTypes) {
            VolumeTemplate volumeTemplate = new VolumeTemplate();
            volumeTemplate.setVolumeType(volumeType.getLeft());
            volumeTemplate.setUsageType(volumeType.getRight());
            template.getVolumeTemplates().add(volumeTemplate);
        }
        return group;
    }
}
