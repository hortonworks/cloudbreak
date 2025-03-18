package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.LoadBalancerResourceBuilder;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;

/**
 * Abstract class for ResourceBuilders that operate based off of the configuration of the loadbalancers in a given stack
 * <p>
 * GCP:a forwarding rule is the tuple (type, external IP(assigneable), service port(s), backend service { health check port ,instancegroups{instances}})
 * each entry value here needs to be its own backend service and then have 1 forwarding rule for all entries that have the same service port
 * this covers a situation where instance A and B are both set to service port X, but have different health check ports
 */
public abstract class AbstractGcpLoadBalancerBuilder extends AbstractGcpResourceBuilder implements LoadBalancerResourceBuilder<GcpContext> {

    static final String TRAFFICPORT = "trafficport";

    static final String TRAFFICPORTS = "trafficports";

    static final String HCPORT = "hcport";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpLoadBalancerBuilder.class);

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    protected CloudResource doOperationalRequest(CloudResource resource, ComputeRequest<Operation> request) throws IOException {
        try {
            Operation operation = request.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                LOGGER.error("Bad status code {} from resource {}-{}, {}", operation.getHttpErrorStatusCode(),
                        resourceType(), resource.getName(), operation.getHttpErrorMessage());
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
            }
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            LOGGER.error("Bad Response from GCP for resource {}", resource.getName(), e);
            throw exceptionHandlerWithThrow(e, resource.getName(), resourceType());
        }
    }

    protected List<CloudResource> getCloudResourcesForFrontendAndBackendCreate(GcpContext context, CloudLoadBalancer loadBalancer) {
        List<CloudResource> resources = new ArrayList<>();
        if (loadBalancer.getType() == LoadBalancerType.PRIVATE || loadBalancer.getType() == LoadBalancerType.GATEWAY_PRIVATE) {
            Map<Integer, List<Integer>> hcPortToTrafficPorts = new HashMap<>();
            loadBalancer.getPortToTargetGroupMapping().keySet().forEach(targetGroupPortPair -> {
                Integer healthCheckPort = targetGroupPortPair.getHealthCheckPort();
                Integer trafficPort = targetGroupPortPair.getTrafficPort();
                if (hcPortToTrafficPorts.containsKey(healthCheckPort)) {
                    hcPortToTrafficPorts.get(healthCheckPort).add(trafficPort);
                } else {
                    List<Integer> arr = new ArrayList<>();
                    arr.add(trafficPort);
                    hcPortToTrafficPorts.put(healthCheckPort, arr);
                }
            });
            hcPortToTrafficPorts.forEach((healthCheckPort, trafficPorts) -> {
                String resourceName = getResourceNameService().loadBalancerWithPort(context.getName(), loadBalancer.getType(), healthCheckPort);
                Map<String, Object> parameters = Map.of(
                        TRAFFICPORTS, trafficPorts,
                        HCPORT, healthCheckPort,
                        CloudResource.ATTRIBUTES, Enum.valueOf(LoadBalancerTypeAttribute.class, loadBalancer.getType().name()));
                resources.add(CloudResource.builder()
                        .withType(resourceType())
                        .withName(resourceName)
                        .withParameters(parameters)
                        .build());
            });
        } else {
            loadBalancer.getPortToTargetGroupMapping().keySet().forEach(targetGroupPortPair -> {
                Integer healthCheckPort = targetGroupPortPair.getHealthCheckPort();
                String resourceName = getResourceNameService().loadBalancerWithPort(context.getName(), loadBalancer.getType(), healthCheckPort);
                Map<String, Object> parameters = Map.of(TRAFFICPORT, targetGroupPortPair.getTrafficPort(), HCPORT, healthCheckPort,
                        CloudResource.ATTRIBUTES, Enum.valueOf(LoadBalancerTypeAttribute.class, loadBalancer.getType().name()));
                resources.add(CloudResource.builder()
                        .withType(resourceType())
                        .withName(resourceName)
                        .withParameters(parameters)
                        .build());
            });
        }
        return resources;
    }
}
