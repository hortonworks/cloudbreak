package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.IpAddressType;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetTypeEnum;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsLoadBalancerCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCloudStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeLoadBalancerLaunchService {
    static final String DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE = "DuplicateLoadBalancerName";

    static final String DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE = "DuplicateTargetGroupName";

    static final String DUPLICATE_LISTENER_ERROR_CODE = "DuplicateListener";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeLoadBalancerLaunchService.class);

    private static final int TARGET_GROUP_HEALTH_CHECK_INTERVAL_SECONDS = 10;

    private static final int HEALTHY_THRESHOLD_COUNT = 2;

    private static final int UNHEALTHY_THRESHOLD_COUNT = 2;

    @Inject
    private AwsLoadBalancerCommonService loadBalancerCommonService;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private PersistenceRetriever persistenceRetriever;

    @Inject
    private AwsResourceNameService resourceNameService;

    public List<CloudResourceStatus> launchLoadBalancerResources(AuthenticatedContext authenticatedContext, CloudStack stack,
            PersistenceNotifier persistenceNotifier, AmazonElasticLoadBalancingClient loadBalancingClient) {
        LOGGER.debug("Creating AWS load balancer and it's resources for cloud stack: '{}'", authenticatedContext.getCloudContext().getCrn());
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        Map<String, List<String>> privateIdsByGroupName = stack.getGroups()
                .stream()
                .collect(Collectors.toMap(Group::getName,
                        group -> group.getInstances().stream().map(in -> String.valueOf(in.getTemplate().getPrivateId())).collect(toList())));
        List<AwsLoadBalancer> loadBalancers = loadBalancerCommonService.getAwsLoadBalancers(stack.getLoadBalancers(), privateIdsByGroupName, awsNetworkView);
        AwsCloudStackView awsCloudStackView = new AwsCloudStackView(stack);
        Long stackId = authenticatedContext.getCloudContext().getId();
        String stackName = authenticatedContext.getCloudContext().getName();
        Collection<Tag> tags = awsTaggingService.prepareElasticLoadBalancingTags(awsCloudStackView.getTags());
        ResourceCreationContext creationContext = new ResourceCreationContext(stackId, stackName, tags, persistenceNotifier, loadBalancingClient,
                authenticatedContext.getCloudContext());
        try {
            for (AwsLoadBalancer awsLoadBalancer : loadBalancers) {
                createLoadBalancer(creationContext, awsLoadBalancer);
                for (AwsListener listener : awsLoadBalancer.getListeners()) {
                    AwsTargetGroup targetGroup = listener.getTargetGroup();
                    creationContext.setTargetGroupName(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, stackName,
                            awsLoadBalancer.getScheme().resourceName(), targetGroup.getPort()));
                    createTargetGroup(creationContext, awsNetworkView, targetGroup);
                    createListener(creationContext, listener);
                    registerTarget(loadBalancingClient, stackId, targetGroup);
                }
            }
        } catch (Exception ex) {
            String message = "Load balancer and it's resources could not be created";
            LOGGER.warn(message, ex);
            throw new CloudConnectorException(message, ex);
        }
        return creationContext.getResourceStatuses();
    }

    private void createLoadBalancer(ResourceCreationContext context, AwsLoadBalancer awsLoadBalancer) {
        CloudResource loadBalancerResource;
        Optional<CloudResource> existingLoadBalancer = persistenceRetriever.retrieveFirstByTypeAndStatusForStack(ResourceType.ELASTIC_LOAD_BALANCER,
                CommonStatus.CREATED, context.getStackId());
        String loadBalancerArn;
        if (existingLoadBalancer.isPresent()) {
            loadBalancerResource = existingLoadBalancer.get();
            loadBalancerArn = loadBalancerResource.getReference();
            LOGGER.info("Elastic load balancer resource has already been created for stack proceeding forward with existing resource '{}'",
                    loadBalancerArn);
        } else {
            AwsLoadBalancerScheme scheme = awsLoadBalancer.getScheme();
            Set<String> subnetIds = awsLoadBalancer.getSubnetIds();
            String loadBalancerName = resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, context.getStackName(), scheme.resourceName());
            LOGGER.info("Creating load balancer with name '{}', subnet ids: '{}' and scheme: '{}'", loadBalancerName, String.join(",", subnetIds), scheme);
            CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
                    .withName(loadBalancerName)
                    .withSubnets(subnetIds)
                    .withScheme(scheme.awsScheme())
                    .withType(LoadBalancerTypeEnum.Network)
                    .withIpAddressType(IpAddressType.Ipv4)
                    .withTags(context.getTags());
            CreateLoadBalancerResult loadBalancerResult = createOrGetLoadBalancer(context, request);
            loadBalancerArn = loadBalancerResult.getLoadBalancers()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getLoadBalancerArn();
            context.setLoadBalancerArn(loadBalancerArn);
            loadBalancerResource = new CloudResource.Builder()
                    .name(loadBalancerName)
                    .type(ResourceType.ELASTIC_LOAD_BALANCER)
                    .reference(loadBalancerArn)
                    .status(CommonStatus.CREATED)
                    .build();
            context.getPersistenceNotifier().notifyAllocation(loadBalancerResource, context.getCloudContext());
        }
        context.setLoadBalancerArn(loadBalancerArn);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(loadBalancerResource, ResourceStatus.CREATED);
        context.addResourceStatus(cloudResourceStatus);
    }

    private CreateLoadBalancerResult createOrGetLoadBalancer(ResourceCreationContext context, CreateLoadBalancerRequest request) {
        try {
            return context.getLoadBalancingClient().registerLoadBalancer(request);
        } catch (AmazonServiceException amazonServiceException) {
            String errorCode = amazonServiceException.getErrorCode();
            if (StringUtils.isNotEmpty(errorCode) && errorCode.contains(DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE)) {
                DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest().withNames(request.getName());
                DescribeLoadBalancersResult describeLoadBalancersResult = context.getLoadBalancingClient().describeLoadBalancers(describeLoadBalancersRequest);
                return describeLoadBalancersResult.getLoadBalancers()
                        .stream()
                        .findFirst()
                        .map(loadBalancer -> new CreateLoadBalancerResult().withLoadBalancers(loadBalancer))
                        .orElseThrow(() -> new CloudConnectorException(
                                String.format("Load balancer could not be created and found with name: '%s'", request.getName())));
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void createTargetGroup(ResourceCreationContext context, AwsNetworkView awsNetworkView, AwsTargetGroup targetGroup) {
        CloudResource targetGroupResource;
        Optional<CloudResource> existingLoadBalancer = persistenceRetriever.retrieveFirstByTypeAndStatusForStack(
                ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, CommonStatus.CREATED, context.getStackId());
        String targetGroupArn;
        if (existingLoadBalancer.isPresent()) {
            targetGroupResource = existingLoadBalancer.get();
            targetGroupArn = targetGroupResource.getReference();
            LOGGER.info("Elastic load balancer target group resource has already been created for stack proceeding forward with existing resource '{}'",
                    targetGroupArn);
        } else {
            int targetGroupPort = targetGroup.getPort();
            String loadBalancerArn = context.getLoadBalancerArn();
            String targetGroupName = context.getTargetGroupName();
            LOGGER.info("Creating target group for load balancer('{}') with name: '{}' port: '{}'", loadBalancerArn, targetGroupName,
                    targetGroupPort);
            CreateTargetGroupRequest targetGroupRequest = new CreateTargetGroupRequest()
                    .withName(targetGroupName)
                    .withPort(targetGroupPort)
                    .withProtocol(ProtocolEnum.TCP)
                    .withTargetType(TargetTypeEnum.Instance)
                    .withHealthCheckPort(targetGroup.getHealthCheckPort())
                    .withHealthCheckIntervalSeconds(TARGET_GROUP_HEALTH_CHECK_INTERVAL_SECONDS)
                    .withHealthyThresholdCount(HEALTHY_THRESHOLD_COUNT)
                    .withUnhealthyThresholdCount(UNHEALTHY_THRESHOLD_COUNT)
                    .withVpcId(awsNetworkView.getExistingVpc())
                    .withTags(context.getTags());
            CreateTargetGroupResult targetGroupResult = createOrGetTargetGroup(context, targetGroupRequest);
            targetGroupArn = targetGroupResult.getTargetGroups()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getTargetGroupArn();
            targetGroup.setArn(targetGroupArn);
            targetGroupResource = new CloudResource.Builder()
                    .name(targetGroupName)
                    .type(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                    .reference(targetGroupArn)
                    .status(CommonStatus.CREATED)
                    .instanceId(loadBalancerArn)
                    .build();
            context.getPersistenceNotifier().notifyAllocation(targetGroupResource, context.getCloudContext());
        }
        context.setTargetGroupArn(targetGroupArn);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(targetGroupResource, ResourceStatus.CREATED);
        context.addResourceStatus(cloudResourceStatus);
    }

    private CreateTargetGroupResult createOrGetTargetGroup(ResourceCreationContext context, CreateTargetGroupRequest targetGroupRequest) {
        try {
            return context.getLoadBalancingClient().createTargetGroup(targetGroupRequest);
        } catch (AmazonServiceException amazonServiceException) {
            String errorCode = amazonServiceException.getErrorCode();
            if (StringUtils.isNotEmpty(errorCode) && errorCode.contains(DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE)) {
                DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest().withNames(targetGroupRequest.getName());
                DescribeTargetGroupsResult describeTargetGroupsResult = context.getLoadBalancingClient().describeTargetGroup(describeTargetGroupsRequest);
                return describeTargetGroupsResult.getTargetGroups()
                        .stream()
                        .findFirst()
                        .map(targetGroup -> new CreateTargetGroupResult().withTargetGroups(targetGroup))
                        .orElseThrow(() -> new CloudConnectorException(
                                String.format("Load balancer target group could not be created and found with name: '%s'", targetGroupRequest.getName())));
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void createListener(ResourceCreationContext context, AwsListener listener) {
        CloudResource listenerResource;
        Optional<CloudResource> existingLoadBalancer = persistenceRetriever.retrieveFirstByTypeAndStatusForStack(
                ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, CommonStatus.CREATED, context.getStackId());
        if (existingLoadBalancer.isPresent()) {
            listenerResource = existingLoadBalancer.get();
            LOGGER.info("Elastic load balancer listener resource has already been created for stack proceeding forward with existing resource '{}'",
                    listenerResource.getReference());
        } else {
            String loadBalancerArn = context.getLoadBalancerArn();
            String targetGroupArn = context.getTargetGroupArn();
            LOGGER.info("Creating listener for load balancer('{}') on target group('{}')", loadBalancerArn, targetGroupArn);
            Action defaultAction = new Action()
                    .withType(ActionTypeEnum.Forward)
                    .withOrder(1)
                    .withTargetGroupArn(targetGroupArn);
            CreateListenerRequest listenerRequest = new CreateListenerRequest()
                    .withLoadBalancerArn(loadBalancerArn)
                    .withProtocol(ProtocolEnum.TCP)
                    .withPort(listener.getPort())
                    .withDefaultActions(defaultAction)
                    .withTags(context.getTags());
            CreateListenerResult registerListenerResult = createOrGetListener(context, listenerRequest);
            String listenerArn = registerListenerResult.getListeners()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getListenerArn();
            listenerResource = new CloudResource.Builder()
                    .name(context.getTargetGroupName())
                    .type(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                    .reference(listenerArn)
                    .status(CommonStatus.CREATED)
                    .instanceId(loadBalancerArn)
                    .build();
            context.getPersistenceNotifier().notifyAllocation(listenerResource, context.getCloudContext());
        }
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(listenerResource, ResourceStatus.CREATED);
        context.addResourceStatus(cloudResourceStatus);
    }

    private CreateListenerResult createOrGetListener(ResourceCreationContext context, CreateListenerRequest listenerRequest) {
        try {
            return context.getLoadBalancingClient().registerListener(listenerRequest);
        } catch (AmazonServiceException amazonServiceException) {
            String errorCode = amazonServiceException.getErrorCode();
            if (StringUtils.isNotEmpty(errorCode) && errorCode.contains(DUPLICATE_LISTENER_ERROR_CODE)) {
                String loadBalancerArn = context.getLoadBalancerArn();
                DescribeListenersRequest describeListenersRequest = new DescribeListenersRequest().withLoadBalancerArn(loadBalancerArn);
                DescribeListenersResult describeListenersResult = context.getLoadBalancingClient().describeListeners(describeListenersRequest);
                return describeListenersResult.getListeners()
                        .stream()
                        .findFirst()
                        .map(listener -> new CreateListenerResult().withListeners(listener))
                        .orElseThrow(() -> new CloudConnectorException(
                                String.format("Load balancer listener could not be created and found with load balancer arn: '%s'", loadBalancerArn)));
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void registerTarget(AmazonElasticLoadBalancingClient loadBalancingClient, Long stackId, AwsTargetGroup targetGroup) {
        LOGGER.debug("Registering targets to target group '{}'", targetGroup.getArn());
        Set<TargetDescription> targetDescriptions = targetGroup.getInstanceIds().stream()
                .map(privateId -> {
                    String instanceId = persistenceRetriever.notifyRetrieve(stackId, privateId, CommonStatus.CREATED, ResourceType.AWS_INSTANCE)
                            .orElseThrow(() -> {
                                String notFoundMsg = String.format("No AWS instance resource found with private id('%s') for stack('%s')", privateId, stackId);
                                return new CloudConnectorException(notFoundMsg);
                            })
                            .getInstanceId();
                    return new TargetDescription()
                            .withPort(targetGroup.getPort())
                            .withId(instanceId);
                })
                .collect(toSet());
        RegisterTargetsRequest registerTargetsRequest = new RegisterTargetsRequest()
                .withTargetGroupArn(targetGroup.getArn())
                .withTargets(targetDescriptions);
        LOGGER.info("Registering target group of load balancer('{}') to instances: '{}'", targetGroup.getArn(),
                String.join(",", targetGroup.getInstanceIds()));
        loadBalancingClient.registerTargets(registerTargetsRequest);
    }

    private static class ResourceCreationContext {
        private final Long stackId;

        private final String stackName;

        private final Collection<Tag> tags;

        private final PersistenceNotifier persistenceNotifier;

        private final AmazonElasticLoadBalancingClient loadBalancingClient;

        private final CloudContext cloudContext;

        private String loadBalancerArn;

        private String targetGroupName;

        private String targetGroupArn;

        private List<CloudResourceStatus> resourceStatuses = new ArrayList<>();

        ResourceCreationContext(Long stackId, String stackName, Collection<Tag> tags, PersistenceNotifier persistenceNotifier,
                AmazonElasticLoadBalancingClient loadBalancingClient, CloudContext cloudContext) {
            this.stackId = stackId;
            this.stackName = stackName;
            this.tags = tags;
            this.persistenceNotifier = persistenceNotifier;
            this.loadBalancingClient = loadBalancingClient;
            this.cloudContext = cloudContext;
        }

        public Long getStackId() {
            return stackId;
        }

        public String getStackName() {
            return stackName;
        }

        public Collection<Tag> getTags() {
            return tags;
        }

        public PersistenceNotifier getPersistenceNotifier() {
            return persistenceNotifier;
        }

        public AmazonElasticLoadBalancingClient getLoadBalancingClient() {
            return loadBalancingClient;
        }

        public CloudContext getCloudContext() {
            return cloudContext;
        }

        public List<CloudResourceStatus> getResourceStatuses() {
            return resourceStatuses;
        }

        public void addResourceStatus(CloudResourceStatus cloudResourceStatus) {
            resourceStatuses.add(cloudResourceStatus);
        }

        public String getLoadBalancerArn() {
            return loadBalancerArn;
        }

        public void setLoadBalancerArn(String loadBalancerArn) {
            this.loadBalancerArn = loadBalancerArn;
        }

        public String getTargetGroupName() {
            return targetGroupName;
        }

        public void setTargetGroupName(String targetGroupName) {
            this.targetGroupName = targetGroupName;
        }

        public String getTargetGroupArn() {
            return targetGroupArn;
        }

        public void setTargetGroupArn(String targetGroupArn) {
            this.targetGroupArn = targetGroupArn;
        }
    }
}
