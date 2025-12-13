package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.context.VolumeMatcher;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
class AwsContextServiceTest {

    @Spy
    private VolumeMatcher volumeMatcher;

    @InjectMocks
    private AwsContextService awsContextService;

    @Test
    void addResourcesToContextTest() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName("worker1")
                .withParameters(Map.of("privateId", 1L)).withGroup("worker").build());
        resources.add(CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName("worker2")
                .withParameters(Map.of("privateId", 2L)).withGroup("worker").build());
        resources.add(CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName("worker3")
                .withParameters(Map.of("privateId", 3L)).withGroup("worker").build());
        resources.add(CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName("compute2")
                .withParameters(Map.of("privateId", 5L)).withGroup("compute").build());
        resources.add(CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName("compute3")
                .withParameters(Map.of("privateId", 6L)).withGroup("compute").build());
        resources.add(CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.REQUESTED).withName("compute4")
                .withParameters(Map.of("privateId", 7L)).withGroup("compute").build());
        VolumeSetAttributes volume1attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        volume1attributes.setDiscoveryFQDN("worker3.example.com");
        resources.add(CloudResource.builder().withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.REQUESTED).withName("volume1").withGroup("worker")
                .withParameters(Map.of("attributes", volume1attributes)).build());
        VolumeSetAttributes volume2attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        volume2attributes.setDiscoveryFQDN("compute2.example.com");
        resources.add(CloudResource.builder().withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.REQUESTED).withName("volume3").withGroup("compute")
                .withParameters(Map.of("attributes", volume2attributes)).build());
        VolumeSetAttributes volume3attributes = new VolumeSetAttributes("az1", false, "fstab", new ArrayList<>(), 100, "general");
        volume3attributes.setDiscoveryFQDN("compute3.example.com");
        resources.add(CloudResource.builder().withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.REQUESTED).withName("volume4").withGroup("compute")
                .withParameters(Map.of("attributes", volume3attributes)).build());
        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withInstances(workerInstances)
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withInstances(computeInstances)
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build());
        ResourceBuilderContext context = new ResourceBuilderContext("context", Location.location(Region.region("us-west-1")), 0);
        awsContextService.addResourcesToContext(resources, context, groups);

        assertNull(context.getComputeResources(1L));

        List<CloudResource> worker2 = context.getComputeResources(2L);
        assertEquals(1, worker2.size());
        assertTrue(worker2.stream().anyMatch(cloudResource -> "worker2".equals(cloudResource.getName())));

        List<CloudResource> worker3 = context.getComputeResources(3L);
        assertEquals(2, worker3.size());
        assertTrue(worker3.stream().anyMatch(cloudResource -> "worker3".equals(cloudResource.getName())));
        assertTrue(worker3.stream().anyMatch(cloudResource -> "volume1".equals(cloudResource.getName())));

        assertNull(context.getComputeResources(4L));

        List<CloudResource> compute2 = context.getComputeResources(5L);
        assertEquals(2, compute2.size());
        assertTrue(compute2.stream().anyMatch(cloudResource -> "compute2".equals(cloudResource.getName())));
        assertTrue(compute2.stream().anyMatch(cloudResource -> "volume3".equals(cloudResource.getName())));

        List<CloudResource> compute3 = context.getComputeResources(6L);
        assertEquals(2, compute3.size());
        assertTrue(compute3.stream().anyMatch(cloudResource -> "compute3".equals(cloudResource.getName())));
        assertTrue(compute3.stream().anyMatch(cloudResource -> "volume4".equals(cloudResource.getName())));
    }

    @Test
    public void throwExceptionWhenNotEnoughInstancesInGroup() {
        List<CloudResource> instances = List.of();
        ResourceBuilderContext context = new ResourceBuilderContext("context", Location.location(Region.region("us-west-1")), 0);
        List<Group> groups = new ArrayList<>();
        List<CloudInstance> computeInstances = new ArrayList<>();
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1"));
        groups.add(Group.builder()
                .withName("compute")
                .withInstances(computeInstances)
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build());
        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> awsContextService.addInstancesToContext(instances, context, groups));
        assertEquals("Not found enough instances in compute group, expected 1, got 0. " +
                "Please check the instances on your cloud provider for further details.", exception.getMessage());
    }

    private InstanceTemplate getInstanceTemplate(long privateId, String group) {
        return new InstanceTemplate("large", group, privateId, new ArrayList<>(), InstanceStatus.CREATED, null, 1L,
                "image", TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }
}
