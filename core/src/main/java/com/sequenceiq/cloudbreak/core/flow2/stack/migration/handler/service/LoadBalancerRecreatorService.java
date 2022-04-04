package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LoadBalancerRecreatorService implements ResourceRecreator {

    private static final Logger LOGGER = getLogger(LoadBalancerRecreatorService.class);

    @Inject
    private AwsNativeLoadBalancerLaunchService loadBalancerLaunchService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private EventBus eventBus;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private StackService stackService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private ResourceService resourceService;

    @Override
    public void recreate(CreateResourcesRequest request, AwsContext awsContext, AuthenticatedContext ac) throws Exception {
        LOGGER.info("Launching elastic load balancers");
        CloudCredential cloudCredential = ac.getCloudCredential();
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = commonAwsClient.createElasticLoadBalancingClient(awsCredentialView, region);
        CloudStack cloudStack = request.getCloudStack();
        loadBalancerLaunchService.launchLoadBalancerResources(ac, cloudStack, persistenceNotifier, elasticLoadBalancingClient, false);
        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = collectLoadBalancerMetadata(ac, ac.getCloudContext().getId());
        Stack stack = stackService.getByIdWithLists(ac.getCloudContext().getId());
        metadataSetupService.saveLoadBalancerMetadata(stack, cloudLoadBalancerMetadata);
    }

    private List<CloudLoadBalancerMetadata> collectLoadBalancerMetadata(AuthenticatedContext authenticatedContext, Long stackId) {
        List<LoadBalancerType> loadBalancerTypes = loadBalancerPersistenceService.findByStackId(stackId).stream()
                .map(LoadBalancer::getType)
                .collect(Collectors.toList());
        List<CloudResource> cloudResources = resourceService.findByStackIdAndType(stackId, ResourceType.ELASTIC_LOAD_BALANCER).stream()
                .map(r -> cloudResourceConverter.convert(r))
                .collect(Collectors.toList());
        CollectLoadBalancerMetadataRequest request = new CollectLoadBalancerMetadataRequest(authenticatedContext.getCloudContext(),
                authenticatedContext.getCloudCredential(), loadBalancerTypes, cloudResources);
        eventBus.notify(request.selector(), Event.wrap(request));
        try {
            CollectLoadBalancerMetadataResult res = request.await();
            LOGGER.debug("Collect load balancer metadata result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                String msg = "Failed to collect the load balancer metadata. " + res.getErrorDetails().getMessage();
                LOGGER.debug(msg);
                throw new CloudbreakServiceException(msg, res.getErrorDetails());
            }
            return res.getResults();
        } catch (InterruptedException e) {
            LOGGER.error("Error while collect load balancer metadata", e);
            throw new OperationException(e);
        }
    }
}
