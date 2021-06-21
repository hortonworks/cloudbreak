package com.sequenceiq.cloudbreak.cloud.aws;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSetup;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Component
public class AwsCloudFormationSetup extends AwsSetup {

    private static final Logger LOGGER = getLogger(AwsCloudFormationSetup.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsInstanceConnector instanceConnector;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Override
    public void scalingPrerequisites(AuthenticatedContext ac, CloudStack stack, boolean upscale) {
        if (!upscale) {
            return;
        }
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(awsCredential, regionName);
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(awsCredential, regionName);
        List<Group> groups = stack.getGroups().stream().filter(g -> g.getInstances().stream().anyMatch(
                inst -> InstanceStatus.CREATE_REQUESTED == inst.getTemplate().getStatus())).collect(Collectors.toList());
        Map<String, Group> groupMap = groups.stream().collect(
                Collectors.toMap(g -> cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, g.getName()), g -> g));
        DescribeAutoScalingGroupsResult result = amazonASClient.describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(groupMap.keySet()));
        for (AutoScalingGroup asg : result.getAutoScalingGroups()) {
            Group group = groupMap.get(asg.getAutoScalingGroupName());
            List<CloudInstance> groupInstances = group.getInstances().stream().filter(
                    inst -> InstanceStatus.CREATED.equals(inst.getTemplate().getStatus())).collect(Collectors.toList());
            List<CloudVmInstanceStatus> instanceStatuses = instanceConnector.check(ac, groupInstances);
            if (!instanceStatuses.stream().allMatch(inst -> inst.getStatus().equals(InstanceStatus.STARTED))) {
                String errorMessage = "Not all the existing instances are in [Started] state, upscale is not possible!";
                LOGGER.info(errorMessage);
                throw new CloudConnectorException(errorMessage);
            }
            List<Instance> asgOnlyInstances = asg.getInstances().stream()
                    .filter(inst -> groupInstances.stream().noneMatch(gi -> gi.getInstanceId().equals(inst.getInstanceId()))).collect(Collectors.toList());
            List<CloudInstance> cbOnlyInstances = groupInstances.stream()
                    .filter(gi -> asg.getInstances().stream().noneMatch(inst -> inst.getInstanceId().equals(gi.getInstanceId()))).collect(Collectors.toList());
            if (!asgOnlyInstances.isEmpty() || !cbOnlyInstances.isEmpty()) {
                String errorMessage = "The instances in the autoscaling group are not in sync with the instances in cloudbreak! Cloudbreak only instances: ["
                        + cbOnlyInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.joining(",")) + "], AWS only instances: ["
                        + asgOnlyInstances.stream().map(Instance::getInstanceId).collect(Collectors.joining(",")) + "]. Upscale is not possible!";
                LOGGER.info(errorMessage);
                throw new CloudConnectorException(errorMessage);
            }
            if (groupInstances.size() != asg.getDesiredCapacity()) {
                String errorMessage = String.format("The autoscale group's desired instance count is not match with the instance count in the cloudbreak."
                        + " Desired count: %d <> cb instance count: %d. Upscale is not possible!", asg.getDesiredCapacity(), groupInstances.size());
                LOGGER.info(errorMessage);
                throw new CloudConnectorException(errorMessage);
            }
        }
    }
}
