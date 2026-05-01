package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskType.LOCAL_SSD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.VolumeInfo;

@ExtendWith(MockitoExtension.class)
class GcpResourceVolumeConnectorTest {
    @InjectMocks
    private GcpResourceVolumeConnector underTest;

    @Test
    void testGetVolumeDeviceMappingByInstance() {
        Map<String, Map<String, String>> result = underTest.getVolumeDeviceMappingByInstance(null, null, createDiskResources());
        assertEquals(Map.ofEntries(
                Map.entry("i1v1", "/dev/disk/by-id/google-local-nvme-ssd-0"),
                Map.entry("i1v2", "/dev/disk/by-id/google-local-nvme-ssd-1"),
                Map.entry("i1v3", "/dev/disk/by-id/google-local-nvme-ssd-2")), result.get("instance1"));
        assertEquals(Map.ofEntries(
                Map.entry("i2v1", "/dev/disk/by-id/google-i2v1"),
                Map.entry("i2v2", "/dev/disk/by-id/google-i2v2"),
                Map.entry("i2v3", "/dev/disk/by-id/google-i2v3"),
                Map.entry("i2v4", "/dev/disk/by-id/google-local-nvme-ssd-0")), result.get("instance2"));
        assertTrue(result.get("instance3").isEmpty());
    }

    private List<CloudResource> createDiskResources() {
        List<CloudResource> resources = new ArrayList<>();
        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/sdc", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v2 = new VolumeSetAttributes.Volume("i1v2", "/dev/sdd", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v3 = new VolumeSetAttributes.Volume("i1v3", "/dev/sde", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudResource volumeSetResource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1, i1v2, i1v3)));

        VolumeSetAttributes.Volume i2v1 = new VolumeSetAttributes.Volume("i2v1", "/dev/sdc", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v2 = new VolumeSetAttributes.Volume("i2v2", "/dev/sdd", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v3 = new VolumeSetAttributes.Volume("i2v3", "/dev/sde", 10, "HDD", CloudVolumeUsageType.DATABASE);
        VolumeSetAttributes.Volume i2v4 = new VolumeSetAttributes.Volume("i2v4", "/dev/sdf", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudResource volumeSetResource2 = createVolumeSetResource("instance2", createVolumeSetAttributes(List.of(i2v1, i2v2, i2v3, i2v4)));

        CloudResource volumeSetResource3 = createVolumeSetResource("instance3", createVolumeSetAttributes(List.of()));

        resources.add(volumeSetResource1);
        resources.add(volumeSetResource2);
        resources.add(volumeSetResource3);
        return resources;
    }

    private CloudResource createVolumeSetResource(String instanceId, VolumeSetAttributes volumeSetAttributes) {
        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId(instanceId)
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(volumeSetAttributes);

        return cloudResource;
    }

    private VolumeSetAttributes createVolumeSetAttributes(List<VolumeSetAttributes.Volume> volumes) {
        return new VolumeSetAttributes.Builder()
                .withVolumes(volumes)
                .build();
    }

    @Test
    void getVolumeInfoFromResourceVolumeForLocalSsd() {
        VolumeSetAttributes.Volume vol = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-abc", 10, "local-ssd", CloudVolumeUsageType.GENERAL);
        VolumeInfo volumeInfo = underTest.getVolumeInfoFromResourceVolume(vol);
        assertEquals("abc", volumeInfo.getId());
        assertEquals("/dev/disk/by-id/google-abc", volumeInfo.getDevice());
        assertEquals("10", volumeInfo.getSize());
        assertFalse(volumeInfo.isDatabaseType());
    }

    @Test
    void getVolumeInfoFromResourceVolumeForNonEphemeralDevice() {
        VolumeSetAttributes.Volume vol = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-abc", 10, "HDD", CloudVolumeUsageType.DATABASE);
        VolumeInfo volumeInfo = underTest.getVolumeInfoFromResourceVolume(vol);
        assertEquals("i2v1", volumeInfo.getId());
        assertEquals("/dev/disk/by-id/google-abc", volumeInfo.getDevice());
        assertEquals("10", volumeInfo.getSize());
        assertTrue(volumeInfo.isDatabaseType());
    }
}
