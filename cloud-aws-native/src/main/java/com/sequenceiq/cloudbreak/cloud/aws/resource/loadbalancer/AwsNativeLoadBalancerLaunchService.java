package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.IpAddressType;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsResult;
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
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCloudStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
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

    public List<CloudResourceStatus> launchLoadBalancerResources(AuthenticatedContext authenticatedContext, CloudStack stack,
            PersistenceNotifier persistenceNotifier, AmazonElasticLoadBalancingClient elasticLoadBalancingClient) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        Map<String, List<String>> privateIdsByGroupName = stack.getGroups()
                .stream()
                .collect(Collectors.toMap(Group::getName,
                        group -> group.getInstances().stream().map(in -> String.valueOf(in.getTemplate().getPrivateId())).collect(toList())));
        List<AwsLoadBalancer> loadBalancers = loadBalancerCommonService.getAwsLoadBalancers(stack.getLoadBalancers(), privateIdsByGroupName, awsNetworkView);
        AwsCloudStackView awsCloudStackView = new AwsCloudStackView(stack);
        Long stackId = authenticatedContext.getCloudContext().getId();


        Collection<Tag> tags = awsTaggingService.prepareElasticLoadBalancingTags(awsCloudStackView.getTags());
        try {
            for (AwsLoadBalancer awsLoadBalancer : loadBalancers) {
                String loadBalancerName = awsLoadBalancer.getName();
                Set<String> subnetIds = awsLoadBalancer.getSubnetIds();
                AwsLoadBalancerScheme scheme = awsLoadBalancer.getScheme();
                LOGGER.info("Creating load balancer with name '{}', subnet ids: '{}' and scheme: '{}'", loadBalancerName, String.join(",", subnetIds), scheme);
                CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
                        .withName(loadBalancerName)
                        .withSubnets(subnetIds)
                        .withScheme(scheme.awsScheme())
                        .withType(LoadBalancerTypeEnum.Network)
                        .withIpAddressType(IpAddressType.Ipv4)
                        .withTags(tags);
                CreateLoadBalancerResult loadBalancerResult = elasticLoadBalancingClient.registerLoadBalancer(request);

                String loadBalancerArn = loadBalancerResult.getLoadBalancers()
                        .stream()
                        .findFirst()
                        .orElseThrow()
                        .getLoadBalancerArn();
                CloudResource loadBalancerResource = new CloudResource.Builder()
                        .name(loadBalancerName)
                        .type(ResourceType.ELASTIC_LOAD_BALANCER)
                        .reference(loadBalancerArn)
                        .status(CommonStatus.CREATED)
                        .build();
                persistenceNotifier.notifyAllocation(loadBalancerResource, authenticatedContext.getCloudContext());
                result.add(new CloudResourceStatus(loadBalancerResource, ResourceStatus.CREATED));

                for (AwsListener listener : awsLoadBalancer.getListeners()) {
                    AwsTargetGroup targetGroup = listener.getTargetGroup();
                    String targetGroupName = targetGroup.getName();
                    LOGGER.info("Creating target group for load balancer('{}') with name: '{}' port: '{}'", loadBalancerName, targetGroupName,
                            targetGroup.getPort());
                    CreateTargetGroupRequest targetGroupRequest = new CreateTargetGroupRequest()
                            .withName(targetGroupName)
                            .withPort(targetGroup.getPort())
                            .withProtocol(ProtocolEnum.TCP)
                            .withTargetType(TargetTypeEnum.Instance)
                            .withHealthCheckPort(targetGroup.getHealthCheckPort())
                            .withHealthCheckIntervalSeconds(TARGET_GROUP_HEALTH_CHECK_INTERVAL_SECONDS)
                            .withHealthyThresholdCount(HEALTHY_THRESHOLD_COUNT)
                            .withUnhealthyThresholdCount(UNHEALTHY_THRESHOLD_COUNT)
                            .withVpcId(awsNetworkView.getExistingVpc())
                            .withTags(tags);
                    CreateTargetGroupResult targetGroupResult = elasticLoadBalancingClient.createTargetGroup(targetGroupRequest);


                    String targetGroupArn = targetGroupResult.getTargetGroups()
                            .stream()
                            .findFirst()
                            .orElseThrow()
                            .getTargetGroupArn();
                    CloudResource targetGroupResource = new CloudResource.Builder()
                            .name(targetGroupName)
                            .type(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                            .reference(targetGroupArn)
                            .status(CommonStatus.CREATED)
                            .instanceId(loadBalancerArn)
                            .build();
                    persistenceNotifier.notifyAllocation(targetGroupResource, authenticatedContext.getCloudContext());
                    result.add(new CloudResourceStatus(targetGroupResource, ResourceStatus.CREATED));

                    Action defaultAction = new Action()
                            .withType(ActionTypeEnum.Forward)
                            .withOrder(1)
                            .withTargetGroupArn(targetGroupArn);
                    CreateListenerRequest listenerRequest = new CreateListenerRequest()
                            .withLoadBalancerArn(loadBalancerArn)
                            .withProtocol(ProtocolEnum.TCP)
                            .withPort(listener.getPort())
                            .withDefaultActions(defaultAction)
                            .withTags(tags);
                    LOGGER.info("Creating listener for load balancer('{}') on target group('{}')", loadBalancerName, targetGroupArn);
                    CreateListenerResult registerListenerResult = elasticLoadBalancingClient.registerListener(listenerRequest);

                    String listenerArn = registerListenerResult.getListeners()
                            .stream()
                            .findFirst()
                            .orElseThrow()
                            .getListenerArn();

                    CloudResource listenerResource = new CloudResource.Builder()
                            .name(targetGroupName)
                            .type(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                            .reference(listenerArn)
                            .status(CommonStatus.CREATED)
                            .instanceId(loadBalancerArn)
                            .build();
                    persistenceNotifier.notifyAllocation(listenerResource, authenticatedContext.getCloudContext());
                    result.add(new CloudResourceStatus(listenerResource, ResourceStatus.CREATED));

                    Set<TargetDescription> targetDescriptions = targetGroup.getInstanceIds().stream()
                            .map(privateId -> {
                                String notFoundMsg = String.format("No AWS instance resource found with private id('%s') for stack('%s')", privateId, stackId);
                                String instanceId = persistenceRetriever.notifyRetrieve(stackId, privateId, CommonStatus.CREATED, ResourceType.AWS_INSTANCE)
                                        .orElseThrow(() -> new CloudConnectorException(notFoundMsg))
                                        .getInstanceId();
                                return new TargetDescription()
                                        .withPort(targetGroup.getPort())
                                        .withId(instanceId);
                            })
                            .collect(toSet());
                    RegisterTargetsRequest registerTargetsRequest = new RegisterTargetsRequest()
                            .withTargetGroupArn(targetGroupArn)
                            .withTargets(targetDescriptions);
                    LOGGER.info("Registering target group of load balancer('{}') to instances: '{}'", loadBalancerName,
                            String.join(",", targetGroup.getInstanceIds()));
                    RegisterTargetsResult registerTargetsResult = elasticLoadBalancingClient.registerTargets(registerTargetsRequest);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Load balancer and it's resources could not be created", ex);
            throw new CloudConnectorException(ex);
        }
        return result;
    }
}
