package com.sequenceiq.cloudbreak.service.diskupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator.LsblkFetcher;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class DiskInstanceInfoCollectorTest {

    @Mock
    private LsblkFetcher lsblkFetcher;

    @Mock
    private ResourceSyncUtil resourceSyncUtil;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private CloudConnectorHelper cloudConnectorHelper;

    @InjectMocks
    private DiskInstanceInfoCollector underTest;

    @Test
    void testGetInstanceResourceDtoAws() {
        VolumeRecord vol = new VolumeRecord("vol-1", "dev1", 100, "type1");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("inst1", List.of(vol));

        MultiValuedMap<String, InstanceResourceDto.VolumeDto> lsblkLines = new ArrayListValuedHashMap<>();
        lsblkLines.put("fqdn1", new InstanceResourceDto.VolumeDto(null, "nvme0n1", "/hadoopfs/", 100, "disk", "uuid", "vol1", null, null));

        InstanceResourceDto result = underTest.getInstanceResourceDto(lsblkLines.get("fqdn1"), cloudMetadata, "inst1", "AWS");

        assertNotNull(result);
        assertEquals(1, result.getVolumes().size());
        assertEquals("vol-1", result.getVolumes().get(0).volumeId());
    }

    @Test
    void testGetInstanceResourceDtoAzure() {
        VolumeRecord vol = new VolumeRecord("vol1", "sdc", 100, "type1");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("inst1", List.of(vol));

        MultiValuedMap<String, InstanceResourceDto.VolumeDto> lsblkLines = new ArrayListValuedHashMap<>();
        lsblkLines.put("host", new InstanceResourceDto.VolumeDto(null, "sdc", "/hadoopfs/", 100, "disk", "uuid", "sdc", null, null));

        InstanceResourceDto result = underTest.getInstanceResourceDto(lsblkLines.get("host"), cloudMetadata, "inst1", "AZURE");

        assertNotNull(result);
        assertEquals(1, result.getVolumes().size());
        assertEquals("sdc", result.getVolumes().get(0).deviceName());
    }

    @Test
    void testGetAndParseSaltInfo() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(1L);

        when(resourceSyncUtil.getFstabInformation(1L)).thenReturn(Map.of("fqdn1", "fstab content"));

        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getAllGatewayConfigs(stackDto)).thenReturn(List.of(gatewayConfig));
        Node node = mock(Node.class);
        when(node.getHostname()).thenReturn("fqdn1");
        when(stackUtil.collectNodes(stackDto)).thenReturn(Set.of(node));
        MultiValuedMap<String, InstanceResourceDto.VolumeDto> lsblkLines = new ArrayListValuedHashMap<>();
        lsblkLines.put("fqdn1", new InstanceResourceDto.VolumeDto(null, "nvme0n1", "/hadoopfs/", 100, "disk", "uuid", "vol1", null, null));

        when(lsblkFetcher.getLsblkResults(anyList(), anySet())).thenReturn(lsblkLines);

        Map<String, String> fqdnInstanceIdMap = Map.of("fqdn1", "inst1");
        Map<String, List<VolumeRecord>> cloudMetadata = Map.of("inst1", List.of(new VolumeRecord("vol-1", "d1", 100, "t1")));

        Map<String, InstanceResourceDto> result = underTest.getAndParseSaltInfo(stackDto, fqdnInstanceIdMap, cloudMetadata, "AWS");

        assertNotNull(result);
        assertTrue(result.containsKey("inst1"));
        assertEquals("fstab content", result.get("inst1").getFstab());
    }
}
