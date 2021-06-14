package com.sequenceiq.cloudbreak.cloud.template.context;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class VolumeMatcherTest {

    @InjectMocks
    private VolumeMatcher volumeMatcher;

    @Test
    public void addVolumeResourcesToContextTest() {
        List<CloudResource> workerInstanceResources = new ArrayList<>();
        workerInstanceResources.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.REQUESTED).name("worker1")
                .group("worker").build());
        workerInstanceResources.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.REQUESTED).name("worker2")
                .group("worker").build());
        List<CloudResource> computeInstanceResources = new ArrayList<>();
        computeInstanceResources.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.REQUESTED).name("compute2")
                .group("compute").build());
        computeInstanceResources.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.REQUESTED).name("compute3")
                .group("compute").build());
        computeInstanceResources.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.REQUESTED).name("compute4")
                .group("compute").build());

        List<CloudResource> workerVolumeResources = new ArrayList<>();

        VolumeSetAttributes volume1attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        volume1attributes.setDiscoveryFQDN("worker3.example.com");
        workerVolumeResources.add(CloudResource.builder().type(ResourceType.AWS_VOLUMESET).status(CommonStatus.REQUESTED).name("volume1").group("worker")
                .params(Map.of("attributes", volume1attributes)).build());

        List<CloudResource> computeVolumeResources = new ArrayList<>();

        VolumeSetAttributes volume2attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        volume2attributes.setDiscoveryFQDN("compute2.example.com");
        computeVolumeResources.add(CloudResource.builder().type(ResourceType.AWS_VOLUMESET).status(CommonStatus.REQUESTED).name("volume3").group("compute")
                .params(Map.of("attributes", volume2attributes)).build());

        VolumeSetAttributes volume3attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        volume3attributes.setDiscoveryFQDN("compute3.example.com");
        computeVolumeResources.add(CloudResource.builder().type(ResourceType.AWS_VOLUMESET).status(CommonStatus.REQUESTED).name("volume4").group("compute")
                .params(Map.of("attributes", volume3attributes)).build());

        VolumeSetAttributes volume4attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        computeVolumeResources.add(CloudResource.builder().type(ResourceType.AWS_VOLUMESET).status(CommonStatus.REQUESTED).name("volume5").group("compute")
                .params(Map.of("attributes", volume4attributes)).build());

        List<CloudInstance> workerInstances = new ArrayList<>();
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        List<CloudInstance> computeInstances = new ArrayList<>();
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(7L, "compute"), mock(InstanceAuthentication.class), "subnet1", "az1"));

        ResourceBuilderContext context = new ResourceBuilderContext("context", Location.location(Region.region("us-west-1")), 0);

        volumeMatcher.addVolumeResourcesToContext(workerInstances, workerInstanceResources, workerVolumeResources, context);

        Assertions.assertNull(context.getComputeResources(1L));

        List<CloudResource> worker2 = context.getComputeResources(2L);
        Assertions.assertEquals(1, worker2.size());
        Assertions.assertTrue(worker2.stream().anyMatch(cloudResource -> "worker1".equals(cloudResource.getName())));

        List<CloudResource> worker3 = context.getComputeResources(3L);
        Assertions.assertEquals(2, worker3.size());
        Assertions.assertTrue(worker3.stream().anyMatch(cloudResource -> "worker2".equals(cloudResource.getName())));
        Assertions.assertTrue(worker3.stream().anyMatch(cloudResource -> "volume1".equals(cloudResource.getName())));

        volumeMatcher.addVolumeResourcesToContext(computeInstances, computeInstanceResources, computeVolumeResources, context);

        Assertions.assertNull(context.getComputeResources(4L));

        List<CloudResource> compute2 = context.getComputeResources(5L);
        Assertions.assertEquals(2, compute2.size());
        Assertions.assertTrue(compute2.stream().anyMatch(cloudResource -> "compute2".equals(cloudResource.getName())));
        Assertions.assertTrue(compute2.stream().anyMatch(cloudResource -> "volume3".equals(cloudResource.getName())));

        List<CloudResource> compute3 = context.getComputeResources(6L);
        Assertions.assertEquals(2, compute3.size());
        Assertions.assertTrue(compute3.stream().anyMatch(cloudResource -> "compute3".equals(cloudResource.getName())));
        Assertions.assertTrue(compute3.stream().anyMatch(cloudResource -> "volume4".equals(cloudResource.getName())));
    }

    private InstanceTemplate getInstanceTemplate(long privateId, String group) {
        return new InstanceTemplate("large", group, privateId, new ArrayList<>(), InstanceStatus.CREATED, null, 1L, "image",
                TemporaryStorage.ATTACHED_VOLUMES);
    }

}