package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_LOAD_BALANCER_RESOURCE_BUILDER_ORDER;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.AbstractAwsNativeComputeBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeLoadBalancerResourceBuilder extends AbstractAwsNativeComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeLoadBalancerResourceBuilder.class);

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("There is no need to create resources as it is handled by the launchLoadBalancers method ResourceConnector interface...");
        return List.of();
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.info("There is no need to create resources as it is handled by the launchLoadBalancers method ResourceConnector interface...");
        return List.of();
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        LOGGER.info("Deleting load balancer ('{}') from provider side", resource.getReference());
        AmazonElasticLoadBalancingClient loadBalancingClient = context.getLoadBalancingClient();
        DeleteLoadBalancerRequest deleteLoadBalancerRequest = new DeleteLoadBalancerRequest()
                .withLoadBalancerArn(resource.getReference());
        DeleteLoadBalancerResult deleteResult = awsMethodExecutor.execute(() -> loadBalancingClient.deleteLoadBalancer(deleteLoadBalancerRequest), null);
        return deleteResult != null ? resource : null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.ELASTIC_LOAD_BALANCER;
    }

    @Override
    public int order() {
        return NATIVE_LOAD_BALANCER_RESOURCE_BUILDER_ORDER;
    }
}
