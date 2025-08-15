package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataCloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataCloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Component
public class AwsMigrationUtil {

    private static final Logger LOGGER = getLogger(AwsMigrationUtil.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private EventBus eventBus;

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    public boolean allInstancesDeletedFromCloudFormation(AuthenticatedContext ac, CloudResource cloudResource) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        List<StackResource> asGroups = getStackResourceIfCfExists(ac, cloudResource);
        LOGGER.debug("AutoScalingGroup fetched: {}", asGroups);
        boolean empty = true;
        int i = 0;
        while (empty && i < asGroups.size()) {
            StackResource asGroup = asGroups.get(i);
            List<String> result = cfStackUtil.getInstanceIds(awsClient.createAutoScalingClient(awsCredential, regionName), asGroup.physicalResourceId());
            LOGGER.debug("{} autoScalingGroup has {} instance(s): {}", asGroup.physicalResourceId(), result.size(), result);
            empty = result.isEmpty();
            i++;
        }
        return empty;
    }

    private List<StackResource> getStackResourceIfCfExists(AuthenticatedContext ac, CloudResource cloudResource) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        List<StackResource> asGroups = new ArrayList<>();
        String stackName = cloudResource.getName();
        try {
            DescribeStackResourcesResponse describeStackResourcesResult = awsClient.createCloudFormationClient(awsCredential, regionName)
                    .describeStackResources(DescribeStackResourcesRequest.builder().stackName(stackName).build());
            asGroups = describeStackResourcesResult.stackResources().stream()
                    .filter(it -> "AWS::AutoScaling::AutoScalingGroup".equals(it.resourceType()))
                    .collect(Collectors.toList());
        } catch (CloudFormationException e) {
            if (e.awsErrorDetails().errorMessage().contains(stackName + " does not exist")) {
                LOGGER.info("CloudFormation resource does not found: {}", e.getMessage());
            } else {
                LOGGER.error("Cannot describe stack resources: {}", e.getMessage(), e);
                throw e;
            }
        }
        return asGroups;
    }

    public void changeLoadBalancer(AuthenticatedContext ac) {
        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = collectLoadBalancerMetadata(ac, ac.getCloudContext().getId());
        StackDto stackDto = stackDtoService.getById(ac.getCloudContext().getId(), false);
        metadataSetupService.saveLoadBalancerMetadata(stackDto.getStack(), cloudLoadBalancerMetadata);
        gatewayPublicEndpointManagementService.updateDnsEntryForLoadBalancers(stackDto);
    }

    private List<CloudLoadBalancerMetadata> collectLoadBalancerMetadata(AuthenticatedContext authenticatedContext, Long stackId) {
        List<LoadBalancerType> loadBalancerTypes = loadBalancerPersistenceService.findByStackId(stackId).stream()
                .map(LoadBalancer::getType)
                .collect(Collectors.toList());
        List<CloudResource> cloudResources = resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, stackId);
        CollectLoadBalancerMetadataCloudPlatformRequest request = getCollectLoadBalancerMetadataRequest(authenticatedContext, loadBalancerTypes, cloudResources);
        eventBus.notify(request.selector(), Event.wrap(request));
        try {
            CollectLoadBalancerMetadataCloudPlatformResult res = request.await();
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

    CollectLoadBalancerMetadataCloudPlatformRequest getCollectLoadBalancerMetadataRequest(AuthenticatedContext authenticatedContext,
            List<LoadBalancerType> loadBalancerTypes, List<CloudResource> cloudResources) {
        return new CollectLoadBalancerMetadataCloudPlatformRequest(authenticatedContext.getCloudContext(),
                authenticatedContext.getCloudCredential(), loadBalancerTypes, cloudResources);
    }
}
