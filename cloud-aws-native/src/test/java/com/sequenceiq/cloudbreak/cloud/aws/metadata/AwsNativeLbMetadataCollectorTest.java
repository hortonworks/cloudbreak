package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AwsNativeLbMetadataCollectorTest {

    private static final String TARGET_GROUP_NAME = "stackTG%dInternal2021";

    private static final String TARGET_GROUP_ARN = "arn:targetgroup";

    private static final String LOAD_BALANCER_ARN = "arn:loadbalancer";

    private static final String LISTENER_ARN = "arn:listener";

    private final AwsNativeLbMetadataCollector underTest = new AwsNativeLbMetadataCollector();

    @Test
    public void testCollectInternalLoadBalancer() {
        List<CloudResource> resources = createCloudResources(1, true);

        Map<String, Object> expectedParameters = Map.of(
            AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LOAD_BALANCER_ARN,
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 0, LISTENER_ARN + "0Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 0, TARGET_GROUP_ARN + "0Internal"
        );

        Map<String, Object> parameters = underTest.getParameters(LOAD_BALANCER_ARN, resources);
        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerMultiplePorts() {
        List<CloudResource> resources = createCloudResources(3, true);

        Map<String, Object> expectedParameters = Map.of(
            AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LOAD_BALANCER_ARN,
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 0, LISTENER_ARN + "0Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 0, TARGET_GROUP_ARN + "0Internal",
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 1, LISTENER_ARN + "1Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 1, TARGET_GROUP_ARN + "1Internal",
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 2, LISTENER_ARN + "2Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 2, TARGET_GROUP_ARN + "2Internal"
        );

        Map<String, Object> parameters = underTest.getParameters(LOAD_BALANCER_ARN, resources);
        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerMissingListener() {
        List<CloudResource> resources = createCloudResources(1, false);

        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put(AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LOAD_BALANCER_ARN);
        expectedParameters.put(AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 0, null);
        expectedParameters.put(AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 0, TARGET_GROUP_ARN + "0Internal");

        Map<String, Object> parameters = underTest.getParameters(LOAD_BALANCER_ARN, resources);
        assertEquals(expectedParameters, parameters);
    }

    private List<CloudResource> createCloudResources(int numPorts, boolean includeListener) {
        List<CloudResource> resources = new ArrayList<>();
        for (int i = 0; i < numPorts; i++) {
            CloudResource targetGroup = CloudResource.builder()
                .type(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                .instanceId(LOAD_BALANCER_ARN)
                .reference(TARGET_GROUP_ARN + i + "Internal")
                .name(String.format(TARGET_GROUP_NAME, i))
                .build();
            resources.add(targetGroup);
            if (includeListener) {
                CloudResource listener = CloudResource.builder()
                    .type(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                    .instanceId(LOAD_BALANCER_ARN)
                    .reference(LISTENER_ARN + i + "Internal")
                    .name(String.format(TARGET_GROUP_NAME, i))
                    .build();
                resources.add(listener);
            }
        }
        return resources;
    }
}
