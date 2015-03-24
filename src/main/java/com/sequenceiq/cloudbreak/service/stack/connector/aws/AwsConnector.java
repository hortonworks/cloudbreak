package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UpdateFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstances;

import reactor.core.Reactor;

@Service
public class AwsConnector implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConnector.class);
    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int POLLING_INTERVAL = 5000;
    private static final int INFINITE_ATTEMPTS = -1;
    private static final String CLOUDBREAK_EBS_SNAPSHOT = "cloudbreak-ebs-snapshot";
    private static final int SNAPSHOT_VOLUME_SIZE = 10;

    @Value("${cb.aws.cf.template.path:templates/aws-cf-stack.ftl}")
    private String awsCloudformationTemplatePath;

    @Autowired private AwsStackUtil awsStackUtil;
    @Autowired private Reactor reactor;
    @Autowired private ASGroupStatusCheckerTask asGroupStatusCheckerTask;
    @Autowired private CloudFormationTemplateBuilder cfTemplateBuilder;
    @Autowired private RetryingStackUpdater stackUpdater;
    @Autowired private CloudFormationStackUtil cfStackUtil;
    @Autowired private InstanceMetaDataRepository instanceMetaDataRepository;
    @Autowired private PollingService<AwsInstances> awsPollingService;
    @Autowired private PollingService<AutoScalingGroupReady> autoScalingGroupReadyPollingService;
    @Autowired private PollingService<CloudFormationStackPollerObject> cloudFormationPollingService;
    @Autowired private PollingService<EbsVolumeStatePollerObject> ebsVolumeStatePollingService;
    @Autowired private PollingService<SnapshotReadyPollerObject> snapshotReadyPollingService;
    @Autowired private ClusterRepository clusterRepository;
    @Autowired private UserDataBuilder userDataBuilder;
    @Autowired private StackRepository stackRepository;
    @Autowired private AwsInstanceStatusCheckerTask awsInstanceStatusCheckerTask;
    @Autowired private CloudFormationStackStatusChecker cloudFormationStackStatusChecker;
    @Autowired private EbsVolumeStateCheckerTask ebsVolumeStateCheckerTask;
    @Autowired private SnapshotReadyCheckerTask snapshotReadyCheckerTask;

    @Override
    public Set<Resource> buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        MDCBuilder.buildMdcContext(stack);
        Long stackId = stack.getId();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        String cFStackName = cfStackUtil.getCfStackName(stack);
        String snapshotId = getEbsSnapshotIdIfNeeded(stack);
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.valueOf(stack.getOnFailureActionAction().name()))
                .withTemplateBody(cfTemplateBuilder.build(stack, snapshotId, stack.isExistingVPC(), awsCloudformationTemplatePath))
                .withParameters(getStackParameters(stack, userData, awsCredential, cFStackName, stack.isExistingVPC()));
        client.createStack(createStackRequest);
        Resource resource = new Resource(ResourceType.CLOUDFORMATION_STACK, cFStackName, stack, null);
        Set<Resource> resources = Sets.newHashSet(resource);
        stack = stackUpdater.updateStackResources(stackId, resources);
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, stackId);
        List<StackStatus> errorStatuses = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);
        CloudFormationStackPollerObject stackPollerContext = new CloudFormationStackPollerObject(client, CREATE_COMPLETE, CREATE_FAILED, errorStatuses, stack);
        try {
            PollingResult pollingResult = cloudFormationPollingService
                    .pollWithTimeout(cloudFormationStackStatusChecker, stackPollerContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
            if (isSuccess(pollingResult)) {
                LOGGER.info("CloudFormation stack({}) creation completed.", cFStackName);
                LOGGER.info("Publishing {} event.", ReactorConfig.PROVISION_COMPLETE_EVENT);
            }
        } catch (CloudFormationStackException e) {
            LOGGER.error(String.format("Failed to create CloudFormation stack: %s", stackId), e);
            stackUpdater.updateStackStatus(stackId, Status.CREATE_FAILED, "Creation of cluster infrastructure failed: " + e.getMessage());
            throw new BuildStackFailureException(e);
        }
        return resources;
    }

    private String getEbsSnapshotIdIfNeeded(Stack stack) {
        if (isEncryptedVolumeRequested(stack)) {
            Optional<String> snapshot = createSnapshotIfNeeded(stack);
            if (snapshot.isPresent()) {
                return snapshot.orNull();
            } else {
                throw new CloudFormationStackException(String.format("Failed to create Ebs encrypted volume on stack: %s", stack.getId()));
            }
        } else {
            return null;
        }
    }

    private Optional<String> createSnapshotIfNeeded(Stack stack) {
        AmazonEC2Client client = awsStackUtil.createEC2Client(stack);
        DescribeSnapshotsRequest describeSnapshotsRequest = new DescribeSnapshotsRequest()
                .withFilters(new Filter().withName("tag-key").withValues(CLOUDBREAK_EBS_SNAPSHOT));
        DescribeSnapshotsResult describeSnapshotsResult = client.describeSnapshots(describeSnapshotsRequest);
        if (describeSnapshotsResult.getSnapshots().isEmpty()) {
            DescribeAvailabilityZonesResult availabilityZonesResult = client.describeAvailabilityZones(new DescribeAvailabilityZonesRequest()
                    .withFilters(new Filter().withName("region-name").withValues(Regions.valueOf(stack.getRegion()).getName())));
            CreateVolumeResult volumeResult = client.createVolume(new CreateVolumeRequest()
                    .withSize(SNAPSHOT_VOLUME_SIZE)
                    .withAvailabilityZone(availabilityZonesResult.getAvailabilityZones().get(0).getZoneName())
                    .withEncrypted(true));
            EbsVolumeStatePollerObject ebsVolumeStateObject =
                    new EbsVolumeStatePollerObject(stack, volumeResult, volumeResult.getVolume().getVolumeId(), client);
            PollingResult pollingResult = ebsVolumeStatePollingService
                    .pollWithTimeout(ebsVolumeStateCheckerTask, ebsVolumeStateObject, POLLING_INTERVAL, INFINITE_ATTEMPTS);
            if (PollingResult.isSuccess(pollingResult)) {
                CreateSnapshotResult snapshotResult = client.createSnapshot(
                        new CreateSnapshotRequest().withVolumeId(volumeResult.getVolume().getVolumeId()).withDescription("Encrypted snapshot"));
                SnapshotReadyPollerObject snapshotReadyPollerObject =
                        new SnapshotReadyPollerObject(stack, snapshotResult, snapshotResult.getSnapshot().getSnapshotId(), client);
                pollingResult = snapshotReadyPollingService
                        .pollWithTimeout(snapshotReadyCheckerTask, snapshotReadyPollerObject, POLLING_INTERVAL, INFINITE_ATTEMPTS);
                if (PollingResult.isSuccess(pollingResult)) {
                    CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                            .withTags(ImmutableList.of(new Tag().withKey(CLOUDBREAK_EBS_SNAPSHOT).withValue(CLOUDBREAK_EBS_SNAPSHOT)))
                            .withResources(snapshotResult.getSnapshot().getSnapshotId());
                    client.createTags(createTagsRequest);
                    return Optional.of(snapshotResult.getSnapshot().getSnapshotId());
                }
            }
        } else {
            return Optional.of(describeSnapshotsResult.getSnapshots().get(0).getSnapshotId());
        }
        return Optional.absent();
    }

    private boolean isEncryptedVolumeRequested(Stack stack) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (((AwsTemplate) instanceGroup.getTemplate()).isEncrypted()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addInstances(Stack stack, String userData, Integer instanceCount, String instanceGroup) {
        MDCBuilder.buildMdcContext(stack);
        InstanceGroup instanceGroupByInstanceGroupName = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        Integer requiredInstances = instanceGroupByInstanceGroupName.getNodeCount() + instanceCount;
        Regions region = Regions.valueOf(stack.getRegion());
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);
        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup);
        amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(asGroupName)
                .withMaxSize(requiredInstances)
                .withDesiredCapacity(requiredInstances));
        LOGGER.info("Updated AutoScaling group's desiredCapacity: [stack: '{}', from: '{}', to: '{}']", stack.getId(),
                instanceGroupByInstanceGroupName.getNodeCount(),
                instanceGroupByInstanceGroupName.getNodeCount() + instanceCount);
        AutoScalingGroupReady asGroupReady = new AutoScalingGroupReady(stack, amazonEC2Client, amazonASClient, asGroupName, requiredInstances);
        LOGGER.info("Polling autoscaling group until new instances are ready. [stack: {}, asGroup: {}]", stack.getId(), asGroupName);
        PollingResult pollingResult = autoScalingGroupReadyPollingService
                .pollWithTimeout(asGroupStatusCheckerTask, asGroupReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        if (isSuccess(pollingResult)) {
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, stack.getId());
//            reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT,
//                    Event.wrap(new AddInstancesComplete(CloudPlatform.AWS, stack.getId(), null, instanceGroup)));
        }
        return true;
    }

    /**
     * If the AutoScaling group has some suspended scaling policies it causes that the CloudFormation stack delete won't be able to remove the ASG.
     * In this case the ASG size is reduced to zero and the processes are resumed first.
     */
    @Override
    public boolean removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        MDCBuilder.buildMdcContext(stack);
        Regions region = Regions.valueOf(stack.getRegion());
        AwsCredential credential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(region, credential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(region, credential);

        String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup);
        DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds)
                .withShouldDecrementDesiredCapacity(true);
        amazonASClient.detachInstances(detachInstancesRequest);
        amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        LOGGER.info("Terminated instances in stack '{}': '{}'", stack.getId(), instanceIds);
