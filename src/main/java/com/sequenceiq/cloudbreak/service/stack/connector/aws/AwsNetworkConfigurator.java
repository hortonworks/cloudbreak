package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class AwsNetworkConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkConfigurator.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    public void disableSourceDestCheck(Stack stack) {

        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        AmazonCloudFormationClient amazonCfClient = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(awsTemplate.getRegion(), awsCredential);
        AmazonAutoScalingClient amazonAutoScalingClient = awsStackUtil.createAutoScalingClient(awsTemplate.getRegion(), awsCredential);

        DescribeStackResourceResult result = amazonCfClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(stack.getResourcesbyType(ResourceType.CLOUDFORMATION_STACK).get(0).getResourceName())
                .withLogicalResourceId("AmbariNodes"));

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonAutoScalingClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(result.getStackResourceDetail()
                        .getPhysicalResourceId()));
        describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances();
        List<String> instanceIds = new ArrayList<>();
        for (Instance instance : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }

        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);
        List<String> enis = new ArrayList<>();
        for (Reservation reservation : instancesResult.getReservations()) {
            for (com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                for (InstanceNetworkInterface instanceNetworkInterface : instance.getNetworkInterfaces()) {
                    enis.add(instanceNetworkInterface.getNetworkInterfaceId());
                }
            }
        }
        for (String eni : enis) {
            ModifyNetworkInterfaceAttributeRequest modifyNetworkInterfaceAttributeRequest = new ModifyNetworkInterfaceAttributeRequest()
                    .withNetworkInterfaceId(eni)
                    .withSourceDestCheck(false);
            amazonEC2Client.modifyNetworkInterfaceAttribute(modifyNetworkInterfaceAttributeRequest);
        }

        LOGGER.info("Disabled sourceDestCheck. (stack: '{}', instances: '{}', network interfaces: '{}')", stack.getId(), instanceIds, enis);

    }
}
