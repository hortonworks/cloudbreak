package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.INSTANCE_TYPE;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;

@Service
public class CloudFormationStackUtil {

    private static final Logger LOGGER = getLogger(CloudFormationStackUtil.class);

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public String getAutoscalingGroupName(AuthenticatedContext ac, String instanceGroup, String region) {
        AmazonCloudFormationClient amazonCfClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
        return getAutoscalingGroupName(ac, amazonCfClient, instanceGroup);
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public String getAutoscalingGroupName(AuthenticatedContext ac, AmazonCloudFormationClient amazonCFClient, String instanceGroup) {
        String cFStackName = getCfStackName(ac);
        DescribeStackResourceResponse asGroupResource = amazonCFClient.describeStackResource(DescribeStackResourceRequest.builder()
                .stackName(cFStackName)
                .logicalResourceId(String.format("AmbariNodes%s", instanceGroup.replaceAll("_", "")))
                .build());
        return asGroupResource.stackResourceDetail().physicalResourceId();
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public List<String> getLoadbalancersArns(AuthenticatedContext ac, AmazonCloudFormationClient amazonCFClient) {
        String cFStackName = getCfStackName(ac);
        ListStackResourcesResponse listStackResourcesResponse = amazonCFClient.listStackResources(ListStackResourcesRequest.builder()
                .stackName(cFStackName)
                .build());
        List<StackResourceSummary> summaries = listStackResourcesResponse.stackResourceSummaries();
        List<StackResourceSummary> loadBalancerSummary = summaries.stream()
                .filter(stackResourceSummary -> stackResourceSummary.resourceType().equals("AWS::ElasticLoadBalancingV2::LoadBalancer"))
                .collect(Collectors.toList());
        return loadBalancerSummary.stream()
                .map(e -> e.physicalResourceId())
                .collect(Collectors.toList());
    }

    public Map<String, String> getOutputs(String cFStackName, AmazonCloudFormationClient client) {
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(cFStackName).build();
        String outputNotFound = String.format("Couldn't get Cloudformation stack's('%s') output", cFStackName);
        List<Output> cfStackOutputs = client.describeStacks(describeStacksRequest).stacks()
                .stream().findFirst().orElseThrow(getCloudConnectorExceptionSupplier(outputNotFound)).outputs();
        return cfStackOutputs.stream().collect(Collectors.toMap(Output::outputKey, Output::outputValue));
    }

    private Supplier<CloudConnectorException> getCloudConnectorExceptionSupplier(String msg) {
        return () -> new CloudConnectorException(msg);
    }

    public List<CloudResource> getInstanceCloudResources(AuthenticatedContext ac, AmazonCloudFormationClient client,
            AmazonAutoScalingClient amazonASClient, List<Group> groups) {
        Map<String, Group> groupNameMapping = getAutoscalingGroupNameToCloudStackGroup(ac, client, groups);
        Map<Group, List<String>> idsByGroups = getInstanceIdsByGroups(amazonASClient, groupNameMapping);
        return idsByGroups.entrySet().stream()
                .flatMap(entry -> {
                    Group group = entry.getKey();
                    List<CloudResource> cloudResources = new ArrayList<>();
                    Iterator<CloudInstance> groupInstancesIterator = group.getInstances().stream()
                            .filter(cloudInstance -> cloudInstance.getInstanceId() == null)
                            .collect(Collectors.toList())
                            .iterator();
                    List<String> knownInstanceIds = group.getInstances().stream().map(CloudInstance::getInstanceId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    List<String> newInstanceIds = entry.getValue().stream().filter(s -> !knownInstanceIds.contains(s)).collect(Collectors.toList());
                    for (String instanceId : newInstanceIds) {
                        CloudResource cloudResource = CloudResource.builder()
                                .withType(ResourceType.AWS_INSTANCE)
                                .withInstanceId(instanceId)
                                .withName(instanceId)
                                .withGroup(group.getName())
                                .withStatus(CommonStatus.CREATED)
                                .withAvailabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                                .withPersistent(false)
                                .build();
                        if (groupInstancesIterator.hasNext()) {
                            CloudInstance cloudInstance = groupInstancesIterator.next();
                            cloudResource.putParameter(PRIVATE_ID, cloudInstance.getTemplate().getPrivateId());
                            cloudResource.putParameter(INSTANCE_TYPE, cloudInstance.getTemplate().getFlavor());
                        }
                        cloudResources.add(cloudResource);
                    }
                    return cloudResources.stream();
                })
                .collect(Collectors.toList());
    }

    private Map<String, Group> getAutoscalingGroupNameToCloudStackGroup(AuthenticatedContext ac, AmazonCloudFormationClient client, List<Group> groups) {
        Map<String, Group> groupNameMapping = groups.stream()
                .collect(Collectors.toMap(
                        group -> getAutoscalingGroupName(ac, client, group.getName()),
                        group -> group
                ));
        return groupNameMapping;
    }

    public Map<String, String> getGroupNameToAutoscalingGroupName(AuthenticatedContext ac, AmazonCloudFormationClient client, List<Group> groups) {
        Map<String, String> groupNameMapping = groups.stream()
                .collect(Collectors.toMap(
                        Group::getName,
                        group -> getAutoscalingGroupName(ac, client, group.getName())
                ));
        return groupNameMapping;
    }

    public CloudResource getCloudFormationStackResource(Iterable<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getType().equals(ResourceType.CLOUDFORMATION_STACK)) {
                return cloudResource;
            }
        }
        return null;
    }

    public String getCfStackName(AuthenticatedContext ac) {
        return String.format("%s-%s", Splitter.fixedLength(maxResourceNameLength - (ac.getCloudContext().getId().toString().length() + 1))
                .splitToList(ac.getCloudContext().getName()).get(0), ac.getCloudContext().getId());
    }

    public Map<Group, List<String>> getInstanceIdsByGroups(AmazonAutoScalingClient amazonASClient, Map<String, Group> groupNameMapping) {
        DescribeAutoScalingGroupsResponse result = amazonASClient
                .describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(groupNameMapping.keySet()).build());
        return result.autoScalingGroups().stream()
                .collect(Collectors.toMap(
                        ag -> groupNameMapping.get(ag.autoScalingGroupName()),
                        ag -> ag.instances().stream()
                                .filter(instance ->  LifecycleState.IN_SERVICE == instance.lifecycleState())
                                .map(Instance::instanceId)
                                .collect(Collectors.toList())));
    }

