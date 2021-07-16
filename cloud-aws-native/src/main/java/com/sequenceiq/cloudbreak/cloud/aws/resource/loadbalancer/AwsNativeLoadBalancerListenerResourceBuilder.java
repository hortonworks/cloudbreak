package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_LOAD_BALANCER_LISTENER_RESOURCE_BUILDER_ORDER;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteListenerResult;
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
public class AwsNativeLoadBalancerListenerResourceBuilder extends AbstractAwsNativeComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeLoadBalancerListenerResourceBuilder.class);

    private static final String LISTENER_NOT_FOUND_ERROR_CODE = "ListenerNotFound";

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
    public CloudResource delete(AwsContext context, com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext auth, CloudResource resource)
            throws Exception {
        LOGGER.info("Trying to delete listener '{}', may it has already been deleted with it's related load balancer '{}'",
                resource.getReference(),
                resource.getInstanceId());
        AmazonElasticLoadBalancingClient loadBalancingClient = context.getLoadBalancingClient();
        DeleteListenerRequest deleteListenerRequest = new DeleteListenerRequest()
                .withListenerArn(resource.getReference());
        DeleteListenerResult deleteResult = null;
        try {
            deleteResult = awsMethodExecutor.execute(() -> loadBalancingClient.deleteListener(deleteListenerRequest), null);
        } catch (AmazonServiceException awsException) {
            if (StringUtils.isNotEmpty(awsException.getErrorCode()) && LISTENER_NOT_FOUND_ERROR_CODE.equals(awsException.getErrorCode())) {
                LOGGER.info("Listener doesn't exist with id: '{}'", resource.getReference());
            } else {
                LOGGER.warn("Listener could not be fetched from AWS with id: '{}'", resource.getReference(), awsException);
                throw awsException;
            }
        }
        return deleteResult != null ? resource : null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.ELASTIC_LOAD_BALANCER_LISTENER;
    }

    @Override
    public int order() {
        return NATIVE_LOAD_BALANCER_LISTENER_RESOURCE_BUILDER_ORDER;
    }
}
