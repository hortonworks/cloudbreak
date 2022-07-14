package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDatabaseServiceTest {
    private static final String CLOUDPLATFORM = "cloudplatform";

    @Mock
    private CloudParameterCache cloudParameterCache;

    @InjectMocks
    private EmbeddedDatabaseService underTest;

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabled() {
        // GIVEN
        StackDto stack = createStack(1);
        Mockito.when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(true);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenNoDisksAttachedSupported() {
        // GIVEN
        StackDto stack = createStack(0);
        Mockito.when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(false);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenExternalDBUsed() {
        // GIVEN
        StackDto stack = createStack(0);
        when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.NON_HA);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenExternalDBCrnSet() {
        // GIVEN
        StackDto stack = createStack(0);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn("dbcrn");
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, cluster);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenEmbeddedDbOnRootDisk() {
        // GIVEN
        StackDto stack = createStack(0);
        when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.ON_ROOT_VOLUME);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreated() {
        // GIVEN
        StackDto stack = createStack(1);
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreatedWhenNoVolumeAttached() {
        // GIVEN
        StackDto stack = createStack(0);
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreatedWhenNoTemplate() {
        // GIVEN
        StackDto stack = createStackWithoutTemplate();
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreatedWhenDbOnAttachedDiskIsDisabled() {
        // GIVEN
        StackDto stack = createStack(1);
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertFalse(actualResult);
    }

    private StackDto createStack(int volumeCount) {
        StackDto stack = mock(StackDto.class);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        Template template = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        volumeTemplate.setUsageType(VolumeUsageType.DATABASE);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        masterGroup.setTemplate(template);
        lenient().when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(masterGroup, List.of(instanceMetaData))));
        lenient().when(stack.getCloudPlatform()).thenReturn(CLOUDPLATFORM);
        lenient().when(stack.getGatewayGroup()).thenReturn(Optional.of(masterGroup));
        return stack;
    }

    private StackDto createStackWithoutTemplate() {
        StackDto stack = mock(StackDto.class);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        lenient().when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(masterGroup, List.of(instanceMetaData))));
        return stack;
    }
}
