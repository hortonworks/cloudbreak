package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;

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
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.domain.CoreInstanceMetaData;

@Component
public class AwsMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataSetup.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private Reactor reactor;

    @Override
    public void setupMetadata(Stack stack) {
        Set<CoreInstanceMetaData> coreInstanceMetadata = new HashSet<>();

        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        AmazonCloudFormationClient amazonCfClient = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(awsTemplate.getRegion(), awsCredential);
        AmazonAutoScalingClient amazonAutoScalingClient = awsStackUtil.createAutoScalingClient(awsTemplate.getRegion(), awsCredential);

        DescribeStackResourceResult result = amazonCfClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(stack.getResourcesByType(ResourceType.CLOUDFORMATION_STACK).get(0).getResourceName())
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
        for (Reservation reservation : instancesResult.getReservations()) {
            for (com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                coreInstanceMetadata.add(new CoreInstanceMetaData(
                        instance.getInstanceId(),
                        instance.getPrivateIpAddress(),
                        instance.getPublicIpAddress(),
                        instance.getBlockDeviceMappings().size() - 1
                        ));
            }
        }

        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.AWS, stack.getId(), coreInstanceMetadata)));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
