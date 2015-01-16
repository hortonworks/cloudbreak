package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;
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

    @Value("${cb.aws.ami.map}")
    private String amis;

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

    /**
     * If the AutoScaling group has some suspended scaling policies it causes that the CloudFormation stack delete won't be able to remove the ASG.
     * In this case the ASG size is reduced to zero and the processes are resumed first.
     */
    @Override
    public void deleteStack(Stack stack, Credential credential) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Deleting stack: {}", stack.getId());
        AwsCredential awsCredential = (AwsCredential) credential;
        Resource resource = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK);
        if (resource != null) {
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", stack.getId(), resource.getResourceName());
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(resource.getResourceName());
            try {
                client.describeStacks(describeStacksRequest);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().equals("Stack:" + resource.getResourceName() + " does not exist")) {
                    LOGGER.info("AWS CloudFormation stack not found, publishing {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
                    reactor.notify(ReactorConfig.DELETE_COMPLETE_EVENT, Event.wrap(new StackDeleteComplete(stack.getId())));
                    return;
                } else {
                    throw e;
                }
            }
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup.getGroupName());
                AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(Regions.valueOf(stack.getRegion()), awsCredential);
                List<AutoScalingGroup> asGroups = amazonASClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(asGroupName)).getAutoScalingGroups();
                if (!asGroups.isEmpty()) {
                    if (!asGroups.get(0).getSuspendedProcesses().isEmpty()) {
                        amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                                .withAutoScalingGroupName(asGroupName)
                                .withMinSize(0)
                                .withDesiredCapacity(0));
                        amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                    }
                }
            }
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(resource.getResourceName());
            client.deleteStack(deleteStackRequest);
        } else {
            LOGGER.info("No resource saved for stack, publishing {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
            reactor.notify(ReactorConfig.DELETE_COMPLETE_EVENT, Event.wrap(new StackDeleteComplete(stack.getId())));
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        return;
    }

    @Override
    public void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        MDCBuilder.buildMdcContext(stack);
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("SSHLocation").withParameterValue("0.0.0.0/0"),
                new Parameter().withParameterKey("CBUserData").withParameterValue(userData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(awsCredential.getRoleArn()),
                new Parameter().withParameterKey("KeyName").withParameterValue(awsCredential.getKeyPairName()),
                new Parameter().withParameterKey("AMI").withParameterValue(prepareAmis().get(Regions.valueOf(stack.getRegion()).getName()))
        ));
        CreateStackRequest createStackRequest = createStackRequest()
                .withStackName(stackName)
                .withTemplateBody(cfTemplateBuilder.build("templates/aws-cf-stack.ftl",
                        spotPriceNeeded(stack.getInstanceGroups()), stack.getInstanceGroupsAsList()))
                .withNotificationARNs((String) setupProperties.get(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY))
                .withParameters(parameters);
        client.createStack(createStackRequest);


        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, stackName, stack, Lists.newArrayList(stack.getInstanceGroups()).get(0).getGroupName()));
        Stack updatedStack = stackUpdater.updateStackResources(stack.getId(), resources);
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", stackName, updatedStack.getId());
    }


    private boolean spotPriceNeeded(Set<InstanceGroup> instanceGroups) {
        boolean spotPrice = true;
        for (InstanceGroup instanceGroup : instanceGroups) {
            AwsTemplate awsTemplate = (AwsTemplate) instanceGroup.getTemplate();
            if (awsTemplate.getSpotPrice() == null) {
                spotPrice = false;
            }
        }
        return spotPrice;
    }

    private Map<String, String> prepareAmis() {
        Map<String, String> amisMap = new HashMap<>();
        for (String s : amis.split(",")) {
            amisMap.put(s.split(":")[0], s.split(":")[1]);
        }
        return amisMap;
    }

    @Override
    public boolean addInstances(Stack stack, String userData, Integer instanceCount, String hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        Integer requiredInstances = stack.getTemplateAsGroup(hostGroup).getNodeCount() + instanceCount;
        Regions region = Regions.valueOf(stack.getRegion());
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);
        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, hostGroup);
        amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(asGroupName)
                .withMaxSize(requiredInstances)
                .withDesiredCapacity(requiredInstances));
        LOGGER.info("Updated AutoScaling group's desiredCapacity: [stack: '{}', from: '{}', to: '{}']", stack.getId(), stack.getFullNodeCount(),
                stack.getFullNodeCount() + instanceCount);
        AutoScalingGroupReady asGroupReady = new AutoScalingGroupReady(stack, amazonEC2Client, amazonASClient, asGroupName, requiredInstances);
        LOGGER.info("Polling autoscaling group until new instances are ready. [stack: {}, asGroup: {}]", stack.getId(), asGroupName);
        pollingService.pollWithTimeout(asGroupStatusCheckerTask, asGroupReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, Event.wrap(new AddInstancesComplete(CloudPlatform.AWS, stack.getId(), null, hostGroup)));
        return true;
    }

    @Override
    public boolean removeInstances(Stack stack, Set<String> instanceIds, String hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        Regions region = Regions.valueOf(stack.getRegion());
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);

        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, hostGroup);
        DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds)
                .withShouldDecrementDesiredCapacity(true);
        amazonASClient.detachInstances(detachInstancesRequest);
        amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        LOGGER.info("Terminated instances in stack '{}': '{}'", stack.getId(), instanceIds);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, stack.getId());
        reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true, instanceIds, hostGroup)));
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
        MDCBuilder.buildMdcContext(stack);
        boolean result = true;
        Regions region = Regions.valueOf(stack.getRegion());
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup.getGroupName());
            Set<InstanceMetaData> instanceMetaData = stack.getInstanceMetaData();
            Collection<String> instances = new ArrayList<>(instanceMetaData.size());
            for (InstanceMetaData instance : instanceMetaData) {
                if (instance.getInstanceGroup().equals(instanceGroup.getGroupName())) {
                    instances.add(instance.getInstanceId());
                }
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
        }
        return result;
    }

    private void updateInstanceMetadata(Stack stack, AmazonEC2Client amazonEC2Client, Set<InstanceMetaData> instanceMetaData, Collection<String> instances) {
        MDCBuilder.buildMdcContext(stack);
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
