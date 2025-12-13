package com.sequenceiq.cloudbreak.domain.stack;

import static com.sequenceiq.cloudbreak.domain.stack.ResourceUtil.getLatestResourceByInstanceId;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.common.api.type.ResourceType;

class ResourceUtilTest {

    @Test
    void selectResourceWithLatestIdIfThereAreMultipleForGivenInstanceId() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId", 1L));
        volumeSets.add(getVolumeSetResource("anInstanceId", 2L));
        volumeSets.add(getVolumeSetResource("secInstanceId", 3L));
        volumeSets.add(getVolumeSetResource("thirdInstanceId", 4L));
        volumeSets.add(getVolumeSetResource("anInstanceId", 5L));
        List<Resource> latestResourceByInstanceId = getLatestResourceByInstanceId(volumeSets);

        assertEquals(3, latestResourceByInstanceId.size());
        assertEquals(3, latestResourceByInstanceId.stream().map(Resource::getId).distinct().count());
        assertEquals(Set.of(3L, 4L, 5L), latestResourceByInstanceId.stream().map(Resource::getId).collect(Collectors.toSet()));

    }

    @Test
    void selectAllResourcesIfThereAreOnlyOneForEachGivenInstanceId() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId", 1L));
        volumeSets.add(getVolumeSetResource("secInstanceId", 2L));
        volumeSets.add(getVolumeSetResource("thirdInstanceId", 3L));
        List<Resource> latestResourceByInstanceId = getLatestResourceByInstanceId(volumeSets);

        assertEquals(3, latestResourceByInstanceId.size());
        assertEquals(3, latestResourceByInstanceId.stream().map(Resource::getId).distinct().count());
        assertEquals(Set.of(1L, 2L, 3L), latestResourceByInstanceId.stream().map(Resource::getId).collect(Collectors.toSet()));
    }

    private Resource getVolumeSetResource(String instanceID, Long id) {
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AZURE_VOLUMESET);
        resource.setInstanceId(instanceID);
        resource.setId(id);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes.Builder()
                .build();
        resource.setAttributes(new Json(volumeSetAttributes));
        return resource;
    }

}