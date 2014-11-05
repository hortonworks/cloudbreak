package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
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
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstances;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class AwsConnector implements CloudPlatformConnector {

    public static final String INSTANCE_TAG_NAME = "Name";
    private static final String CF_SERVICE_NAME = "AmazonCloudFormation";
    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int POLLING_INTERVAL = 5000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConnector.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private Reactor reactor;

    @Autowired
    private ASGroupStatusCheckerTask asGroupStatusCheckerTask;

    @Autowired
    private CloudFormationTemplateBuilder cfTemplateBuilder;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private CloudFormationStackUtil cfStackUtil;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private PollingService<AwsInstances> awsPollingService;

    @Autowired
    private PollingService<AutoScalingGroupReady> pollingService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public StackDescription describeStackWithResources(Stack stack, Credential credential) {
        CbLoggerFactory.buildMdcContext(stack);
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
        CbLoggerFactory.buildMdcContext(stack);
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
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        return;
    }

    @Override
    public void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        CbLoggerFactory.buildMdcContext(stack);
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        boolean spotPriced = awsTemplate.getSpotPrice() == null ? false : true;
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()),
                new Parameter().withParameterKey("CBUserData").withParameterValue(userData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(awsCredential.getRoleArn()),
                new Parameter().withParameterKey("InstanceCount").withParameterValue(stack.getNodeCount().toString()),
                new Parameter().withParameterKey("InstanceType").withParameterValue(awsTemplate.getInstanceType().toString()),
                new Parameter().withParameterKey("KeyName").withParameterValue(awsCredential.getKeyPairName()),
                new Parameter().withParameterKey("AMI").withParameterValue(awsTemplate.getAmiId()),
                new Parameter().withParameterKey("VolumeSize").withParameterValue(awsTemplate.getVolumeSize().toString()),
                new Parameter().withParameterKey("VolumeType").withParameterValue(awsTemplate.getVolumeType().toString())));
        if (spotPriced) {
            parameters.add(new Parameter().withParameterKey("SpotPrice").withParameterValue(awsTemplate.getSpotPrice().toString()));
        }
        CreateStackRequest createStackRequest = createStackRequest()
                .withStackName(stackName)
                .withTemplateBody(cfTemplateBuilder.build("templates/aws-cf-stack.ftl", awsTemplate.getVolumeCount(), spotPriced))
                .withNotificationARNs((String) setupProperties.get(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY))
                .withParameters(parameters);
        client.createStack(createStackRequest);
        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, stackName, stack));
        Stack updatedStack = stackUpdater.updateStackResources(stack.getId(), resources);
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", stackName, updatedStack.getId());
    }

    @Override
    public boolean addInstances(Stack stack, String userData, Integer instanceCount) {
        CbLoggerFactory.buildMdcContext(stack);
        Integer requiredInstances = stack.getNodeCount() + instanceCount;
        Regions region = ((AwsTemplate) stack.getTemplate()).getRegion();
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);
        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack);
        amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(asGroupName)
                .withMaxSize(requiredInstances)
                .withDesiredCapacity(requiredInstances));
        LOGGER.info("Updated AutoScaling group's desiredCapacity: [stack: '{}', from: '{}', to: '{}']", stack.getId(), stack.getNodeCount(),
                stack.getNodeCount() + instanceCount);
        AutoScalingGroupReady asGroupReady = new AutoScalingGroupReady(stack, amazonEC2Client, amazonASClient, asGroupName, requiredInstances);
        LOGGER.info("Polling autoscaling group until new instances are ready. [stack: {}, asGroup: {}]", stack.getId(), asGroupName);
        pollingService.pollWithTimeout(asGroupStatusCheckerTask, asGroupReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, Event.wrap(new AddInstancesComplete(CloudPlatform.AWS, stack.getId(), null)));
        return true;
    }

    @Override
    public boolean removeInstances(Stack stack, Set<String> instanceIds) {
        CbLoggerFactory.buildMdcContext(stack);
        Regions region = ((AwsTemplate) stack.getTemplate()).getRegion();
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);

        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack);
        DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds)
                .withShouldDecrementDesiredCapacity(true);
        amazonASClient.detachInstances(detachInstancesRequest);
        amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        LOGGER.info("Terminated instances in stack '{}': '{}'", stack.getId(), instanceIds);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, stack.getId());
        reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true, instanceIds)));
        return true;
    }

    protected CreateStackRequest createStackRequest() {
        return new CreateStackRequest();
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
        CbLoggerFactory.buildMdcContext(stack);
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
                        new AwsInstances(stack, amazonEC2Client, new ArrayList(instances), "Running"),
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
        CbLoggerFactory.buildMdcContext(stack);
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
