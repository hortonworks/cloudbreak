package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler.service;

import static org.slf4j.LoggerFactory.getLogger;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

@Component
public class LoadBalancerRecreatorService implements ResourceRecreator {

    private static final Logger LOGGER = getLogger(LoadBalancerRecreatorService.class);

    @Inject
    private AwsNativeLoadBalancerLaunchService loadBalancerLaunchService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public void recreate(CreateResourcesRequest request, AwsContext awsContext, AuthenticatedContext ac) throws Exception {
        LOGGER.info("Launching elastic load balancers in Aws native migration");
        CloudCredential cloudCredential = ac.getCloudCredential();
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = commonAwsClient.createElasticLoadBalancingClient(awsCredentialView, region);
        CloudStack cloudStack = request.getCloudStack();
        loadBalancerLaunchService.launchLoadBalancerResources(ac, cloudStack, persistenceNotifier, elasticLoadBalancingClient, false);
    }
}
