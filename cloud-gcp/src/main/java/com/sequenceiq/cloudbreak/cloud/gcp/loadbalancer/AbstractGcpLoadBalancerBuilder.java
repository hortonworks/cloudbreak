package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.io.IOException;
import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.LoadBalancerResourceBuilder;

/**
 * Abstract class for ResourceBuilders that operate based off of the configuration of the loadbalancers in a given stack
 *
 * GCP:a forwarding rule is the tuple (type, external IP(assigneable), service port(s), backend service { health check port ,instancegroups{instances}}
 * each entry value here needs to be it's own backend service and then have 1 forwarding rule for all entries that have the same service port
 * this covers a situation where instance A and B are both set to service port X, but have different health check ports
 * */
public abstract class AbstractGcpLoadBalancerBuilder extends AbstractGcpResourceBuilder implements LoadBalancerResourceBuilder<GcpContext> {

    static final String TRAFFICPORT = "trafficport";

    static final String HCPORT = "hcport";

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    protected CloudResource doOperationalRequest(CloudResource resource, ComputeRequest<Operation> request) throws IOException {
        try {
            Operation operation = request.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
            }
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            throw exceptionHandlerWithThrow(e, resource.getName(), resourceType());
        }
    }
}
