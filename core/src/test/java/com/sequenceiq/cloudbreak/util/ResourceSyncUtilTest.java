package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.DATABASE;
import static com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.GENERAL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_FSTAB_MISMATCH_FOUND;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_VOLUME_MISMATCH_FOUND;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskSyncMode;
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
    void testSyncFstabMismatch() {
        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setFstab("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0\nUUID=5678 /hadoopfs/fs2 ext4 defaults 0 0");
        VolumeSetAttributes volumeSetAttributeFromDB = new VolumeSetAttributes.Builder()
                .withFstab("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0")
                .build();

        underTest.syncFstab(instanceInfo, volumeSetAttributeFromDB, 1L, 2L, "inst1", "AVAILABLE", false);

        verify(eventService).fireCloudbreakEvent(eq(1L), eq("AVAILABLE"), eq(DISK_SYNC_FSTAB_MISMATCH_FOUND), anyList());
    }

    @Test
    void testSyncFstabMatch() {
        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setFstab("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0");
        VolumeSetAttributes volumeSetAttributeFromDB = new VolumeSetAttributes.Builder()
                .withFstab("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0")
                .build();

        underTest.syncFstab(instanceInfo, volumeSetAttributeFromDB, 1L, 2L, "inst1", "AVAILABLE", false);

        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_FSTAB_MISMATCH_FOUND), any());
    }

    @Test
    void testCreateFstabFromLsblk() {
        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> lsblkDisks = List.of(
            new InstanceResourceDto.VolumeDto("svol1", null, "/dbfs", 100, "stype1", "uuid1", "ser1", "hctl1", "ext4"),
            new InstanceResourceDto.VolumeDto("svol2", null, "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2", "xfs")
        );
        instanceInfo.setVolumes(lsblkDisks);

        String result = underTest.createFstabFromLsblk(instanceInfo);

        assertEquals("UUID=uuid1 /dbfs ext4 defaults,noatime,nofail 0 2\nUUID=uuid2 /hadoopfs/fs1 xfs defaults,noatime,nofail 0 2", result);
    }

    @Test
    void testCountHadoopMountsPerServer() {
        assertEquals(Collections.emptyMap(), underTest.countHadoopMountsPerServer(null));

        InstanceResourceDto instanceResourceDto1 = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> saltDisks1 = List.of(
            new InstanceResourceDto.VolumeDto("svol1", null, "/dbfs", 100, "stype1", "uuid1", "ser1", "hctl1", ""),
            new InstanceResourceDto.VolumeDto("svol2", null, "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2", "")
        );
        instanceResourceDto1.setVolumes(saltDisks1);
        InstanceResourceDto instanceResourceDto2 = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> saltDisks2 = List.of(
            new InstanceResourceDto.VolumeDto("svol2", null, "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2", "")
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
                new InstanceResourceDto.VolumeDto("svol1", "sdev1", "dbfs", 100, "stype1", "uuid1", "ser1", "hctl1", ""),
                new InstanceResourceDto.VolumeDto("svol2", "sdev2", "/hadoopfs/fs1", 200, "stype2", "uuid2", "ser2", "hctl2", "")
        );

        List<VolumeSetAttributes.Volume> result = underTest.syncResourceDisks(stack, databaseList, saltDisks, "inst1", 1L, false);

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

        underTest.updateResource(List.of(res), saltInfoMap, stack, DiskSyncMode.DRY_RUN, false);

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
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        underTest.updateResource(List.of(res), saltInfoMap, stack, DiskSyncMode.DRY_RUN, false);

        verify(resourceService, times(0)).saveAll(anyList());
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
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        underTest.updateResource(List.of(res), saltInfoMap, stack, DiskSyncMode.DRY_RUN, false);

        verify(resourceService, never()).saveAll(anyList());
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

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(1L), anyList())).thenReturn(Collections.emptyList());

        Map<String, String> result = underTest.getFstabInformation(1L);

        assertEquals("content", result.get("host1"));
        verify(resourceService).findAllByStackIdAndResourceTypeIn(eq(1L), anyList());
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

        underTest.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack, false);

        verify(eventService).fireCloudbreakEvent(eq(1L), eq("AVAILABLE"), eq(DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND), anyList());
    }

    @Test
    void testNormalizeFstabCollapsesWhitespace() {
        String raw = "UUID=a  \t   /hadoopfs/fs1   ext4  defaults  0 0";
        assertEquals("UUID=a /hadoopfs/fs1 ext4 defaults 0 0", underTest.normalizeFstab(raw));
    }

    @Test
    void testCreateFstabFromLsblkSkipsInvalidAndSystemMounts() {
        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> disks = List.of(
                new InstanceResourceDto.VolumeDto("skip-uuid", null, "/hadoopfs/fs1", 100, "pd-standard", null, "", "", "ext4"),
                new InstanceResourceDto.VolumeDto("skip-mp", "d", null, 100, "pd-standard", "u1", "", "", "ext4"),
                new InstanceResourceDto.VolumeDto("skip-root", null, "/", 100, "pd-standard", "u-root", "", "", "xfs"),
                new InstanceResourceDto.VolumeDto("skip-boot", null, "/boot", 100, "pd-standard", "u-boot", "", "", "ext2"),
                new InstanceResourceDto.VolumeDto("skip-efi", null, "/boot/efi", 100, "pd-standard", "u-efi", "", "", "vfat"),
                new InstanceResourceDto.VolumeDto("ok", null, "/data", 50, "pd-balanced", "u-data", "", "", "ext4")
        );
        instanceInfo.setVolumes(disks);
        assertEquals("UUID=u-data /data ext4 defaults,noatime,nofail 0 2", underTest.createFstabFromLsblk(instanceInfo));
    }

    @Test
    void testCountHadoopMountsPerServerNullVolumeList() {
        InstanceResourceDto dto = new InstanceResourceDto();
        dto.setVolumes(null);
        Map<String, Long> counts = underTest.countHadoopMountsPerServer(Map.of("i-1", dto));
        assertEquals(0L, counts.get("i-1"));
    }

    @Test
    void testGetMountedVolumesCountFiltersAndMatchesPrefixes() {
        assertEquals(0L, underTest.getMountedVolumesCount("# /hadoopfs/fs1 ext4\n"));
        assertEquals(0L, underTest.getMountedVolumesCount("onlyonecolumn"));
        assertEquals(1L, underTest.getMountedVolumesCount("UUID=x /dbfs/data ext4 defaults 0 0"));
        assertEquals(0L, underTest.getMountedVolumesCount("UUID=x /tmp ext4 defaults 0 0"));
        assertEquals(2L, underTest.getMountedVolumesCount("UUID=a /hadoopfs/fs1 ext4 defaults 0 0\r\nUUID=b /dbfs ext4 defaults 0 0"));
    }

    @Test
    void testSyncResourceDisksEqualSizesDifferentAttributesFiresMismatchEvent() {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        List<VolumeSetAttributes.Volume> databaseList = List.of(new VolumeSetAttributes.Volume("v1", "d1", 100, "pd-standard", GENERAL));
        List<InstanceResourceDto.VolumeDto> saltDisks = List.of(
                new InstanceResourceDto.VolumeDto("v1", "d1", "/hadoopfs/fs1", 200, "pd-standard", "u1", "", "", "ext4")
        );

        List<VolumeSetAttributes.Volume> result = underTest.syncResourceDisks(stack, databaseList, saltDisks, "i-1", 2L, false);

        assertEquals(1, result.size());
        assertEquals(200, result.get(0).getSize());
        verify(eventService).fireCloudbreakEvent(eq(1L), anyString(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), anyList());
    }

    @Test
    void testSyncResourceDisksEqualSizesNoMismatchEvent() {
        StackDto stack = mock(StackDto.class);
        List<VolumeSetAttributes.Volume> databaseList = List.of(new VolumeSetAttributes.Volume("1", "d1", 10, "t1", GENERAL));
        List<InstanceResourceDto.VolumeDto> saltDisks = List.of(
                new InstanceResourceDto.VolumeDto("1", "d1", "/hadoopfs/fs1", 10, "t1", "u1", "", "", "ext4")
        );
        assertTrue(underTest.syncResourceDisks(stack, databaseList, saltDisks, "i-1", 2L, false).isEmpty());
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), any());
    }

    @Test
    void testSyncResourceDisksNullDatabaseListReturnsEmpty() {
        StackDto stack = mock(StackDto.class);
        List<InstanceResourceDto.VolumeDto> saltDisks = List.of(
                new InstanceResourceDto.VolumeDto("1", "d1", "/hadoopfs/fs1", 10, "t1", "u1", "", "", "ext4")
        );
        assertTrue(underTest.syncResourceDisks(stack, null, saltDisks, "i-1", 2L, false).isEmpty());
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), any());
    }

    @Test
    void testSyncResourceDisksMapsDbfsMountToDatabaseUsageOnMismatch() {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(88L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);
        List<VolumeSetAttributes.Volume> databaseList = List.of(new VolumeSetAttributes.Volume("1", "d1", 10, "t1", GENERAL));
        List<InstanceResourceDto.VolumeDto> saltDisks = List.of(
                new InstanceResourceDto.VolumeDto("s1", "d1", "/my/dbfs/data", 10, "t1", "u1", "", "", "ext4"),
                new InstanceResourceDto.VolumeDto("s2", "d2", "/hadoopfs/fs1", 20, "t1", "u2", "", "", "ext4")
        );
        List<VolumeSetAttributes.Volume> result = underTest.syncResourceDisks(stack, databaseList, saltDisks, "i-1", 2L, false);
        assertEquals(2, result.size());
        assertEquals(DATABASE, result.get(0).getCloudVolumeUsageType());
        assertEquals(GENERAL, result.get(1).getCloudVolumeUsageType());
        verify(eventService).fireCloudbreakEvent(eq(88L), anyString(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), anyList());
    }

    @Test
    void testUpdateResourceEmptySaltMapDoesNotSync() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder().withFstab("fstab").withVolumes(new ArrayList<>()).build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));
        StackDto stack = mock(StackDto.class);

        underTest.updateResource(List.of(res), Collections.emptyMap(), stack, DiskSyncMode.PERSIST, false);

        verify(resourceAttributeUtil, never()).setTypedAttributes(any(), any());
        verify(resourceService, never()).saveAll(anyList());
    }

    @Test
    void testUpdateResourceNoSaltEntryForInstanceDoesNotSync() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder().withFstab("fstab").withVolumes(new ArrayList<>()).build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));
        InstanceResourceDto other = new InstanceResourceDto();
        other.setVolumes(Collections.emptyList());
        StackDto stack = mock(StackDto.class);

        underTest.updateResource(List.of(res), Map.of("other-instance", other), stack, DiskSyncMode.PERSIST, false);

        verify(resourceAttributeUtil, never()).setTypedAttributes(any(), any());
        verify(resourceService, never()).saveAll(anyList());
    }

    @Test
    void testUpdateResourceFstabMismatchOnlyPersistsWhenUpdateDatabaseTrue() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        when(res.getId()).thenReturn(99L);

        List<VolumeSetAttributes.Volume> dbVolumes = List.of(new VolumeSetAttributes.Volume("v1", "d1", 100, "pd-standard", GENERAL));
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder()
                .withFstab("UUID=a /hadoopfs/fs1 ext4 defaults 0 0\nUUID=b /hadoopfs/fs2 ext4 defaults 0 0")
                .withVolumes(new ArrayList<>(dbVolumes))
                .build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setVolumes(List.of(
                new InstanceResourceDto.VolumeDto("v1", "d1", "/hadoopfs/fs3", 100, "pd-standard", "uuid-1", "", "", "ext4")
        ));
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("inst1", instanceInfo);

        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        underTest.updateResource(List.of(res), saltInfoMap, stack, DiskSyncMode.PERSIST, false);

        verify(eventService).fireCloudbreakEvent(eq(1L), anyString(), eq(DISK_SYNC_FSTAB_MISMATCH_FOUND), anyList());
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), any());
        verify(resourceAttributeUtil).setTypedAttributes(eq(res), any(VolumeSetAttributes.class));
        verify(resourceService).saveAll(anyList());
    }

    @Test
    void testGetFstabInformationUsesEmptyStringWhenFstabKeyMissing() {
        Stack stack = mock(Stack.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stackService.getByIdWithLists(7L)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(mock(GatewayConfig.class));
        when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(Collections.emptySet());
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(7L), anyList())).thenReturn(Collections.emptyList());
        SaltConnector connector = mock(SaltConnector.class);
        when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(connector);
        when(saltOrchestrator.getFstabInformation(eq(connector), any(Target.class), any()))
                .thenReturn(Map.of("host1", Map.of("fstab", "line1"), "host2", Map.of("other", "x")));

        Map<String, String> result = underTest.getFstabInformation(7L);

        assertEquals("line1", result.get("host1"));
        assertEquals("", result.get("host2"));
    }

    @Test
    void testGetFstabInformationPropagatesSaltException() {
        Stack stack = mock(Stack.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stackService.getByIdWithLists(3L)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(mock(GatewayConfig.class));
        when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(Collections.emptySet());
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(3L), anyList())).thenReturn(Collections.emptyList());
        SaltConnector connector = mock(SaltConnector.class);
        when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(connector);
        when(saltOrchestrator.getFstabInformation(eq(connector), any(Target.class), any()))
                .thenThrow(new IllegalStateException("salt down"));

        assertThrows(IllegalStateException.class, () -> underTest.getFstabInformation(3L));
    }

    @Test
    void testCheckForUnmountedVolumesSkipsWhenSaltCountMatchesCloud() {
        Stack stack = mock(Stack.class);

        InstanceGroupDto igDto = mock(InstanceGroupDto.class);
        InstanceMetadataView imView = mock(InstanceMetadataView.class);
        when(imView.getInstanceGroupName()).thenReturn("compute");
        when(imView.getDiscoveryFQDN()).thenReturn("h1.example");
        when(igDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(imView));
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(igDto));

        InstanceResourceDto saltDto = new InstanceResourceDto();
        saltDto.setVolumes(List.of(new InstanceResourceDto.VolumeDto("v1", null, "/hadoopfs/fs1", 100, "t1", "u1", "", "", "")));
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("instance-42", saltDto);
        Map<String, String> fqdnInstanceIdMap = Map.of("h1.example", "instance-42");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("instance-42", List.of(new VolumeRecord("v1", "d1", 100, "t1")));

        underTest.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack, false);

        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND), any());
    }

    @Test
    void testUpdateResourceVolumeMismatchPersistsWhenUpdateDatabaseTrue() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        when(res.getId()).thenReturn(5L);

        List<VolumeSetAttributes.Volume> dbOne = List.of(new VolumeSetAttributes.Volume("v1", "d1", 10, "t1", GENERAL));
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder()
                .withFstab("UUID=a /hadoopfs/fs1 ext4 defaults 0 0")
                .withVolumes(new ArrayList<>(dbOne))
                .build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        List<InstanceResourceDto.VolumeDto> twoSalt = List.of(
                new InstanceResourceDto.VolumeDto("s1", "d1", "/hadoopfs/fs1", 10, "t1", "u1", "", "", "ext4"),
                new InstanceResourceDto.VolumeDto("s2", "d2", "/hadoopfs/fs2", 20, "t1", "u2", "", "", "ext4")
        );
        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setVolumes(twoSalt);

        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        underTest.updateResource(List.of(res), Map.of("inst1", instanceInfo), stack, DiskSyncMode.PERSIST, false);

        verify(eventService).fireCloudbreakEvent(eq(1L), anyString(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), anyList());
        verify(resourceAttributeUtil).setTypedAttributes(eq(res), any(VolumeSetAttributes.class));
        verify(resourceService).saveAll(anyList());
    }

    @Test
    void testSyncFstabMismatchSuppressedDoesNotFireButReturnsTrue() {
        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setFstab("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0\nUUID=5678 /hadoopfs/fs2 ext4 defaults 0 0");
        VolumeSetAttributes volumeSetAttributeFromDB = new VolumeSetAttributes.Builder()
                .withFstab("UUID=1234 /hadoopfs/fs1 ext4 defaults 0 0")
                .build();

        boolean result = underTest.syncFstab(instanceInfo, volumeSetAttributeFromDB, 1L, 2L, "inst1", "AVAILABLE", true);

        assertTrue(result);
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_FSTAB_MISMATCH_FOUND), any());
    }

    @Test
    void testSyncResourceDisksSuppressedDoesNotFireButReturnsSyncedVolumes() {
        StackDto stack = mock(StackDto.class);
        List<VolumeSetAttributes.Volume> databaseList = List.of(new VolumeSetAttributes.Volume("v1", "d1", 100, "pd-standard", GENERAL));
        List<InstanceResourceDto.VolumeDto> saltDisks = List.of(
                new InstanceResourceDto.VolumeDto("v1", "d1", "/hadoopfs/fs1", 200, "pd-standard", "u1", "", "", "ext4")
        );

        List<VolumeSetAttributes.Volume> result = underTest.syncResourceDisks(stack, databaseList, saltDisks, "i-1", 2L, true);

        assertEquals(1, result.size());
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MISMATCH_FOUND), any());
    }

    @Test
    void testCheckForUnmountedVolumesSuppressedDoesNotFireButReturnsTrue() {
        Stack stack = mock(Stack.class);

        InstanceGroupDto igDto = mock(InstanceGroupDto.class);
        InstanceMetadataView imView = mock(InstanceMetadataView.class);
        when(imView.getInstanceGroupName()).thenReturn("group1");
        when(imView.getDiscoveryFQDN()).thenReturn("fqdn1");
        when(igDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(imView));
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(igDto));

        Map<String, InstanceResourceDto> saltInfoMap = Map.of("fqdn1", new InstanceResourceDto());
        Map<String, String> fqdnInstanceIdMap = Map.of("fqdn1", "inst1");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("inst1", List.of(new VolumeRecord("v1", "d1", 100, "t1")));

        boolean result = underTest.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack, true);

        assertTrue(result);
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND), any());
    }

    @Test
    void testCheckForUnmountedVolumesReturnsFalseWhenSaltCountMatchesCloud() {
        Stack stack = mock(Stack.class);

        InstanceGroupDto igDto = mock(InstanceGroupDto.class);
        InstanceMetadataView imView = mock(InstanceMetadataView.class);
        when(imView.getInstanceGroupName()).thenReturn("compute");
        when(imView.getDiscoveryFQDN()).thenReturn("h1.example");
        when(igDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(imView));
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(igDto));

        InstanceResourceDto saltDto = new InstanceResourceDto();
        saltDto.setVolumes(List.of(new InstanceResourceDto.VolumeDto("v1", null, "/hadoopfs/fs1", 100, "t1", "u1", "", "", "")));
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("instance-42", saltDto);
        Map<String, String> fqdnInstanceIdMap = Map.of("h1.example", "instance-42");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("instance-42", List.of(new VolumeRecord("v1", "d1", 100, "t1")));

        boolean result = underTest.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack, false);

        assertFalse(result);
        verify(eventService, never()).fireCloudbreakEvent(any(), any(), eq(DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND), any());
    }

    @Test
    void testUpdateResourceReturnsTrueOnFstabMismatch() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");

        VolumeSetAttributes attr = new VolumeSetAttributes.Builder()
                .withFstab("UUID=a /hadoopfs/fs1 ext4 defaults 0 0\nUUID=b /hadoopfs/fs2 ext4 defaults 0 0")
                .withVolumes(new ArrayList<>())
                .build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        InstanceResourceDto instanceInfo = new InstanceResourceDto();
        instanceInfo.setVolumes(List.of(
                new InstanceResourceDto.VolumeDto("v1", "d1", "/hadoopfs/fs3", 100, "pd-standard", "uuid-1", "", "", "ext4")
        ));
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("inst1", instanceInfo);

        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        boolean result = underTest.updateResource(List.of(res), saltInfoMap, stack, DiskSyncMode.DRY_RUN, false);

        assertTrue(result);
    }

    @Test
    void testUpdateResourceReturnsFalseWhenNoMismatch() {
        Resource res = mock(Resource.class);
        when(res.getInstanceId()).thenReturn("inst1");
        VolumeSetAttributes attr = new VolumeSetAttributes.Builder().withFstab("fstab").withVolumes(new ArrayList<>()).build();
        when(resourceAttributeUtil.getTypedAttributes(eq(res), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(attr));

        InstanceResourceDto info = new InstanceResourceDto();
        info.setVolumes(new ArrayList<>());
        Map<String, InstanceResourceDto> saltInfoMap = Map.of("inst1", info);
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStatus()).thenReturn(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);

        boolean result = underTest.updateResource(List.of(res), saltInfoMap, stack, DiskSyncMode.DRY_RUN, false);

        assertFalse(result);
    }
}
