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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
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

    @BeforeEach
    void setup() {
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

        List<CloudResource> result = underTest.getVolumesForValidation(stack, getHostgroupWithHostNames());

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

        List<CloudResource> result = underTest.getVolumesForValidation(stack, getHostgroupWithHostNames());

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

        List<CloudResource> result = underTest.getVolumesForValidation(stack, getHostgroupWithHostNames());

        assertEquals(0, result.size());
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
            resource.setAttributes(getVolumeSetAttributesJsonWithDeleteOnTerminationFlag(deleteOnTermination));
            result.add(resource);
        }
        return result;
    }

    private Json getVolumeSetAttributesJsonWithDeleteOnTerminationFlag(boolean deleteOnTermination) {
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("availabilityZone", deleteOnTermination,
                "fstab", List.of(), 100, "volumeType");
        return Json.silent(volumeSetAttributes);
    }

    private Map<String, Set<String>> getHostgroupWithHostNames() {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("group", Set.of("fqdn1", "fqdn2"));
        return result;
    }

}