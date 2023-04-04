package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Service
public class AwsDownscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsDownscaleService.class);

    private static final int MAX_DETACH_INSTANCE_SIZE = 20;

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    @Inject
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Inject
    private LoadBalancerService loadBalancerService;

    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vmsToDownscale) {
        if (!vmsToDownscale.isEmpty()) {
            List<String> instanceIdsToDownscale = new ArrayList<>();
            for (CloudInstance vm : vmsToDownscale) {
                instanceIdsToDownscale.add(vm.getInstanceId());
            }

            AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(auth);
            String regionName = authenticatedContextView.getRegion();
            LOGGER.debug("Calling deleteCloudWatchAlarmsForSystemFailures from AwsDownscaleService");
            awsCloudWatchService.deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIdsToDownscale);

            List<CloudResource> resourcesToDownscale = resources.stream()
                    .filter(resource -> instanceIdsToDownscale.contains(resource.getInstanceId()))
                    .collect(Collectors.toList());
            awsComputeResourceService.deleteComputeResources(auth, stack, resourcesToDownscale);

            AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView,
                    auth.getCloudContext().getLocation().getRegion().value());
            AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView,
                    auth.getCloudContext().getLocation().getRegion().value());

            Map<String, List<CloudInstance>> downscaledGroupsWithCloudInstances =
                    vmsToDownscale.stream().collect(Collectors.groupingBy(cloudInstance -> cloudInstance.getTemplate().getGroupName()));
            Long stackId = auth.getCloudContext().getId();

            List<String> terminatedInstances = terminateInstances(auth, amazonASClient, amazonEC2Client, downscaledGroupsWithCloudInstances, stackId);

            if (!terminatedInstances.isEmpty()) {
                waitForTerminateInstances(stackId, terminatedInstances, amazonEC2Client);
            }

            updateAutoscalingGroups(auth, amazonASClient, downscaledGroupsWithCloudInstances);

            List<String> targetGroupArns = getTargetGroupArns(stack.getLoadBalancers(), auth);
            loadBalancerService.removeLoadBalancerTargets(auth, targetGroupArns, resourcesToDownscale);
        }
        return awsResourceConnector.check(auth, resources);
    }

    private List<String> getTargetGroupArns(List<CloudLoadBalancer> loadBalancers, AuthenticatedContext auth) {
        List<String> targetGroupArns = new ArrayList<>();
        for (CloudLoadBalancer loadBalancer : loadBalancers) {
            for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : loadBalancer.getPortToTargetGroupMapping().entrySet()) {
                String region = auth.getCloudContext().getLocation().getRegion().value();
                AwsLoadBalancerScheme scheme = loadBalancerTypeConverter.convert(loadBalancer.getType());
                String targetGroupArn = cfStackUtil.getResourceArnByLogicalId(auth, AwsTargetGroup.getTargetGroupName(entry.getKey().getTrafficPort(), scheme),
                        region);
                targetGroupArns.add(targetGroupArn);
            }
        }
        return targetGroupArns;
    }

    private List<String> terminateInstances(AuthenticatedContext auth, AmazonAutoScalingClient amazonASClient, AmazonEc2Client amazonEC2Client, Map<String,
            List<CloudInstance>> downscaledGroupsWithCloudInstances, Long stackId) {
        List<String> terminatedInstances = new ArrayList<>();
        downscaledGroupsWithCloudInstances.forEach((groupName, cloudInstances) -> {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, groupName, auth.getCloudContext().getLocation().getRegion().value());
            LOGGER.info("Detach and terminate instances from autoscaling group: {}", asGroupName);
            List<String> instanceIdsInAutoScalingGroup = getInstanceIdsInAutoScalingGroup(asGroupName, amazonASClient);
            List<String> instanceIdsForGroupToDownscale = cloudInstances.stream()
                    .map(CloudInstance::getInstanceId).collect(Collectors.toList());
            detachInstances(asGroupName, instanceIdsInAutoScalingGroup, instanceIdsForGroupToDownscale, amazonASClient);
            terminatedInstances.addAll(terminateInstances(stackId, instanceIdsForGroupToDownscale, amazonEC2Client));
        });
        LOGGER.info("Terminated instances: {}", terminatedInstances);
        return terminatedInstances;
    }

    private void updateAutoscalingGroups(AuthenticatedContext auth, AmazonAutoScalingClient amazonASClient,
            Map<String, List<CloudInstance>> downscaledGroupsWithCloudInstances) {
        downscaledGroupsWithCloudInstances.forEach((groupName, cloudInstances) -> {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, groupName, auth.getCloudContext().getLocation().getRegion().value());
            LOGGER.info("Update autoscaling group: {}", asGroupName);
            List<String> instanceIdsInAutoScalingGroup = getInstanceIdsInAutoScalingGroup(asGroupName, amazonASClient);
            List<String> instanceIdsForGroupToDownscale = cloudInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList());
            updateAutoScalingGroupSize(asGroupName, amazonASClient, instanceIdsInAutoScalingGroup, instanceIdsForGroupToDownscale);
        });
    }

    private void updateAutoScalingGroupSize(String asGroupName, AmazonAutoScalingClient amazonASClient, List<String> instanceIdsInAutoScalingGroup,
            List<String> instanceIdsToDownscale) {
        try {
            int maxSize = calculateAutoScalingGroupSize(instanceIdsInAutoScalingGroup, instanceIdsToDownscale);
            UpdateAutoScalingGroupRequest updateDesiredAndMax = new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(asGroupName)
                    .withDesiredCapacity(maxSize)
                    .withMaxSize(maxSize);
            amazonASClient.updateAutoScalingGroup(updateDesiredAndMax);
            LOGGER.info("Update autoscaling group: {}", updateDesiredAndMax);
        } catch (AmazonServiceException e) {
            LOGGER.warn("Failed to update asGroupName: {}, error: {}", asGroupName, e.getErrorMessage(), e);
        }
    }

    private List<String> getInstanceIdsInAutoScalingGroup(String asGroupName, AmazonAutoScalingClient amazonASClient) {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(List.of(asGroupName));
        return amazonASClient.describeAutoScalingGroups(request).getAutoScalingGroups().stream()
                .map(AutoScalingGroup::getInstances)
                .flatMap(Collection::stream)
                .map(Instance::getInstanceId)
                .collect(Collectors.toList());
    }

    private void detachInstances(String asGroupName, List<String> instanceIdsForAutoScalingGroup, List<String> instanceIdsToDownscale,
            AmazonAutoScalingClient amazonASClient) {
        try {
            List<String> instanceIdsToDetach = instanceIdsForAutoScalingGroup.stream()
                    .filter(instanceIdsToDownscale::contains)
                    .collect(Collectors.toList());
            if (instanceIdsToDownscale.size() != instanceIdsToDetach.size()) {
                LOGGER.info("Some instances were already detached. Requesting to detach [{}] from original list [{}].",
                        instanceIdsToDetach, instanceIdsToDownscale);
            }

            for (int i = 0; i < instanceIdsToDetach.size(); i += MAX_DETACH_INSTANCE_SIZE) {
                List<String> idPartition = instanceIdsToDetach.subList(i, i + Math.min(instanceIdsToDetach.size() - i, MAX_DETACH_INSTANCE_SIZE));
                DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(idPartition)
                        .withShouldDecrementDesiredCapacity(true);
                LOGGER.info("Detach instances from asGroupName: {}, instanceIdsToDetach: {}, detachInstancesRequest: {}", asGroupName,
                        instanceIdsToDetach, detachInstancesRequest);
                DetachInstancesResult result = amazonASClient.detachInstances(detachInstancesRequest);
                LOGGER.info("Detach instances from asGroupName: {}, instanceIdsToDetach: {}, result: {}", asGroupName,
                        instanceIdsToDetach, result);
            }
        } catch (AmazonServiceException e) {
            LOGGER.info("Detach instances failed: {}", instanceIdsToDownscale, e);
            throw e;
        }
    }

    private int calculateAutoScalingGroupSize(List<String> instanceIdsInAutoScalingGroup, List<String> instanceIdsToDownscale) {
        return Math.toIntExact(instanceIdsInAutoScalingGroup.stream()
                .filter(instanceId -> !instanceIdsToDownscale.contains(instanceId))
                .count());
    }

    private List<String> terminateInstances(Long stackId, List<String> instanceIdsToDelete, AmazonEc2Client amazonEC2Client) {
        List<String> instanceIdsForTermination = new ArrayList<>(instanceIdsToDelete);
        while (instanceIdsForTermination.size() > 0) {
            int originalDeletableInstanceIdsSize = instanceIdsForTermination.size();
            LOGGER.debug("Terminate the following instances: [stack: {}, instances: {}]", stackId, instanceIdsForTermination);
            try {
                List<String> existingInstances = getExistingInstances(instanceIdsForTermination, amazonEC2Client);
                if (!existingInstances.isEmpty()) {
                    amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(existingInstances));
                }
                return existingInstances;
            } catch (AmazonServiceException e) {
                LOGGER.info("Termination failed, lets check if it is failed because instance was not found", e);
                if (!INSTANCE_NOT_FOUND_ERROR_CODE.equals(e.getErrorCode())) {
                    throw e;
                } else {
                    LOGGER.info("Instance was not found, lets filter out the non existing instances");
                    instanceIdsForTermination = instanceIdsForTermination.stream()
                            .filter(instanceId -> !e.getMessage().contains(instanceId))
                            .collect(Collectors.toList());
                    LOGGER.info("Collected instances from AWS for termination in next round: {}", instanceIdsForTermination);
                    if (instanceIdsForTermination.size() < originalDeletableInstanceIdsSize) {
                        LOGGER.info("We removed instances from the original set, instance list size is smaller than the original");
                    } else {
                        LOGGER.error("Element numbers are the same, it should not happen, let's cancel from the loop with exception.");
                        throw new CloudbreakServiceException("AWS instance termination failed, instance termination list is not shrinking", e);
                    }
                }
            }
        }
        return instanceIdsForTermination;
    }

    private List<String> getExistingInstances(List<String> instanceIdsToDelete, AmazonEc2Client amazonEC2Client) {
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIdsToDelete));
        List<String> existingInstances = instancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .map(com.amazonaws.services.ec2.model.Instance::getInstanceId)
                .collect(Collectors.toList());
        List<String> missingInstances = new ArrayList<>(instanceIdsToDelete);
        missingInstances.removeAll(existingInstances);
        LOGGER.debug("Known instances on AWS: {} Missing instances: {}", existingInstances, missingInstances);
        return existingInstances;
    }

    private void waitForTerminateInstances(Long stackId, List<String> instanceIds, AmazonEc2Client amazonEC2Client) {
        LOGGER.debug("Polling instance until terminated. [stack: {}, instances: {}]", stackId,
                instanceIds);
        Waiter<DescribeInstancesRequest> instanceTerminatedWaiter = amazonEC2Client.waiters().instanceTerminated();
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(stackId);
        try {
            waitTermination(instanceIds, instanceTerminatedWaiter, stackCancellationCheck);
        } catch (CloudConnectorException e) {
            LOGGER.info("Wait termination failed, lets check if it is because instance was not found", e);
            if (e.getCause() instanceof AmazonServiceException) {
                if (!INSTANCE_NOT_FOUND_ERROR_CODE.equals(((AmazonServiceException) e.getCause()).getErrorCode())) {
                    throw e;
                } else {
                    LOGGER.info("Instance was not found, lets wait for others");
                    List<String> runningInstanceIds = instanceIds.stream()
                            .filter(instanceId -> !e.getCause().getMessage().contains(instanceId))
                            .collect(Collectors.toList());
                    LOGGER.info("Running instances on AWS to check: {}", runningInstanceIds);
                    if (!runningInstanceIds.isEmpty()) {
                        waitTermination(runningInstanceIds, instanceTerminatedWaiter, stackCancellationCheck);
                    }
                }
            }
        }
    }

    private void waitTermination(List<String> instanceIds, Waiter<DescribeInstancesRequest> instanceTerminatedWaiter,
            StackCancellationCheck stackCancellationCheck) {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
        run(instanceTerminatedWaiter, describeInstancesRequest, stackCancellationCheck,
                String.format("There was an error when application are deleting instances %s", String.join(",", instanceIds)));
    }
}
