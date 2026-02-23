package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.LoadBalancerResourceBuilder;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Abstract class for ResourceBuilders that operate based off of the configuration of the loadbalancers in a given stack
 * <p>
 * GCP:a forwarding rule is the tuple (type, external IP(assigneable), service port(s), backend service { health check port ,instancegroups{instances}})
 * each entry value here needs to be its own backend service and then have 1 forwarding rule for all entries that have the same service port
 * this covers a situation where instance A and B are both set to service port X, but have different health check ports
 */
public abstract class AbstractGcpLoadBalancerBuilder extends AbstractGcpResourceBuilder implements LoadBalancerResourceBuilder<GcpContext> {
    static final String TRAFFICPORTS = "trafficports";

    static final String HCPORT = "hcport";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpLoadBalancerBuilder.class);

    @Inject
    private ResourceRetriever resourceRetriever;

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

    protected String convertProtocolWithTcpFallback(NetworkProtocol protocol) {
        return protocol != null ? protocol.name() : NetworkProtocol.TCP.name();
    }

    protected Map<String, Object> enrichParametersWithAttributes(Map<String, Object> parameters, LoadBalancerType loadBalancerType) {
        Map<String, Object> result = new HashMap<>(parameters);
        Map<String, Object> attributes = new HashMap<>(Enum.valueOf(LoadBalancerTypeAttribute.class, loadBalancerType.name()).asMap());
        attributes.putAll(parameters);
        result.put(CloudResource.ATTRIBUTES, attributes);
        return result;
    }

    protected Optional<CloudResource> fetchResourceFromDb(ResourceType resourceType, Long id) {
        return resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.CREATED, resourceType, id)
                .or(() -> resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.REQUESTED, resourceType, id));
    }

    protected List<CloudResource> fetchAllResourceFromDb(ResourceType resourceType, Long id) {
        List<CloudResource> result = resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, resourceType, id);
        result.addAll(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.REQUESTED, resourceType, id));
        return result;
    }

    protected String mapPortToPortPart(int port) {
        return CloudbreakResourceNameService.DELIMITER + port + CloudbreakResourceNameService.DELIMITER;
    }

    protected String mapProtocolToPart(NetworkProtocol networkProtocol) {
        return CloudbreakResourceNameService.DELIMITER
                + getResourceNameService().normalize(convertProtocolWithTcpFallback(networkProtocol))
                + CloudbreakResourceNameService.DELIMITER;
    }
}