//        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, stack.getId());
//        reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true, instanceIds, instanceGroup)));
        return true;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String userData) throws UpdateFailedException {
        String cFStackName = cfStackUtil.getCfStackName(stack);
        String snapshotId = getEbsSnapshotIdIfNeeded(stack);
        UpdateStackRequest updateStackRequest = new UpdateStackRequest()
                .withStackName(cFStackName)
                .withTemplateBody(cfTemplateBuilder.build(stack, snapshotId, stack.isExistingVPC(), awsCloudformationTemplatePath))
                .withParameters(getStackParameters(stack, userData, (AwsCredential) stack.getCredential(), stack.getName(), stack.isExistingVPC()));
        AmazonCloudFormationClient cloudFormationClient = awsStackUtil.createCloudFormationClient(stack);
        cloudFormationClient.updateStack(updateStackRequest);
        List<StackStatus> errorStatuses = Arrays.asList(UPDATE_ROLLBACK_COMPLETE, UPDATE_ROLLBACK_FAILED);
        CloudFormationStackPollerObject stackPollerContext = new CloudFormationStackPollerObject(cloudFormationClient, UPDATE_COMPLETE,
                UPDATE_ROLLBACK_IN_PROGRESS, errorStatuses, stack);
        try {
            PollingResult pollingResult = cloudFormationPollingService
                            .pollWithTimeout(cloudFormationStackStatusChecker, stackPollerContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
            if (isExited(pollingResult)) {
                throw new UpdateFailedException(new IllegalStateException());
            }
        } catch (CloudFormationStackException e) {
            throw new UpdateFailedException(e);
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
            String cFStackName = resource.getResourceName();
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", stack.getId(), cFStackName);
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                client.describeStacks(describeStacksRequest);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().equals("Stack:" + cFStackName + " does not exist")) {
                    LOGGER.info("AWS CloudFormation stack not found, publishing {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
                    return;
                } else {
                    throw e;
                }
            }
            resumeAutoScalingPolicies(stack, awsCredential);
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
            client.deleteStack(deleteStackRequest);
            List<StackStatus> errorStatuses = Arrays.asList(DELETE_FAILED);
            CloudFormationStackPollerObject stackPollerContext = new CloudFormationStackPollerObject(client, DELETE_COMPLETE,
                    DELETE_FAILED, errorStatuses, stack);
            try {
                PollingResult pollingResult = cloudFormationPollingService
                        .pollWithTimeout(cloudFormationStackStatusChecker, stackPollerContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
                if (isSuccess(pollingResult)) {
                    LOGGER.info("CloudFormation stack({}) delete completed. Publishing {} event.", cFStackName, ReactorConfig.DELETE_COMPLETE_EVENT);
                }
            } catch (CloudFormationStackException e) {
                LOGGER.error(String.format("Failed to delete CloudFormation stack: %s, id:%s", cFStackName, stack.getId()), e);
                throw e;
            }
        } else {
            LOGGER.info("No resource saved for stack, publishing {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
        }
    }

    private List<Parameter> getStackParameters(Stack stack, String userData, AwsCredential awsCredential, String stackName, boolean existingVPC) {
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("CBUserData").withParameterValue(userData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(awsCredential.getRoleArn()),
                new Parameter().withParameterKey("KeyName").withParameterValue(awsCredential.getKeyPairName()),
                new Parameter().withParameterKey("AMI").withParameterValue(stack.getImage()),
                new Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(stack, awsCredential))
        ));
        if (existingVPC) {
            parameters.add(new Parameter().withParameterKey("VPCId").withParameterValue(stack.getParameters().get("vpcId")));
            parameters.add(new Parameter().withParameterKey("SubnetCIDR").withParameterValue(stack.getParameters().get("subnetCIDR")));
            parameters.add(new Parameter().withParameterKey("InternetGatewayId").withParameterValue(stack.getParameters().get("internetGatewayId")));
        }
        return parameters;
    }

    private String getRootDeviceName(Stack stack, AwsCredential awsCredential) {
        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(Regions.valueOf(stack.getRegion()), awsCredential);
        DescribeImagesResult images = ec2Client.describeImages(new DescribeImagesRequest().withImageIds(stack.getImage()));
        Image image = images.getImages().get(0);
        if (image != null) {
            return image.getRootDeviceName();
        } else {
            throw new StackCreationFailureException(String.format("Couldn't describe AMI '%s'.", stack.getImage()));
        }
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
            Collection<String> instances = new ArrayList<>();
            for (InstanceMetaData instance : instanceGroup.getInstanceMetaData()) {
                if (instance.getInstanceGroup().getGroupName().equals(instanceGroup.getGroupName())) {
                    instances.add(instance.getInstanceId());
                }
            }
            try {
                if (stopped) {
                    amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName));
                    amazonEC2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instances));
                    awsPollingService.pollWithTimeout(
                            awsInstanceStatusCheckerTask,
                            new AwsInstances(stack, amazonEC2Client, new ArrayList(instances), "Stopped"),
                            AmbariClusterConnector.POLLING_INTERVAL,
                            AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
                } else {
                    amazonEC2Client.startInstances(new StartInstancesRequest().withInstanceIds(instances));
                    PollingResult pollingResult = awsPollingService.pollWithTimeout(
                            awsInstanceStatusCheckerTask,
                            new AwsInstances(stack, amazonEC2Client, new ArrayList(instances), "Running"),
                            AmbariClusterConnector.POLLING_INTERVAL,
                            AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
                    if (isSuccess(pollingResult)) {
                        amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                        updateInstanceMetadata(stack, amazonEC2Client, stack.getRunningInstanceMetaData(), instances);
                    }
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to %s AWS instances on stack: %s", stopped ? "stop" : "start", stack.getId()), e);
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
                        String publicIp = instance.getPublicIpAddress();
                        if (metaData.getAmbariServer()) {
                            stack.setAmbariIp(publicIp);
                            Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                            stack.setCluster(cluster);
                            stackRepository.save(stack);
                        }
                        metaData.setPublicIp(publicIp);
                        instanceMetaDataRepository.save(metaData);
                        break;
                    }
                }
            }
        }
    }

    private void resumeAutoScalingPolicies(Stack stack, AwsCredential awsCredential) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            try {
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
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().matches("Resource.*does not exist for stack.*")) {
                    MDCBuilder.buildMdcContext(stack);
                    LOGGER.info(e.getErrorMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        return;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    protected CreateStackRequest createStackRequest() {
        return new CreateStackRequest();
    }

}
