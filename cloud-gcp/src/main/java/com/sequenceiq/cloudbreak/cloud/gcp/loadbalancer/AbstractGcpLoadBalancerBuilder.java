package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.io.IOException;
import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.template.LoadBalancerResourceBuilder;

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
}
