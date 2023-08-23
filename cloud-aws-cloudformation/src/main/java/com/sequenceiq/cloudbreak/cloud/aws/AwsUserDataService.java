package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;

@Service
public class AwsUserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsUserDataService.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Inject
    private AwsCloudFormationClient awsCloudFormationClient;

    @Inject
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    /**
     * Obtains user data associated with instance groups
     *
     * @param ac         Authenticated context
     * @param cloudStack Cloud stack
     * @return Map of group name (of cloud stack) -> user data (raw string from EC2 Launch Template Version)
     */
    public Map<String, String> getUserData(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonCloudFormationClient cloudFormationRetryClient = getAmazonCloudFormationClient(ac);
        AmazonAutoScalingClient autoScalingClient = getAmazonAutoScalingClient(ac);

        LOGGER.debug("Getting autoscaling groups for stack '{}'", ac.getCloudContext().getName());
        Map<String, AutoScalingGroup> autoScalingGroups = autoScalingGroupHandler
                .autoScalingGroupByName(cloudFormationRetryClient, autoScalingClient, cfStackUtil.getCfStackName(ac));
        LOGGER.debug("Mapping instance group name to autoscaling group name for stack '{}'", ac.getCloudContext().getName());
        Map<String, String> groupNameToAsgName = cfStackUtil.getGroupNameToAutoscalingGroupName(ac, cloudFormationRetryClient, cloudStack.getGroups());

        Map<String, String> groupNameToUserData = groupNameToAsgName.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    AutoScalingGroup asg = autoScalingGroups.get(e.getValue());
                    LOGGER.debug("Fetching user data for ASG '{}'", asg.autoScalingGroupName());
                    return awsLaunchTemplateUpdateService.getUserDataFromAutoScalingGroup(ac, asg);
                }));
        return groupNameToUserData;
    }

    private AmazonAutoScalingClient getAmazonAutoScalingClient(AuthenticatedContext ac) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        return awsCloudFormationClient.createAutoScalingClient(credentialView, regionName);
    }

    private AmazonCloudFormationClient getAmazonCloudFormationClient(AuthenticatedContext ac) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        return awsCloudFormationClient.createCloudFormationClient(credentialView, regionName);
    }

}
