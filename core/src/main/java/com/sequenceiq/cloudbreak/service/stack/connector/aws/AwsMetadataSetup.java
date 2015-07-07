package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.DELETED;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.RUNNING;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.STOPPED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

@Component
public class AwsMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataSetup.class);

    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int MAX_SPOT_POLLING_ATTEMPTS = 600;
    private static final int POLLING_INTERVAL = 5000;

    @Inject
    private AwsStackUtil awsStackUtil;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private PollingService<AutoScalingGroupReadyContext> pollingService;

    @Inject
    private ASGroupStatusCheckerTask asGroupStatusCheckerTask;

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        Set<CoreInstanceMetaData> coreInstanceMetadata = new HashSet<>();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        AmazonCloudFormationClient amazonCFClient = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(Regions.valueOf(stack.getRegion()), awsCredential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            // wait until all instances are up
            String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup.getGroupName());
            AutoScalingGroupReadyContext asGroupReady = new AutoScalingGroupReadyContext(stack, asGroupName, instanceGroup.getNodeCount());
            LOGGER.info("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", stack.getId(), asGroupName);
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
                            instance.getPublicIpAddress(),
                            instance.getBlockDeviceMappings().size() - 1,
                            instanceGroup
                    ));
                }
            }
        }
        return coreInstanceMetadata;
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroupName) {
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
                                instance.getPublicIpAddress(),
                                instance.getBlockDeviceMappings().size() - 1,
                                instanceGroup
                        ));
                        LOGGER.info("New instance added to metadata: [stack: '{}', instanceId: '{}']", stack.getId(), instance.getInstanceId());
                    }
                }
            }
        }
        return coreInstanceMetadata;
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        InstanceSyncState instanceSyncState = IN_PROGRESS;
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);
        try {
            DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
            if (describeInstancesResult.getReservations().size() > 0) {
                Instance instance = describeInstancesResult.getReservations().iterator().next().getInstances().iterator().next();
                String instanceState = instance.getState().getName().toLowerCase();
                if ("stopped".equals(instanceState)) {
                    instanceSyncState = STOPPED;
                } else if ("running".equals(instanceState)) {
                    instanceSyncState = RUNNING;
                } else if ("terminated".equals(instanceState)) {
                    instanceSyncState = DELETED;
                    detachInstance(stack, instanceGroup, instanceId);
                }
            } else {
                instanceSyncState = DELETED;
                detachInstance(stack, instanceGroup, instanceId);
            }
        } catch (AmazonServiceException e) {
            if ("InvalidInstanceID.NotFound".equals(e.getErrorCode())) {
                instanceSyncState = DELETED;
                detachInstance(stack, instanceGroup, instanceId);
            } else {
                throw new AwsResourceException("Failed to retrieve state of instance " + instanceId, e);
            }
        } catch (Exception e) {
            throw new AwsResourceException("Failed to retrieve state of instance " + instanceId, e);
        }
        return instanceSyncState;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return null;
    }

    private void detachInstance(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(stack);
        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup.getGroupName());
        DescribeAutoScalingInstancesResult result = amazonASClient.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest()
                .withInstanceIds(instanceId));
        if (!result.getAutoScalingInstances().isEmpty()) {
            amazonASClient.detachInstances(new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceId)
                    .withShouldDecrementDesiredCapacity(true));
        }
    }


}
