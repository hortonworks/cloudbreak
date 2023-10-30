package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ATTRIBUTES;
import static com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.GENERAL;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;

@ExtendWith(MockitoExtension.class)
class VolumeResourceCollectorTest {

    @InjectMocks
    private VolumeResourceCollector underTest;

    @Test
    void getVolumeIdsByVolumeResourceShouldFilterOutVolumesWithoutIds() {
        List<VolumeSetAttributes.Volume> volumes = List.of(new VolumeSetAttributes.Volume("id1", null, null, null, GENERAL),
                new VolumeSetAttributes.Volume(null, null, null, null, GENERAL));
        CloudResource cloudResource = CloudResource.builder()
                .withType(AWS_VOLUMESET)
                .withStatus(CommonStatus.REQUESTED)
                .withName("resource")
                .withParameters(Map.of(ATTRIBUTES, new VolumeSetAttributes.Builder().withVolumes(volumes).build()))
                .build();
        List<CloudResource> cloudResources = List.of(cloudResource);
        Pair<List<String>, List<CloudResource>> volumeIdsByVolumeResources = underTest.getVolumeIdsByVolumeResources(cloudResources, AWS_VOLUMESET,
                resource -> resource.getParameterWithFallback(ATTRIBUTES, VolumeSetAttributes.class));

        assertThat(volumeIdsByVolumeResources.getFirst()).containsExactly("id1");
        assertThat(volumeIdsByVolumeResources.getSecond()).containsExactly(cloudResource);
    }

}