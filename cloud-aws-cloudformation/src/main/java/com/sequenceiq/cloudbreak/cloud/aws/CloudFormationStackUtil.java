package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsResult;
import com.amazonaws.services.elasticfilesystem.model.FileSystemDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.InvalidTargetException;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class CloudFormationStackUtil {

    private static final String INSTANCE_LIFECYCLE_IN_SERVICE = "InService";

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
        DescribeStackResourceResult asGroupResource = amazonCFClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(cFStackName)
                .withLogicalResourceId(String.format("AmbariNodes%s", instanceGroup.replaceAll("_", ""))));
        return asGroupResource.getStackResourceDetail().getPhysicalResourceId();
    }

    public Map<String, String> getOutputs(String cFStackName, AmazonCloudFormationClient client) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
        String outputNotFound = String.format("Couldn't get Cloudformation stack's('%s') output", cFStackName);
        List<Output> cfStackOutputs = client.describeStacks(describeStacksRequest).getStacks()
                .stream().findFirst().orElseThrow(getCloudConnectorExceptionSupplier(outputNotFound)).getOutputs();
        return cfStackOutputs.stream().collect(Collectors.toMap(Output::getOutputKey, Output::getOutputValue));
    }

    private Supplier<CloudConnectorException> getCloudConnectorExceptionSupplier(String msg) {
        return () -> new CloudConnectorException(msg);
    }

    public List<CloudResource> getInstanceCloudResources(AuthenticatedContext ac, AmazonCloudFormationClient client,
            AmazonAutoScalingClient amazonASClient, List<Group> groups) {
        Map<String, Group> groupNameMapping = groups.stream()
                .collect(Collectors.toMap(
                        group -> getAutoscalingGroupName(ac, client, group.getName()),
                        group -> group
                ));

        Map<Group, List<String>> idsByGroups = getInstanceIdsByGroups(amazonASClient, groupNameMapping);
        return idsByGroups.entrySet().stream()
                .flatMap(entry -> {
                    Group group = entry.getKey();
                    return entry.getValue().stream()
                            .map(id -> CloudResource.builder()
                                    .type(ResourceType.AWS_INSTANCE)
                                    .instanceId(id)
                                    .name(id)
                                    .group(group.getName())
                                    .status(CommonStatus.CREATED)
                                    .availabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                                    .persistent(false)
                                    .build());
                })
                .collect(Collectors.toList());
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
        DescribeAutoScalingGroupsResult result = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(groupNameMapping.keySet()));
        return result.getAutoScalingGroups().stream()
                .collect(Collectors.toMap(
                        ag -> groupNameMapping.get(ag.getAutoScalingGroupName()),
                        ag -> ag.getInstances().stream()
                                .filter(instance -> INSTANCE_LIFECYCLE_IN_SERVICE.equals(instance.getLifecycleState()))
                                .map(Instance::getInstanceId)
                                .collect(Collectors.toList())));
    }

    public List<String> getInstanceIds(AmazonAutoScalingClient amazonASClient, String asGroupName) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asGroupName));
        List<String> instanceIds = new ArrayList<>();
        if (!describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()
                && describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances() != null) {
            for (Instance instance : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()) {
                if (INSTANCE_LIFECYCLE_IN_SERVICE.equals(instance.getLifecycleState())) {
                    instanceIds.add(instance.getInstanceId());
                }
            }
        }
        return instanceIds;
    }

    public DescribeInstancesRequest createDescribeInstancesRequest(Collection<String> instanceIds) {
        return new DescribeInstancesRequest().withInstanceIds(instanceIds);
    }

    public LoadBalancer getLoadBalancerByLogicalId(AuthenticatedContext ac, String logicalId) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonElasticLoadBalancingClient amazonElbClient =
                awsClient.createElasticLoadBalancingClient(new AwsCredentialView(ac.getCloudCredential()), region);

        String loadBalancerArn = getResourceArnByLogicalId(ac, logicalId, region);

        DescribeLoadBalancersResult loadBalancersResult = amazonElbClient.describeLoadBalancers(new DescribeLoadBalancersRequest()
                .withLoadBalancerArns(Collections.singletonList(loadBalancerArn)));

        return loadBalancersResult.getLoadBalancers().get(0);
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
            DescribeTargetHealthResult targetHealthResult = amazonElbClient.describeTargetHealth(new DescribeTargetHealthRequest()
                    .withTargetGroupArn(targetGroupArn));
            List<TargetDescription> targetDescriptions = targetHealthResult.getTargetHealthDescriptions().stream()
                    .map(TargetHealthDescription::getTarget)
                    .collect(Collectors.toList());
            Set<String> alreadyRegisteredInstanceIds = targetDescriptions.stream()
                    .map(TargetDescription::getId)
                    .collect(Collectors.toSet());

            // Remove any targets that have already been registered from the list being processed
            updatedInstanceIds.removeAll(alreadyRegisteredInstanceIds);

            // Register any new instances
            if (!updatedInstanceIds.isEmpty()) {
                List<TargetDescription> targetsToAdd = updatedInstanceIds.stream()
                        .map(instanceId -> new TargetDescription().withId(instanceId))
                        .collect(Collectors.toList());
                RegisterTargetsResult registerTargetsResult = amazonElbClient.registerTargets(new RegisterTargetsRequest()
                        .withTargetGroupArn(targetGroupArn)
                        .withTargets(targetsToAdd));
            }
        }
    }

    public void removeLoadBalancerTargets(AuthenticatedContext ac, CloudLoadBalancer loadBalancer, List<CloudResource> resourcesToRemove) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonElasticLoadBalancingClient amazonElbClient =
                awsClient.createElasticLoadBalancingClient(new AwsCredentialView(ac.getCloudCredential()), region);

        for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : loadBalancer.getPortToTargetGroupMapping().entrySet()) {
            // Get a list of the instance ids to remove
            Set<String> instancesToRemove = getInstanceIdsForGroups(resourcesToRemove, entry.getValue());

            // Find target group ARN
            AwsLoadBalancerScheme scheme = loadBalancerTypeConverter.convert(loadBalancer.getType());
            String targetGroupArn = getResourceArnByLogicalId(ac, AwsTargetGroup.getTargetGroupName(entry.getKey().getTrafficPort(), scheme), region);

            // Deregister any instances that no longer exist
            if (!instancesToRemove.isEmpty()) {
                try {
                    List<TargetDescription> targetsToRemove = instancesToRemove.stream()
                            .map(instanceId -> new TargetDescription().withId(instanceId))
                            .collect(Collectors.toList());
                    DeregisterTargetsResult deregisterTargetsResult = amazonElbClient.deregisterTargets(new DeregisterTargetsRequest()
                            .withTargetGroupArn(targetGroupArn)
                            .withTargets(targetsToRemove));
                } catch (InvalidTargetException ignored) {
                    // no-op - we tried to remove a target that wasn't in the target group, which is fine
                }
            }
        }
    }

    public FileSystemDescription getEfsByFileSystemId(AuthenticatedContext ac, String fileSystemId) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEfsClient amazonEfsClient =
                awsClient.createElasticFileSystemClient(new AwsCredentialView(ac.getCloudCredential()), region);

        DescribeFileSystemsResult efsResult = amazonEfsClient.describeFileSystems(new DescribeFileSystemsRequest()
                .withFileSystemId(fileSystemId));

        return efsResult.getFileSystems().get(0);
    }

    private Set<String> getInstanceIdsForGroups(List<CloudResource> resources, Set<Group> groups) {
        return resources.stream()
            .filter(instance -> instance.getInstanceId() != null)
            .map(CloudResource::getInstanceId)
            .collect(Collectors.toSet());
    }

    private String getResourceArnByLogicalId(AuthenticatedContext ac, String logicalId, String region) {
        String cFStackName = getCfStackName(ac);
        AmazonCloudFormationClient amazonCfClient =
            awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
        DescribeStackResourceResult result = amazonCfClient.describeStackResource(new DescribeStackResourceRequest()
            .withStackName(cFStackName)
            .withLogicalResourceId(logicalId));
        return result.getStackResourceDetail().getPhysicalResourceId();
    }
}