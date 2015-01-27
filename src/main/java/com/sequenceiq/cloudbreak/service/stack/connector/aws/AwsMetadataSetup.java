package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataUpdateComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AwsMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataSetup.class);

    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int MAX_SPOT_POLLING_ATTEMPTS = 600;
    private static final int POLLING_INTERVAL = 5000;

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private Reactor reactor;

    @Autowired
    private CloudFormationStackUtil cfStackUtil;

    @Autowired
    private PollingService<AutoScalingGroupReady> pollingService;

    @Autowired
    private ASGroupStatusCheckerTask asGroupStatusCheckerTask;


    @Override
    public void setupMetadata(Stack stack) {
        MDCBuilder.buildMdcContext(stack);

        Set<CoreInstanceMetaData> coreInstanceMetadata = new HashSet<>();

        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        AmazonCloudFormationClient amazonCFClient = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(Regions.valueOf(stack.getRegion()), awsCredential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            // wait until all instances are up
            String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup.getGroupName());
            AutoScalingGroupReady asGroupReady = new AutoScalingGroupReady(stack, amazonEC2Client, amazonASClient, asGroupName, instanceGroup.getNodeCount());
            LOGGER.info("Polling autoscaling group until new instances are ready. [stack: {}, asGroup: {}]", stack.getId(), asGroupName);
            if (((AwsTemplate) instanceGroup.getTemplate()).getSpotPrice() == null) {
                pollingService.pollWithTimeout(asGroupStatusCheckerTask, asGroupReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            } else {
                pollingService.pollWithTimeout(asGroupStatusCheckerTask, asGroupReady, POLLING_INTERVAL, MAX_SPOT_POLLING_ATTEMPTS);
            }
        }

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            List<String> instanceIds = new ArrayList<>();
            instanceIds.addAll(cfStackUtil.getInstanceIds(stack, amazonASClient, amazonCFClient, instanceGroup.getGroupName()));
            DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
            DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);
            for (Reservation reservation : instancesResult.getReservations()) {
                LOGGER.info("Number of instances found in reservation: {}", reservation.getInstances().size());
                for (com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                    coreInstanceMetadata.add(new CoreInstanceMetaData(
                            instance.getInstanceId(),
                            instance.getPrivateIpAddress(),
                            instance.getPublicDnsName(),
                            instance.getBlockDeviceMappings().size() - 1,
                            instance.getPrivateDnsName(),
                            instanceGroup
                            ));
                }
            }
        }

        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.AWS, stack.getId(), coreInstanceMetadata)));
    }

    @Override
    public void addNewNodesToMetadata(Stack stack, Set<Resource> resourceList, String hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        Set<CoreInstanceMetaData> coreInstanceMetadata = new HashSet<>();
        LOGGER.info("Adding new instances to metadata: [stack: '{}']", stack.getId());
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            List<String> instanceIds = new ArrayList<>();
            instanceIds.addAll(cfStackUtil.getInstanceIds(stack, instanceGroup.getGroupName()));

            DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
            DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);
            for (Reservation reservation : instancesResult.getReservations()) {
                for (final com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                    boolean metadataExists = FluentIterable.from(instanceGroup.getInstanceMetaData()).anyMatch(new Predicate<InstanceMetaData>() {
                        @Override
                        public boolean apply(InstanceMetaData input) {
                            return input.getInstanceId().equals(instance.getInstanceId());
                        }
                    });
                    if (!metadataExists) {
                        coreInstanceMetadata.add(new CoreInstanceMetaData(
                                instance.getInstanceId(),
                                instance.getPrivateIpAddress(),
                                instance.getPublicDnsName(),
                                instance.getBlockDeviceMappings().size() - 1,
                                instance.getPrivateDnsName(),
                                instanceGroup
                        ));
                        LOGGER.info("New instance added to metadata: [stack: '{}', instanceId: '{}']", stack.getId(), instance.getInstanceId());
                    }
                }
            }
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT,
                Event.wrap(new MetadataUpdateComplete(CloudPlatform.AWS, stack.getId(), coreInstanceMetadata, hostGroup)));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
