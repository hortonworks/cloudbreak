package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
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
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Object resourcesToRemove) {
        if (!vms.isEmpty()) {
            List<String> instanceIds = new ArrayList<>();
            for (CloudInstance vm : vms) {
                instanceIds.add(vm.getInstanceId());
            }

            AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(auth);
            String regionName = authenticatedContextView.getRegion();
            awsCloudWatchService.deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView, instanceIds);

            List<CloudResource> resourcesToDownscale = resources.stream()
                    .filter(resource -> instanceIds.contains(resource.getInstanceId()))
                    .collect(Collectors.toList());
            awsComputeResourceService.deleteComputeResources(auth, stack, resourcesToDownscale);

            String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(),
                    auth.getCloudContext().getLocation().getRegion().value());
            AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(credentialView,
                    auth.getCloudContext().getLocation().getRegion().value());
            detachInstances(asGroupName, instanceIds, amazonASClient, true);
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView,
                    auth.getCloudContext().getLocation().getRegion().value());

            Long stackId = auth.getCloudContext().getId();
            terminateInstances(stackId, instanceIds, amazonEC2Client);
            waitForTerminateInstances(stackId, instanceIds, amazonEC2Client);

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

        }
        return awsResourceConnector.check(auth, resources);
    }

    private void detachInstances(String asGroupName, List<String> instanceIds, AmazonAutoScalingRetryClient amazonASClient,
            boolean retryIfUnattachedInstancesPresent) {
        try {
            for (int i = 0; i < instanceIds.size(); i += MAX_DETACH_INSTANCE_SIZE) {
                List<String> idPartition = instanceIds.subList(i, i + Math.min(instanceIds.size() - i, MAX_DETACH_INSTANCE_SIZE));
                DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(idPartition)
                        .withShouldDecrementDesiredCapacity(true);
                LOGGER.info("Detach instances from asGroupName: {}, instanceIds: {}, detachInstancesRequest: {}", asGroupName,
                        instanceIds, detachInstancesRequest);
                DetachInstancesResult result = amazonASClient.detachInstances(detachInstancesRequest);
                LOGGER.info("Detach instances from asGroupName: {}, instanceIds: {}, result: {}", asGroupName,
                        instanceIds, result);
            }
        } catch (AmazonServiceException e) {
            LOGGER.info("Detach instances failed: {}", instanceIds, e);
            // it is good enough to check with string contains, because AWS instance ids are unique and the lengths are fix
            if (retryIfUnattachedInstancesPresent &&
                    ("ValidationError".equals(e.getErrorCode()) && e.getErrorMessage().contains("not part of Auto Scaling"))) {
                List<String> attachedInstances = instanceIds.stream().filter(id -> !e.getErrorMessage().contains(id)).collect(Collectors.toList());
                if (!attachedInstances.isEmpty()) {
                    LOGGER.info("Attached instances from AWS error response: {}", attachedInstances);
                    detachInstances(asGroupName, attachedInstances, amazonASClient, false);
                    return;
                }
            }
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

    private void terminateInstances(Long stackId, List<String> instanceIds, AmazonEC2Client amazonEC2Client) {
        LOGGER.debug("Terminated instances. [stack: {}, instances: {}]", stackId, instanceIds);
        try {
            amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        } catch (AmazonServiceException e) {
            if (!INSTANCE_NOT_FOUND_ERROR_CODE.equals(e.getErrorCode())) {
                throw e;
            } else {
                List<String> runningInstances = instanceIds.stream()
                        .filter(instanceId -> !e.getMessage().contains(instanceId))
                        .collect(Collectors.toList());
                if (!runningInstances.isEmpty()) {
                    amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(runningInstances));
                }
            }
            LOGGER.debug(e.getErrorMessage());
        }
    }

    private void waitForTerminateInstances(Long stackId, List<String> instanceIds, AmazonEC2Client amazonEC2Client) {
        LOGGER.debug("Polling instance until terminated. [stack: {}, instances: {}]", stackId,
                instanceIds);
        Waiter<DescribeInstancesRequest> instanceTerminatedWaiter = amazonEC2Client.waiters().instanceTerminated();
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(stackId);
        run(instanceTerminatedWaiter, describeInstancesRequest, stackCancellationCheck);
    }
}
