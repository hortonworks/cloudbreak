package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAwsStackDescription;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstances;

import reactor.core.Reactor;

@Service
public class AwsConnector implements CloudPlatformConnector {

    public static final String INSTANCE_TAG_NAME = "Name";
    private static final String CF_SERVICE_NAME = "AmazonCloudFormation";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConnector.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private Reactor reactor;

    @Autowired
    private CloudFormationStackUtil cfStackUtil;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private PollingService<AwsInstances> awsPollingService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public StackDescription describeStackWithResources(Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        DescribeStacksResult stackResult = null;
        DescribeStackResourcesResult resourcesResult = null;
        DescribeInstancesResult instancesResult = null;
        Resource resource = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK);
        if (resource != null) {
            try {
                AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsInfra.getRegion(), awsCredential);
                DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(
                        resource.getResourceName()
                );
                stackResult = client.describeStacks(stackRequest);

                DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest().withStackName(
                        resource.getResourceName()
                );
                resourcesResult = client.describeStackResources(resourcesRequest);
            } catch (AmazonServiceException e) {
                if (CF_SERVICE_NAME.equals(e.getServiceName())
                        && e.getErrorMessage().equals(String.format("Stack:%s does not exist",
                        resource.getResourceName()))) {
                    LOGGER.error("Amazon CloudFormation stack {} doesn't exist. Returning null in describeStack.",
                            resource.getResourceName());
                    stackResult = new DescribeStacksResult();
                } else {
                    throw e;
                }
            }
            try {
                AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(awsInfra.getRegion(), awsCredential);
                DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                        .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_NAME).withValues(resource.getResourceName()));
                instancesResult = ec2Client.describeInstances(instancesRequest);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                instancesResult = new DescribeInstancesResult();
            }

        }
        return new DetailedAwsStackDescription(stackResult, resourcesResult, instancesResult);
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        LOGGER.info("Deleting stack: {}", stack.getId());
        AwsTemplate template = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        Resource resource = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK);
        if (resource != null) {
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(template.getRegion(), awsCredential);
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", stack.getId(),
                    resource.getResourceName());
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest()
                    .withStackName(resource.getResourceName());
            client.deleteStack(deleteStackRequest);
        }
    }

    @Override
    public boolean startAll(Stack stack) {
        return setStackState(stack, false);
    }

    @Override
    public boolean stopAll(Stack stack) {
        return setStackState(stack, true);
    }

    private boolean setStackState(Stack stack, boolean stopped) {
        boolean result = true;
        Regions region = ((AwsTemplate) stack.getTemplate()).getRegion();
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);
        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack);
        Set<InstanceMetaData> instanceMetaData = stack.getInstanceMetaData();
        Collection<String> instances = new ArrayList<>(instanceMetaData.size());
        for (InstanceMetaData instance : instanceMetaData) {
            instances.add(instance.getInstanceId());
        }
        try {
            if (stopped) {
                amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName));
                amazonEC2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instances));
            } else {
                amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                amazonEC2Client.startInstances(new StartInstancesRequest().withInstanceIds(instances));
                awsPollingService.pollWithTimeout(
                        new AwsInstanceStatusCheckerTask(),
                        new AwsInstances(stack.getId(), amazonEC2Client, new ArrayList(instances), "Running"),
                        AmbariClusterConnector.POLLING_INTERVAL,
                        AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
                updateInstanceMetadata(stack, amazonEC2Client, instanceMetaData, instances);
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to %s AWS instances on stack: %s", stopped ? "stop" : "start", stack.getId()));
            result = false;
        }
        return result;
    }

    private void updateInstanceMetadata(Stack stack, AmazonEC2Client amazonEC2Client, Set<InstanceMetaData> instanceMetaData, Collection<String> instances) {
        DescribeInstancesResult describeResult = amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instances));
        for (Reservation reservation : describeResult.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                for (InstanceMetaData metaData : instanceMetaData) {
                    if (metaData.getInstanceId().equals(instance.getInstanceId())) {
                        String publicDnsName = instance.getPublicDnsName();
                        if (metaData.getAmbariServer()) {
                            stack.setAmbariIp(publicDnsName);
                            Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                            stack.setCluster(cluster);
                            stackRepository.save(stack);
                        }
                        metaData.setPublicIp(publicDnsName);
                        instanceMetaDataRepository.save(metaData);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
