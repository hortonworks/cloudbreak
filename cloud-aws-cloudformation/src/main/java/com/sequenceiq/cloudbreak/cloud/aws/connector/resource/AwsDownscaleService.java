package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
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
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class AwsDownscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsDownscaleService.class);

    private static final int MAX_DETACH_INSTANCE_SIZE = 20;

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    @Inject
    private LegacyAwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        if (!vms.isEmpty()) {
            List<String> instanceIds = new ArrayList<>();
            for (CloudInstance vm : vms) {
                instanceIds.add(vm.getInstanceId());
            }

            AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(auth);
            String regionName = authenticatedContextView.getRegion();
            LOGGER.debug("Calling deleteCloudWatchAlarmsForSystemFailures from AwsDownscaleService");
            awsCloudWatchService.deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIds);

            List<CloudResource> resourcesToDownscale = resources.stream()
                    .filter(resource -> instanceIds.contains(resource.getInstanceId()))
                    .collect(Collectors.toList());
            awsComputeResourceService.deleteComputeResources(auth, stack, resourcesToDownscale);

            String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(),
                    auth.getCloudContext().getLocation().getRegion().value());
            AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, auth.getCloudContext().getLocation().getRegion().value());
            detachInstances(asGroupName, instanceIds, amazonASClient);
            AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView,
                    auth.getCloudContext().getLocation().getRegion().value());

            Long stackId = auth.getCloudContext().getId();
            List<String> terminatedInstances = terminateInstances(stackId, instanceIds, amazonEC2Client);
            waitForTerminateInstances(stackId, terminatedInstances, amazonEC2Client);

            try {
                int maxSize = getInstanceCount(stack, vms.get(0).getTemplate().getGroupName());
                UpdateAutoScalingGroupRequest updateDesiredAndMax = new UpdateAutoScalingGroupRequest()
                        .withAutoScalingGroupName(asGroupName)
                        .withDesiredCapacity(maxSize)
                        .withMaxSize(maxSize);
                amazonASClient.updateAutoScalingGroup(updateDesiredAndMax);
            } catch (AmazonServiceException e) {
                LOGGER.warn("Failed to update asGroupName: {}, error: {}", asGroupName, e.getErrorMessage(), e);
            }

            for (CloudLoadBalancer loadBalancer : stack.getLoadBalancers()) {
                cfStackUtil.removeLoadBalancerTargets(auth, loadBalancer, resourcesToDownscale);
            }

        }
        return awsResourceConnector.check(auth, resources);
    }

    private void detachInstances(String asGroupName, List<String> instanceIds, AmazonAutoScalingClient amazonASClient) {
        try {
            DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
            request.setAutoScalingGroupNames(List.of(asGroupName));
            List<String> instanceIdsToDetach = amazonASClient.describeAutoScalingGroups(request).getAutoScalingGroups().stream()
                    .map(AutoScalingGroup::getInstances)
                    .flatMap(Collection::stream)
                    .map(Instance::getInstanceId)
                    .filter(instanceIds::contains)
                    .collect(Collectors.toList());
            if (instanceIds.size() != instanceIdsToDetach.size()) {
                LOGGER.info("Some instances were already detached. Requesting to detach [{}] from original list [{}].", instanceIdsToDetach, instanceIds);
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
            LOGGER.info("Detach instances failed: {}", instanceIds, e);
            throw e;
        }
    }

    private int getInstanceCount(CloudStack stack, String groupName) {
        int result = -1;
        Optional<Group> group = stack.getGroups().stream().filter(g -> g.getName().equals(groupName)).findFirst();
        if (group.isPresent()) {
            result = (int) group.get().getInstances().stream().filter(inst -> !inst.getTemplate().getStatus().equals(InstanceStatus.DELETE_REQUESTED)).count();
        }
        return result;
    }

    private List<String> terminateInstances(Long stackId, List<String> instanceIds, AmazonEc2Client amazonEC2Client) {
        LOGGER.debug("Terminated instances. [stack: {}, instances: {}]", stackId, instanceIds);
        try {
            amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
            return instanceIds;
        } catch (AmazonServiceException e) {
            LOGGER.info("Termination failed, lets check if it is because instance was not found", e);
            if (!INSTANCE_NOT_FOUND_ERROR_CODE.equals(e.getErrorCode())) {
                throw e;
            } else {
                LOGGER.info("Instance was not found, lets terminate others");
                List<String> runningInstances = instanceIds.stream()
                        .filter(instanceId -> !e.getMessage().contains(instanceId))
                        .collect(Collectors.toList());
                LOGGER.info("Running instances on AWS to terminate: {}", runningInstances);
                if (!runningInstances.isEmpty()) {
                    amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(runningInstances));
                }
                return runningInstances;
            }
        }
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
