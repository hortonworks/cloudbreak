package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.IpAddressType;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetTypeEnum;

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
    private ResourceRetriever resourceRetriever;

    @Inject
    private AwsResourceNameService resourceNameService;

    public List<CloudResourceStatus> launchLoadBalancerResources(AuthenticatedContext authenticatedContext, CloudStack stack,
            PersistenceNotifier persistenceNotifier, AmazonElasticLoadBalancingClient loadBalancingClient, boolean registerTargetGroups) {
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
                if (registerTargetGroups) {
                    for (AwsListener listener : awsLoadBalancer.getListeners()) {
                        AwsTargetGroup targetGroup = listener.getTargetGroup();
                        creationContext.setTargetGroupName(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, stackName,
                                awsLoadBalancer.getScheme().resourceName(), targetGroup.getPort()));
                        createTargetGroup(creationContext, awsNetworkView, targetGroup);
                        createListener(creationContext, listener);
                        registerTarget(loadBalancingClient, stackId, targetGroup);
                    }
                }
            }
        } catch (Exception ex) {
            String message = "Load balancer and it's resources could not be created. " + ex.getMessage();
            LOGGER.warn(message, ex);
            throw new CloudConnectorException(message, ex);
        }
        return creationContext.getResourceStatuses();
    }

    private void createLoadBalancer(ResourceCreationContext context, AwsLoadBalancer awsLoadBalancer) {
        CloudResource loadBalancerResource;
        AwsLoadBalancerScheme scheme = awsLoadBalancer.getScheme();
        String loadBalancerName = resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, context.getStackName(), scheme.resourceName());
        String loadBalancerNameWithoutHash = resourceNameService.trimHash(loadBalancerName);
        LOGGER.info("Looking for elastic load balancer resource for stack with resource name prefix '{}'", loadBalancerNameWithoutHash);
        List<CloudResource> existingLoadBalancers = resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, context.getStackId());
        LOGGER.info("Existing elastic load balancer resources for stack: {}", existingLoadBalancers);
        Optional<CloudResource> existingLoadBalancer = existingLoadBalancers.stream()
                .filter(cloudResource -> loadBalancerNameWithoutHash.equals(resourceNameService.trimHash(cloudResource.getName())))
                .findFirst();
        String loadBalancerArn;
        if (existingLoadBalancer.isPresent()) {
            loadBalancerResource = existingLoadBalancer.get();
            loadBalancerArn = loadBalancerResource.getReference();
            LOGGER.info("Elastic load balancer resource has already been created for stack, proceeding forward with existing resource '{}'",
                    loadBalancerArn);
        } else {
            Set<String> subnetIds = awsLoadBalancer.getSubnetIds();
            LOGGER.info("Creating load balancer with name '{}', subnet ids: '{}' and scheme: '{}'", loadBalancerName, String.join(",", subnetIds), scheme);
            CreateLoadBalancerRequest request = CreateLoadBalancerRequest.builder()
                    .name(loadBalancerName)
                    .subnets(subnetIds)
                    .scheme(scheme.awsScheme())
                    .type(LoadBalancerTypeEnum.NETWORK)
                    .ipAddressType(IpAddressType.IPV4)
                    .tags(context.getTags())
                    .build();
            CreateLoadBalancerResponse loadBalancerResponse = createOrGetLoadBalancer(context, request);
            loadBalancerArn = loadBalancerResponse.loadBalancers()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .loadBalancerArn();
            context.setLoadBalancerArn(loadBalancerArn);
            Map<String, Object> params = Map.of(CloudResource.ATTRIBUTES,
                    Enum.valueOf(LoadBalancerTypeAttribute.class, awsLoadBalancer.getScheme().getLoadBalancerType().name()));
            loadBalancerResource = CloudResource.builder()
                    .withName(loadBalancerName)
                    .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                    .withReference(loadBalancerArn)
                    .withStatus(CommonStatus.CREATED)
                    .withParameters(params)
                    .build();
            context.getPersistenceNotifier().notifyAllocation(loadBalancerResource, context.getCloudContext());
        }
        context.setLoadBalancerArn(loadBalancerArn);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(loadBalancerResource, ResourceStatus.CREATED);
        context.addResourceStatus(cloudResourceStatus);
    }

    private CreateLoadBalancerResponse createOrGetLoadBalancer(ResourceCreationContext context, CreateLoadBalancerRequest request) {
        try {
            return context.getLoadBalancingClient().registerLoadBalancer(request);
        } catch (AwsServiceException amazonServiceException) {
            String errorCode = amazonServiceException.awsErrorDetails().errorCode();
            if (StringUtils.isNotEmpty(errorCode) && errorCode.contains(DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE)) {
                DescribeLoadBalancersRequest describeLoadBalancersRequest = DescribeLoadBalancersRequest.builder().names(request.name()).build();
                DescribeLoadBalancersResponse describeLoadBalancersResponse = context.getLoadBalancingClient()
                        .describeLoadBalancers(describeLoadBalancersRequest);
                return describeLoadBalancersResponse.loadBalancers()
                        .stream()
                        .findFirst()
                        .map(loadBalancer -> CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build())
                        .orElseThrow(() -> new CloudConnectorException(
                                String.format("Load balancer could not be created and found with name: '%s'", request.name())));
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void createTargetGroup(ResourceCreationContext context, AwsNetworkView awsNetworkView, AwsTargetGroup targetGroup) {
        CloudResource targetGroupResource;
        String targetGroupName = context.getTargetGroupName();
        String targetGroupNameWithoutHash = resourceNameService.trimHash(targetGroupName);
        LOGGER.info("Looking for elastic load balancer target group resource for stack with resource name prefix '{}'", targetGroupNameWithoutHash);
        List<CloudResource> existingTargetGroups = resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, context.getStackId());
        LOGGER.info("Existing elastic load balancer target group resources for stack: {}", existingTargetGroups);
        Optional<CloudResource> existingTargetGroup = existingTargetGroups.stream()
                .filter(cloudResource -> targetGroupNameWithoutHash.equals(resourceNameService.trimHash(cloudResource.getName())))
                .findFirst();
        String targetGroupArn;
        if (existingTargetGroup.isPresent()) {
            targetGroupResource = existingTargetGroup.get();
            targetGroupArn = targetGroupResource.getReference();
            targetGroup.setArn(targetGroupArn);
            LOGGER.info("Elastic load balancer target group resource has already been created for stack, proceeding forward with existing resource '{}'",
                    targetGroupArn);
        } else {
            int targetGroupPort = targetGroup.getPort();
            String loadBalancerArn = context.getLoadBalancerArn();
            LOGGER.info("Creating target group for load balancer('{}') with name: '{}' port: '{}'", loadBalancerArn, targetGroupName,
                    targetGroupPort);
            CreateTargetGroupRequest targetGroupRequest = CreateTargetGroupRequest.builder()
                    .name(targetGroupName)
                    .port(targetGroupPort)
                    .protocol(ProtocolEnum.TCP)
                    .targetType(TargetTypeEnum.INSTANCE)
                    .healthCheckPort(targetGroup.getHealthCheckPort())
                    .healthCheckIntervalSeconds(TARGET_GROUP_HEALTH_CHECK_INTERVAL_SECONDS)
                    .healthyThresholdCount(HEALTHY_THRESHOLD_COUNT)
                    .unhealthyThresholdCount(UNHEALTHY_THRESHOLD_COUNT)
                    .vpcId(awsNetworkView.getExistingVpc())
                    .tags(context.getTags())
                    .build();
            CreateTargetGroupResponse targetGroupResponse = createOrGetTargetGroup(context, targetGroupRequest);
            targetGroupArn = targetGroupResponse.targetGroups()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .targetGroupArn();
            targetGroup.setArn(targetGroupArn);
            targetGroupResource = CloudResource.builder()
                    .withName(targetGroupName)
                    .withType(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                    .withReference(targetGroupArn)
                    .withStatus(CommonStatus.CREATED)
                    .withInstanceId(loadBalancerArn)
                    .build();
            context.getPersistenceNotifier().notifyAllocation(targetGroupResource, context.getCloudContext());
        }
        context.setTargetGroupArn(targetGroupArn);
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(targetGroupResource, ResourceStatus.CREATED);
        context.addResourceStatus(cloudResourceStatus);
    }

    private CreateTargetGroupResponse createOrGetTargetGroup(ResourceCreationContext context, CreateTargetGroupRequest targetGroupRequest) {
        try {
            return context.getLoadBalancingClient().createTargetGroup(targetGroupRequest);
        } catch (AwsServiceException amazonServiceException) {
            String errorCode = amazonServiceException.awsErrorDetails().errorCode();
            if (StringUtils.isNotEmpty(errorCode) && errorCode.contains(DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE)) {
                DescribeTargetGroupsRequest describeTargetGroupsRequest = DescribeTargetGroupsRequest.builder().names(targetGroupRequest.name()).build();
                DescribeTargetGroupsResponse describeTargetGroupsResponse = context.getLoadBalancingClient().describeTargetGroup(describeTargetGroupsRequest);
                return describeTargetGroupsResponse.targetGroups()
                        .stream()
                        .findFirst()
                        .map(targetGroup -> CreateTargetGroupResponse.builder().targetGroups(targetGroup).build())
                        .orElseThrow(() -> new CloudConnectorException(
                                String.format("Load balancer target group could not be created and found with name: '%s'", targetGroupRequest.name())));
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void createListener(ResourceCreationContext context, AwsListener listener) {
        CloudResource listenerResource;
        String targetGroupName = context.getTargetGroupName();
        String targetGroupNameWithoutHash = resourceNameService.trimHash(targetGroupName);
        LOGGER.info("Looking for elastic load balancer listener resource for stack with resource name prefix '{}'", targetGroupNameWithoutHash);
        List<CloudResource> existingListeners = resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, context.getStackId());
        LOGGER.info("Existing elastic load balancer listener resources for stack: {}", existingListeners);
        Optional<CloudResource> existingListener = existingListeners.stream()
                .filter(cloudResource -> targetGroupNameWithoutHash.equals(resourceNameService.trimHash(cloudResource.getName())))
                .findFirst();
        if (existingListener.isPresent()) {
            listenerResource = existingListener.get();
            LOGGER.info("Elastic load balancer listener resource has already been created for stack, proceeding forward with existing resource '{}'",
                    listenerResource.getReference());
        } else {
            String loadBalancerArn = context.getLoadBalancerArn();
            String targetGroupArn = context.getTargetGroupArn();
            LOGGER.info("Creating listener for load balancer('{}') on target group('{}')", loadBalancerArn, targetGroupArn);
            Action defaultAction = Action.builder()
                    .type(ActionTypeEnum.FORWARD)
                    .order(1)
                    .targetGroupArn(targetGroupArn)
                    .build();
            CreateListenerRequest listenerRequest = CreateListenerRequest.builder()
                    .loadBalancerArn(loadBalancerArn)
                    .protocol(ProtocolEnum.TCP)
                    .port(listener.getPort())
                    .defaultActions(defaultAction)
                    .tags(context.getTags())
                    .build();
            CreateListenerResponse registerListenerResponse = createOrGetListener(context, listenerRequest);
            String listenerArn = registerListenerResponse.listeners()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .listenerArn();
            listenerResource = CloudResource.builder()
                    .withName(targetGroupName)
                    .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                    .withReference(listenerArn)
                    .withStatus(CommonStatus.CREATED)
                    .withInstanceId(loadBalancerArn)
                    .build();
            context.getPersistenceNotifier().notifyAllocation(listenerResource, context.getCloudContext());
        }
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(listenerResource, ResourceStatus.CREATED);
        context.addResourceStatus(cloudResourceStatus);
    }

    private CreateListenerResponse createOrGetListener(ResourceCreationContext context, CreateListenerRequest listenerRequest) {
        try {
            return context.getLoadBalancingClient().registerListener(listenerRequest);
        } catch (AwsServiceException amazonServiceException) {
            String errorCode = amazonServiceException.awsErrorDetails().errorCode();
            if (StringUtils.isNotEmpty(errorCode) && errorCode.contains(DUPLICATE_LISTENER_ERROR_CODE)) {
                String loadBalancerArn = context.getLoadBalancerArn();
                DescribeListenersRequest describeListenersRequest = DescribeListenersRequest.builder().loadBalancerArn(loadBalancerArn).build();
                DescribeListenersResponse describeListenersResponse = context.getLoadBalancingClient().describeListeners(describeListenersRequest);
                return describeListenersResponse.listeners()
                        .stream()
                        .findFirst()
                        .map(listener -> CreateListenerResponse.builder().listeners(listener).build())
                        .orElseThrow(() -> new CloudConnectorException(
                                String.format("Load balancer listener could not be created and found with load balancer arn: '%s'", loadBalancerArn)));
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void registerTarget(AmazonElasticLoadBalancingClient loadBalancingClient, Long stackId, AwsTargetGroup targetGroup) {
        LOGGER.debug("Registering targets to target group '{}'", targetGroup.getArn());
        Set<String> privateIds = targetGroup.getInstanceIds();
        List<CloudResource> cloudResources = persistenceRetriever.notifyRetrieveAll(stackId, privateIds, CommonStatus.CREATED, ResourceType.AWS_INSTANCE);
        if (privateIds.size() != cloudResources.size()) {
            List<String> cloudResourceReferences = cloudResources.stream()
                    .filter(cr -> cr.getReference() != null)
                    .map(CloudResource::getReference)
                    .collect(toList());
            Set<String> missingPrivateIds = new HashSet<>(privateIds);
            cloudResourceReferences.forEach(missingPrivateIds::remove);
            LOGGER.debug("The following resources are missing from DB: {}", missingPrivateIds);
        }
        Set<TargetDescription> targetDescriptions = cloudResources.stream()
                .map(cloudResource -> {
                    String instanceId = cloudResource.getInstanceId();
                    return TargetDescription.builder()
                            .port(targetGroup.getPort())
                            .id(instanceId)
                            .build();
                })
                .collect(toSet());
        RegisterTargetsRequest registerTargetsRequest = RegisterTargetsRequest.builder()
                .targetGroupArn(targetGroup.getArn())
                .targets(targetDescriptions)
                .build();
        LOGGER.info("Registering target group ('{}') of load balancer to instances: '{}'", targetGroup.getArn(),
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

        private final List<CloudResourceStatus> resourceStatuses = new ArrayList<>();

        private String loadBalancerArn;

        private String targetGroupName;

        private String targetGroupArn;

        ResourceCreationContext(Long stackId, String stackName, Collection<Tag> tags, PersistenceNotifier persistenceNotifier,
                AmazonElasticLoadBalancingClient loadBalancingClient, CloudContext cloudContext) {
            this.stackId = stackId;
            this.stackName = stackName;
            this.tags = tags;
            this.persistenceNotifier = persistenceNotifier;
            this.loadBalancingClient = loadBalancingClient;
            this.cloudContext = cloudContext;
        }

        public void addResourceStatus(CloudResourceStatus cloudResourceStatus) {
            resourceStatuses.add(cloudResourceStatus);
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