    public List<String> getInstanceIds(AmazonAutoScalingClient amazonASClient, String asGroupName) {
        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse = amazonASClient
                .describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(asGroupName).build());
        List<String> instanceIds = new ArrayList<>();
        if (!describeAutoScalingGroupsResponse.autoScalingGroups().isEmpty()
                && describeAutoScalingGroupsResponse.autoScalingGroups().get(0).instances() != null) {
            Map<String, LifecycleState> instanceIdsNotInService = new HashMap<>();
            for (Instance instance : describeAutoScalingGroupsResponse.autoScalingGroups().get(0).instances()) {
                if (LifecycleState.IN_SERVICE == instance.lifecycleState()) {
                    instanceIds.add(instance.instanceId());
                } else {
                    instanceIdsNotInService.put(instance.instanceId(), instance.lifecycleState());
                }
            }
            if (!instanceIdsNotInService.isEmpty()) {
                LOGGER.debug("Following instances are not in service  : {}", String.join(",",
                        instanceIdsNotInService.entrySet().stream().map(k -> k.getKey() + ": " + k.getValue()).collect(Collectors.toList())));
            }
        } else {
            LOGGER.debug("Autoscaling group ({}) is empty", asGroupName);
        }
        return instanceIds;
    }

    public DescribeInstancesRequest createDescribeInstancesRequest(Collection<String> instanceIds) {
        return DescribeInstancesRequest.builder().instanceIds(instanceIds).build();
    }

    public LoadBalancer getLoadBalancerByLogicalId(AuthenticatedContext ac, String logicalId) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonElasticLoadBalancingClient amazonElbClient =
                awsClient.createElasticLoadBalancingClient(new AwsCredentialView(ac.getCloudCredential()), region);

        String loadBalancerArn = getResourceArnByLogicalId(ac, logicalId, region);

        DescribeLoadBalancersResponse loadBalancersResponse = amazonElbClient.describeLoadBalancers(DescribeLoadBalancersRequest.builder()
                .loadBalancerArns(Collections.singletonList(loadBalancerArn))
                .build());

        return loadBalancersResponse.loadBalancers().get(0);
    }

    public void addLoadBalancerTargets(AuthenticatedContext ac, CloudLoadBalancer loadBalancer, List<CloudResource> resourcesToAdd) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonElasticLoadBalancingClient amazonElbClient =
                awsClient.createElasticLoadBalancingClient(new AwsCredentialView(ac.getCloudCredential()), region);

        for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : loadBalancer.getPortToTargetGroupMapping().entrySet()) {
            // Get a list of the new instances in the target groups
            Set<String> updatedInstanceIds = getInstanceIdsForGroups(resourcesToAdd, entry.getValue());

            // Find target group ARN
            AwsLoadBalancerScheme scheme = loadBalancerTypeConverter.convert(loadBalancer.getType());
            String targetGroupArn = getResourceArnByLogicalId(ac, AwsTargetGroup.getTargetGroupName(entry.getKey().getTrafficPort(), scheme), region);

            // Use ARN to fetch a list of current targets
            DescribeTargetHealthResponse targetHealthResponse = amazonElbClient.describeTargetHealth(DescribeTargetHealthRequest.builder()
                    .targetGroupArn(targetGroupArn)
                    .build());
            List<TargetDescription> targetDescriptions = targetHealthResponse.targetHealthDescriptions().stream()
                    .map(TargetHealthDescription::target)
                    .collect(Collectors.toList());
            Set<String> alreadyRegisteredInstanceIds = targetDescriptions.stream()
                    .map(TargetDescription::id)
                    .collect(Collectors.toSet());

            // Remove any targets that have already been registered from the list being processed
            updatedInstanceIds.removeAll(alreadyRegisteredInstanceIds);

            // Register any new instances
            if (!updatedInstanceIds.isEmpty()) {
                List<TargetDescription> targetsToAdd = updatedInstanceIds.stream()
                        .map(instanceId -> TargetDescription.builder().id(instanceId).build())
                        .collect(Collectors.toList());
                RegisterTargetsResponse registerTargetsResponse = amazonElbClient.registerTargets(RegisterTargetsRequest.builder()
                        .targetGroupArn(targetGroupArn)
                        .targets(targetsToAdd)
                        .build());
            }
        }
    }

    public boolean isCfStackExists(AmazonCloudFormationClient cfClient, String stackName) {
        try {
            cfClient.describeStacks(DescribeStacksRequest.builder().stackName(stackName).build());
            LOGGER.debug("CF stack exists with name: {}", stackName);
            return true;
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorMessage().contains(stackName + " does not exist")) {
                LOGGER.debug("CF stack does not exist with name: {}", stackName);
                return false;
            } else if (HttpStatus.valueOf(e.statusCode()).is4xxClientError()) {
                LOGGER.error("Cannot describe the CF stack. {}", e.awsErrorDetails().errorMessage(), e);
                throw e;
            }
            throw new Retry.ActionFailedException(e.getMessage(), e);
        }
    }

    //our metadata on the Group might not have the latest set of instances, but if the name matches it is being added
    private Set<String> getInstanceIdsForGroups(List<CloudResource> resources, Set<Group> groups) {
        List<String> groupNames = groups.stream().map(Group::getName).collect(Collectors.toList());
        return resources.stream()
                .filter(instance -> instance.getInstanceId() != null)
                .filter(resource -> groupNames.contains(resource.getGroup()))
                .map(CloudResource::getInstanceId)
                .collect(Collectors.toSet());
    }

    public String getResourceArnByLogicalId(AuthenticatedContext ac, String logicalId, String region) {
        String cFStackName = getCfStackName(ac);
        AmazonCloudFormationClient amazonCfClient =
                awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
        DescribeStackResourceResponse result = amazonCfClient.describeStackResource(DescribeStackResourceRequest.builder()
                .stackName(cFStackName)
                .logicalResourceId(logicalId)
                .build());
        return result.stackResourceDetail().physicalResourceId();
    }
}
