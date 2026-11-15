package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.cloud.azure.providersync.AzureVolumeSetSyncer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.DiskValidationService.DiskValidationVolumes;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class DiskValidationServiceTest {

    public static final long STACK_ID = 1L;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ResourceService resourceService;

    @Spy
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @InjectMocks
    private DiskValidationService underTest;

    @Mock
    private StackView stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private StackDtoDelegate stackDto;

    @BeforeEach
    void setup() {
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getCluster()).thenReturn(cluster);
        List<ProviderResourceSyncer> providerResourceSyncers = new ArrayList<>();
        providerResourceSyncers.add(new AzureVolumeSetSyncer());
        Field providerResourceSyncersField = ReflectionUtils.findField(DiskValidationService.class, "providerResourceSyncers");
        ReflectionUtils.makeAccessible(providerResourceSyncersField);
        ReflectionUtils.setField(providerResourceSyncersField, underTest, providerResourceSyncers);
    }

    @Test
    void testGetVolumesForValidation() {
        setupConverter();
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET)))
                .thenReturn(getResourcesForStack(false, ResourceType.AWS_VOLUMESET));
        for (int i = 1; i <= 2; i++) {
            when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn" + i))).thenReturn(getInstanceMetadataOptional(i));
        }

        List<CloudResource> result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames()).volumes();

        assertEquals(2, result.size());
        List<String> instanceIds = result.stream()
                .map(CloudResource::getInstanceId)
                .toList();
        assertTrue(instanceIds.containsAll(List.of("instance1", "instance2")));
    }

    @Test
    void testGetVolumesForValidationAzure() {
        setupConverter();
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AZURE_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AZURE");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AZURE_VOLUMESET, ResourceType.AZURE_RESOURCE_GROUP)))
                .thenReturn(getResourcesForStack(false, ResourceType.AZURE_VOLUMESET));
        for (int i = 1; i <= 2; i++) {
            when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn" + i))).thenReturn(getInstanceMetadataOptional(i));
        }

        List<CloudResource> result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames()).volumes();

        assertEquals(2, result.size());
        List<String> instanceIds = result.stream()
                .map(CloudResource::getInstanceId)
                .toList();
        assertTrue(instanceIds.containsAll(List.of("instance1", "instance2")));
    }

    @Test
    void testGetVolumesForValidationDeleteOnTerminationIsSet() {
        setupConverter();
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET)))
                .thenReturn(getResourcesForStack(true, ResourceType.AWS_VOLUMESET));
        for (int i = 1; i <= 2; i++) {
            when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn" + i))).thenReturn(getInstanceMetadataOptional(i));
        }

        List<CloudResource> result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames()).volumes();

        assertEquals(0, result.size());
    }

    @Test
    void testGetVolumesForValidationDeleteOnTerminationIsSetOnEmbeddedDatabaseHost() {
        setupConverter();
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(true);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET)))
                .thenReturn(getResourcesForStack(true, ResourceType.AWS_VOLUMESET));
        Optional<InstanceMetaData> cmServer = getInstanceMetadataOptional(1);
        cmServer.get().setClusterManagerServer(true);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn1"))).thenReturn(cmServer);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn2"))).thenReturn(getInstanceMetadataOptional(2));

        DiskValidationVolumes result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames());

        assertTrue(result.volumes().isEmpty());
        assertEmbeddedDatabaseVolumeOfInstance1(result);
    }

    @Test
    void testGetVolumesForValidationEmbeddedDatabaseHost() {
        setupConverter();
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(true);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET)))
                .thenReturn(getResourcesForStack(false, ResourceType.AWS_VOLUMESET));
        Optional<InstanceMetaData> cmServer = getInstanceMetadataOptional(1);
        cmServer.get().setClusterManagerServer(true);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn1"))).thenReturn(cmServer);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn2"))).thenReturn(getInstanceMetadataOptional(2));

        DiskValidationVolumes result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames());

        assertEquals(2, result.volumes().size());
        result.volumes()
                .forEach(volumeSet -> assertEquals(2, volumeSet.getTypedAttributes(VolumeSetAttributes.class, () -> null).getVolumes().size()));
        assertEmbeddedDatabaseVolumeOfInstance1(result);
    }

    @Test
    void testGetVolumesForValidationEmbeddedDatabaseHostAzureAddsResourceGroup() {
        setupConverter();
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(true);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AZURE_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AZURE");
        List<Resource> resources = new ArrayList<>(getResourcesForStack(false, ResourceType.AZURE_VOLUMESET));
        Resource resourceGroup = new Resource();
        resourceGroup.setResourceStatus(CommonStatus.CREATED);
        resourceGroup.setResourceName("resource-group");
        resourceGroup.setResourceType(ResourceType.AZURE_RESOURCE_GROUP);
        resourceGroup.setAttributes(Json.silent(Map.of()));
        resources.add(resourceGroup);
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AZURE_VOLUMESET, ResourceType.AZURE_RESOURCE_GROUP)))
                .thenReturn(resources);
        Optional<InstanceMetaData> cmServer = getInstanceMetadataOptional(1);
        cmServer.get().setClusterManagerServer(true);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn1"))).thenReturn(cmServer);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn2"))).thenReturn(getInstanceMetadataOptional(2));

        DiskValidationVolumes result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames());

        assertEquals(Set.of("name1", "name2", "resource-group"), result.volumes().stream().map(CloudResource::getName).collect(Collectors.toSet()));
        assertEquals(List.of("name1", "resource-group"), result.embeddedDatabaseVolumes().stream().map(CloudResource::getName).toList());
        assertEquals(Set.of("instance1"), result.clusterManagerServerWithEmbeddedDbInstanceIds());
    }

    @Test
    void testGetVolumesForValidationMatchesVolumeSetByFqdnWhenInstanceIdDiffers() {
        setupConverter();
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(true);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        Resource detachedVolumeSet = getResourcesForStack(false, ResourceType.AWS_VOLUMESET).getFirst();
        detachedVolumeSet.setInstanceId("old-instance1");
        detachedVolumeSet.setAttributes(Json.silent(new VolumeSetAttributes("availabilityZone", false, "fstab", null, List.of(
                new VolumeSetAttributes.Volume("vol-db-instance1", "/dev/xvdc", 100, "gp3", CloudVolumeUsageType.DATABASE)), "fqdn1")));
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET)))
                .thenReturn(List.of(detachedVolumeSet));
        InstanceMetaData cmServer = new InstanceMetaData();
        cmServer.setDiscoveryFQDN("fqdn1");
        cmServer.setClusterManagerServer(true);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn1"))).thenReturn(Optional.of(cmServer));

        DiskValidationVolumes result = underTest.getVolumesForValidation(stackDto, Map.of("group", Set.of("fqdn1")));

        assertEquals(List.of("old-instance1"), result.volumes().stream().map(CloudResource::getInstanceId).toList());
        assertEquals(List.of("old-instance1"), result.embeddedDatabaseVolumes().stream().map(CloudResource::getInstanceId).toList());
        assertEquals(Set.of("old-instance1"), result.clusterManagerServerWithEmbeddedDbInstanceIds());
    }

    @Test
    void testGetVolumesForValidationCmServerWithoutDatabaseUsageTypeVolume() {
        setupConverter();
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(true);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        Resource volumeSet = getResourcesForStack(false, ResourceType.AWS_VOLUMESET).getFirst();
        volumeSet.setAttributes(Json.silent(new VolumeSetAttributes("availabilityZone", false, "fstab", List.of(
                new VolumeSetAttributes.Volume("vol-general-instance1", "/dev/xvdb", 100, "gp3", CloudVolumeUsageType.GENERAL)), 100, "gp3")));
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET))).thenReturn(List.of(volumeSet));
        Optional<InstanceMetaData> cmServer = getInstanceMetadataOptional(1);
        cmServer.get().setClusterManagerServer(true);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn1"))).thenReturn(cmServer);

        DiskValidationVolumes result = underTest.getVolumesForValidation(stackDto, Map.of("group", Set.of("fqdn1")));

        assertEquals(1, result.volumes().size());
        assertTrue(result.embeddedDatabaseVolumes().isEmpty());
        assertTrue(result.clusterManagerServerWithEmbeddedDbInstanceIds().isEmpty());
    }

    private void assertEmbeddedDatabaseVolumeOfInstance1(DiskValidationVolumes result) {
        assertEquals(Set.of("instance1"), result.clusterManagerServerWithEmbeddedDbInstanceIds());
        assertEquals(1, result.embeddedDatabaseVolumes().size());
        CloudResource databaseVolumeSet = result.embeddedDatabaseVolumes().getFirst();
        assertEquals("name1", databaseVolumeSet.getName());
        assertEquals("instance1", databaseVolumeSet.getInstanceId());
        assertEquals(List.of("vol-db-instance1"), databaseVolumeSet.getTypedAttributes(VolumeSetAttributes.class, () -> null).getVolumes().stream()
                .map(VolumeSetAttributes.Volume::getId)
                .toList());
    }

    @Test
    void testGetVolumesForValidationCmServerWithoutEmbeddedDatabaseOnAttachedDisk() {
        setupConverter();
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(false);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET)))
                .thenReturn(getResourcesForStack(false, ResourceType.AWS_VOLUMESET));
        Optional<InstanceMetaData> cmServer = getInstanceMetadataOptional(1);
        cmServer.get().setClusterManagerServer(true);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn1"))).thenReturn(cmServer);
        when(instanceMetaDataService.findHostInStack(eq(STACK_ID), eq("fqdn2"))).thenReturn(getInstanceMetadataOptional(2));

        DiskValidationVolumes result = underTest.getVolumesForValidation(stackDto, getHostgroupWithHostNames());

        assertEquals(2, result.volumes().size());
        assertTrue(result.embeddedDatabaseVolumes().isEmpty());
        assertTrue(result.clusterManagerServerWithEmbeddedDbInstanceIds().isEmpty());
    }

    private void setupConverter() {
        Field resourceAttributeUtilField = ReflectionUtils.findField(ResourceToCloudResourceConverter.class, "resourceAttributeUtil");
        ReflectionUtils.makeAccessible(resourceAttributeUtilField);
        ReflectionUtils.setField(resourceAttributeUtilField, resourceToCloudResourceConverter, resourceAttributeUtil);
    }

    private Optional<InstanceMetaData> getInstanceMetadataOptional(int id) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("instance" + id);
        instanceMetaData.setDiscoveryFQDN("fqdn" + id);
        return Optional.of(instanceMetaData);
    }

    private List<Resource> getResourcesForStack(boolean deleteOnTermination, ResourceType resourceType) {
        List<Resource> result = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Resource resource = new Resource();
            resource.setResourceStatus(CommonStatus.CREATED);
            resource.setResourceName("name" + i);
            resource.setResourceType(resourceType);
            resource.setInstanceGroup("group");
            resource.setInstanceId("instance" + i);
            resource.setAttributes(getVolumeSetAttributesJsonWithDeleteOnTerminationFlag(deleteOnTermination, "instance" + i));
            result.add(resource);
        }
        return result;
    }

    private Json getVolumeSetAttributesJsonWithDeleteOnTerminationFlag(boolean deleteOnTermination, String instanceId) {
        List<VolumeSetAttributes.Volume> volumes = List.of(
                new VolumeSetAttributes.Volume("vol-general-" + instanceId, "/dev/xvdb", 100, "gp3", CloudVolumeUsageType.GENERAL),
                new VolumeSetAttributes.Volume("vol-db-" + instanceId, "/dev/xvdc", 100, "gp3", CloudVolumeUsageType.DATABASE));
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("availabilityZone", deleteOnTermination,
                "fstab", volumes, 100, "volumeType");
        return Json.silent(volumeSetAttributes);
    }

    private Map<String, Set<String>> getHostgroupWithHostNames() {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("group", Set.of("fqdn1", "fqdn2"));
        return result;
    }

}