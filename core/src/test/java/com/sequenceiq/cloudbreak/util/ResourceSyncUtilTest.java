package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.GENERAL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_VOLUME_MISMATCH_FOUND;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltService;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ResourceSyncUtilTest {

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @Mock
    private StackService stackService;

    @Mock
    private SaltService saltService;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ResourceSyncUtil underTest;

    @Test
    void testNormalizeFstab() {
        assertEquals("", underTest.normalizeFstab(null));
        assertEquals("", underTest.normalizeFstab(""));
        assertEquals("", underTest.normalizeFstab("   \n  \n"));

        String fstab = "UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0\n" +
                "UUID=5678 /hadoopfs/fs2 ext4 defaults 0 0\n" +
                "UUID=5678 /hadoopfs/fs2 ext4 defaults 0 0\n" +
                "\n";
        String normalizedFstab = underTest.normalizeFstab(fstab);
        assertEquals("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0\n" +
                "UUID=5678 /hadoopfs/fs2 ext4 defaults 0 0", normalizedFstab);
    }

    @Test
    void testCountHadoopMountsPerServer() {
        assertEquals(Collections.emptyMap(), underTest.countHadoopMountsPerServer(null));

        InstanceResourceDto instanceResourceDto1 = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> saltDisks1 = List.of(
            new InstanceResourceDto.VolumeDto("svol1", null, "/dbfs", 100, "stype1", "uuid1", "ser1", "hctl1"),
            new InstanceResourceDto.VolumeDto("svol2", null, "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2")
        );
        instanceResourceDto1.setVolumes(saltDisks1);
        InstanceResourceDto instanceResourceDto2 = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> saltDisks2 = List.of(
            new InstanceResourceDto.VolumeDto("svol2", null, "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2")
        );
        instanceResourceDto2.setVolumes(saltDisks2);

        Map<String, InstanceResourceDto> fstabMap = Map.of(
                "host1", instanceResourceDto1,
                "host2", instanceResourceDto2
        );
        Map<String, Long> result = underTest.countHadoopMountsPerServer(fstabMap);
        assertEquals(2, result.get("host1"));
        assertEquals(1, result.get("host2"));
    }

    @Test
    void testGetMountedVolumesCountWithEmpty() {
        assertEquals(0L, underTest.getMountedVolumesCount(null));
        assertEquals(0L, underTest.getMountedVolumesCount(""));
    }

    @Test
    void testSyncResourceDisks() {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        List<VolumeSetAttributes.Volume> databaseList = new ArrayList<>();
        databaseList.add(new VolumeSetAttributes.Volume("vol1", "dev1", 10, "type1", GENERAL));

        List<InstanceResourceDto.VolumeDto> saltDisks = List.of(
                new InstanceResourceDto.VolumeDto("svol1", "sdev1", "dbfs", 100, "stype1", "uuid1", "ser1", "hctl1"),
                new InstanceResourceDto.VolumeDto("svol2", "sdev2", "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2")
        );

        List<VolumeSetAttributes.Volume> result = underTest.syncResourceDisks(stack, databaseList, saltDisks, "inst1", 1L);

        assertEquals(2, result.size());
        verify(eventService).fireCloudbreakEvent(eq(1L), anyString(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), anyList());
    }

    @Test
    void testUpdateResourceMismatched() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");

        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("v1", "d1", 10, "t1", GENERAL));

        VolumeSetAttributes attr = new VolumeSetAttributes.Builder()
                .withAvailabilityZone("az")
                .withDeleteOnTermination(true)
                .withFstab("fstab")
                .withUuids("uuids")
                .withVolumes(volumes)
                .withDiscoveryFQDN("fqdn")
                .build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setVolumes(Collections.emptyList());
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("inst1", instanceInfo);

        boolean result = underTest.updateResource(res, saltInfoMap, stack);

        assertTrue(result);
        assertEquals("fstab", attr.getFstab());
    }

    @Test
    void testUpdateResourceMatches() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder().withFstab("fstab").withVolumes(new ArrayList<>()).build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        InstanceResourceDto info = new InstanceResourceDto();
        info.setVolumes(new ArrayList<>());
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("inst1", info);

        boolean result = underTest.updateResource(res, saltInfoMap, null);

        assertTrue(result);
    }

    @Test
    void testUpdateResourceAndFstabWithFqdnMap() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder().withFstab("fstab").withVolumes(new ArrayList<>()).build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        InstanceResourceDto info = new InstanceResourceDto();
        info.setVolumes(Collections.emptyList());
        Map<String, InstanceResourceDto> saltInfoMap = new HashMap<>();
        saltInfoMap.put("fqdn1", info);
        saltInfoMap.put("inst1", info);

        boolean result = underTest.updateResource(res, saltInfoMap, null);

        assertTrue(result);
    }

    @Test
    void testGetFstabInformation() {
        Stack stack = mock(Stack.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stackService.getByIdWithLists(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        Node node = new Node("1.1.1.1", "2.2.2.2", "inst1", "type", "host1", "group");
        when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(Set.of(node));

        SaltConnector connector = mock(SaltConnector.class);
        when(saltService.createSaltConnector(gatewayConfig)).thenReturn(connector);

        Map<String, Map<String, String>> saltFstabInfo = Map.of("host1", Map.of("fstab", "content"));
        when(saltOrchestrator.getFstabInformation(eq(connector), any(Target.class), any())).thenReturn(saltFstabInfo);

        Map<String, String> result = underTest.getFstabInformation(1L);

        assertEquals("content", result.get("host1"));
    }

    @Test
    void testGetFstabInformationThrowsException() {
        Stack stack = mock(Stack.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stackService.getByIdWithLists(1L)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> underTest.getFstabInformation(1L));
    }

    @Test
    void testCheckForUnmountedVolumes() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getDetailedStatus()).thenReturn(DetailedStackStatus.AVAILABLE);

        InstanceGroupDto igDto = mock(InstanceGroupDto.class);
        InstanceMetadataView imView = mock(InstanceMetadataView.class);
        when(imView.getInstanceGroupName()).thenReturn("group1");
        when(imView.getDiscoveryFQDN()).thenReturn("fqdn1");
        when(igDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(imView));
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(igDto));

        Map<String, InstanceResourceDto> saltInfoMap = Map.of("fqdn1", new InstanceResourceDto());
        Map<String, String> fqdnInstanceIdMap = Map.of("fqdn1", "inst1");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("inst1", List.of(new VolumeRecord("v1", "d1", 100, "t1")));

        underTest.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack);

        verify(eventService).fireCloudbreakEvent(eq(1L), eq("AVAILABLE"), eq(DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND), anyList());
    }
}
