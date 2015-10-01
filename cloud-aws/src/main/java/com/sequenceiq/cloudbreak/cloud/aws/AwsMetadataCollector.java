package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@Service
public class AwsMetadataCollector implements MetadataCollector {

    @Inject
    private AwsClient awsClient;

    @Override
    public List<CloudVmInstanceStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        /*List<CloudVmInstanceStatus> results = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();

        try {
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
                                instanceGroup.getGroupName()
                        ));
                    }
                }
            }
            return coreInstanceMetadata;
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return results;*/
        return null;
    }

}
