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
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_CF_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AwsInstances;

@Service
public class AwsConnector implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConnector.class);
    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int POLLING_INTERVAL = 5000;
    private static final int INFINITE_ATTEMPTS = -1;
    private static final String CLOUDBREAK_EBS_SNAPSHOT = "cloudbreak-ebs-snapshot";
    private static final int SNAPSHOT_VOLUME_SIZE = 10;

    @Value("${cb.aws.cf.template.path:" + CB_AWS_CF_TEMPLATE_PATH + "}")
    private String awsCloudformationTemplatePath;

    @Inject
    private AwsStackUtil awsStackUtil;
    @Inject
    private ASGroupStatusCheckerTask asGroupStatusCheckerTask;
    @Inject
    private CloudFormationTemplateBuilder cfTemplateBuilder;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private CloudFormationStackUtil cfStackUtil;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private PollingService<AwsInstances> awsPollingService;
    @Inject
    private PollingService<AutoScalingGroupReady> autoScalingGroupReadyPollingService;
    @Inject
    private PollingService<CloudFormationStackPollerObject> cloudFormationPollingService;
    @Inject
    private PollingService<EbsVolumeStatePollerObject> ebsVolumeStatePollingService;
    @Inject
    private PollingService<SnapshotReadyPollerObject> snapshotReadyPollingService;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private StackRepository stackRepository;
    @Inject
    private AwsInstanceStatusCheckerTask awsInstanceStatusCheckerTask;
    @Inject
    private CloudFormationStackStatusChecker cloudFormationStackStatusChecker;
    @Inject
    private EbsVolumeStateCheckerTask ebsVolumeStateCheckerTask;
    @Inject
    private SnapshotReadyCheckerTask snapshotReadyCheckerTask;

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        Long stackId = stack.getId();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(Regions.valueOf(stack.getRegion()), awsCredential);
        String cFStackName = cfStackUtil.getCfStackName(stack);
        AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest().withDomain(DomainType.Vpc);
        AllocateAddressResult allocateAddressResult = amazonEC2Client.allocateAddress(allocateAddressRequest);
        Resource reservedIp = new Resource(ResourceType.AWS_RESERVED_IP, allocateAddressResult.getAllocationId(), stack, null);
        stack = stackUpdater.addStackResources(stackId, Arrays.asList(reservedIp));
        String snapshotId = getEbsSnapshotIdIfNeeded(stack);
        AwsNetwork network = (AwsNetwork) stack.getNetwork();
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.valueOf(stack.getOnFailureActionAction().name()))
                .withTemplateBody(cfTemplateBuilder.build(stack, snapshotId, network.isExistingVPC(), awsCloudformationTemplatePath))
                .withParameters(getStackParameters(stack, coreUserData, gateWayUserData, awsCredential, cFStackName, network.isExistingVPC()));
        client.createStack(createStackRequest);
        Resource cloudFormationStackResource = new Resource(ResourceType.CLOUDFORMATION_STACK, cFStackName, stack, null);
        stack = stackUpdater.addStackResources(stackId, Arrays.asList(cloudFormationStackResource));
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, stackId);
        List<StackStatus> errorStatuses = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);
        CloudFormationStackPollerObject stackPollerContext = new CloudFormationStackPollerObject(client, CREATE_COMPLETE, CREATE_FAILED, errorStatuses, stack);

        PollingResult pollingResult = cloudFormationPollingService
                .pollWithTimeout(cloudFormationStackStatusChecker, stackPollerContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
        if (!isSuccess(pollingResult)) {
            throw new AwsResourceException(String.format("Failed to create CloudFormation stack: %s, polling result '%s'",
                    stackId, pollingResult));
        }
        String gateWayGroupName = stack.getGatewayInstanceGroup().getGroupName();

        List<String> instanceIds = cfStackUtil.getInstanceIds(stack, amazonASClient, client, gateWayGroupName);
        if (!instanceIds.isEmpty()) {
            AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest()
                    .withAllocationId(allocateAddressResult.getAllocationId())
                    .withInstanceId(instanceIds.get(0));
            amazonEC2Client.associateAddress(associateAddressRequest);
        }
        return stack.getResources();
    }

    private String getEbsSnapshotIdIfNeeded(Stack stack) {
        if (isEncryptedVolumeRequested(stack)) {
            Optional<String> snapshot = createSnapshotIfNeeded(stack);
            if (snapshot.isPresent()) {
                return snapshot.orNull();
            } else {
                throw new AwsResourceException(String.format("Failed to create Ebs encrypted volume on stack: %s", stack.getId()));
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
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer instanceCount, String instanceGroup) {
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
        if (!isSuccess(pollingResult)) {
            throw new AwsResourceException("Failed to create CloudFormation stack, because polling reached an invalid end state.");
        }
        return Collections.emptySet();
    }

    /**
     * If the AutoScaling group has some suspended scaling policies it causes that the CloudFormation stack delete won't be able to remove the ASG.
     * In this case the ASG size is reduced to zero and the processes are resumed first.
     */
    @Override
    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
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
        return instanceIds;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        String cFStackName = cfStackUtil.getCfStackName(stack);
        String snapshotId = getEbsSnapshotIdIfNeeded(stack);
        AwsNetwork awsNetwork = (AwsNetwork) stack.getNetwork();
        UpdateStackRequest updateStackRequest = new UpdateStackRequest()
                .withStackName(cFStackName)
                .withTemplateBody(cfTemplateBuilder.build(stack, snapshotId, awsNetwork.isExistingVPC(), awsCloudformationTemplatePath))
                .withParameters(getStackParameters(stack, coreUserData, gateWayUserData,
                        (AwsCredential) stack.getCredential(), stack.getName(), awsNetwork.isExistingVPC()));
        AmazonCloudFormationClient cloudFormationClient = awsStackUtil.createCloudFormationClient(stack);
        cloudFormationClient.updateStack(updateStackRequest);
        List<StackStatus> errorStatuses = Arrays.asList(UPDATE_ROLLBACK_COMPLETE, UPDATE_ROLLBACK_FAILED);
        CloudFormationStackPollerObject stackPollerContext = new CloudFormationStackPollerObject(cloudFormationClient, UPDATE_COMPLETE,
                UPDATE_ROLLBACK_IN_PROGRESS, errorStatuses, stack);
        PollingResult pollingResult = cloudFormationPollingService
                .pollWithTimeout(cloudFormationStackStatusChecker, stackPollerContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
        if (!isSuccess(pollingResult)) {
            LOGGER.info("Cloud Formation Stack status check result: {} . ", pollingResult);
            throw new AwsResourceException(String.format("Cloud Formation update failed. polling result: '%s', stack id: '%' ",
                    pollingResult, stack.getId()));
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
                if (e.getErrorMessage().contains(cFStackName + " does not exist")) {
                    AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);
                    releaseReservedIp(stack, amazonEC2Client);
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
            PollingResult pollingResult = cloudFormationPollingService
                    .pollWithTimeout(cloudFormationStackStatusChecker, stackPollerContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
            if (!isSuccess(pollingResult)) {
                LOGGER.error(String.format("Failed to delete CloudFormation stack: %s, id:%s", cFStackName, stack.getId()));
                throw new AwsResourceException(String.format("Failed to delete CloudFormation stack; polling result: '%s'.", pollingResult));
            }
            AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);
            releaseReservedIp(stack, amazonEC2Client);
        } else {
            AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);
            releaseReservedIp(stack, amazonEC2Client);
            LOGGER.info("No CloudFormation stack saved for stack.");
        }
    }

    private void releaseReservedIp(Stack stack, AmazonEC2Client client) {
        Resource elasticIpResource = stack.getResourceByType(ResourceType.AWS_RESERVED_IP);
        if (elasticIpResource != null && elasticIpResource.getResourceName() != null) {
            Address address = null;
            try {
                DescribeAddressesResult describeResult = client.describeAddresses(
                        new DescribeAddressesRequest().withAllocationIds(elasticIpResource.getResourceName()));
                address = describeResult.getAddresses().get(0);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().equals("The allocation ID '" + elasticIpResource.getResourceName() + "' does not exist")) {
                    LOGGER.warn("Elastic IP with allocation ID '{}' not found. Ignoring IP release.");
                    return;
                } else {
                    throw e;
                }
            }
            if (address.getAssociationId() != null) {
                client.disassociateAddress(new DisassociateAddressRequest().withAssociationId(elasticIpResource.getResourceName()));
            }
            client.releaseAddress(new ReleaseAddressRequest().withAllocationId(elasticIpResource.getResourceName()));
        }
    }

    private List<Parameter> getStackParameters(Stack stack, String coreGroupUserData, String gateWayUserData, AwsCredential awsCredential, String stackName,
            boolean existingVPC) {
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("CBUserData").withParameterValue(coreGroupUserData),
                new Parameter().withParameterKey("CBGateWayUserData").withParameterValue(gateWayUserData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(awsCredential.getRoleArn()),
                new Parameter().withParameterKey("KeyName").withParameterValue(awsCredential.getKeyPairName()),
                new Parameter().withParameterKey("AMI").withParameterValue(stack.getImage()),
                new Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(stack, awsCredential))
        ));
        if (existingVPC) {
            AwsNetwork awsNetwork = (AwsNetwork) stack.getNetwork();
            parameters.add(new Parameter().withParameterKey("VPCId").withParameterValue(awsNetwork.getVpcId()));
            parameters.add(new Parameter().withParameterKey("SubnetCIDR").withParameterValue(awsNetwork.getSubnetCIDR()));
            parameters.add(new Parameter().withParameterKey("InternetGatewayId").withParameterValue(awsNetwork.getInternetGatewayId()));
        }
        return parameters;
    }

    private String getRootDeviceName(Stack stack, AwsCredential awsCredential) {
        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(Regions.valueOf(stack.getRegion()), awsCredential);
        DescribeImagesResult images = ec2Client.describeImages(new DescribeImagesRequest().withImageIds(stack.getImage()));
        Image image = images.getImages().get(0);
        if (image == null) {
            throw new AwsResourceException(String.format("Couldn't describe AMI '%s'.", stack.getImage()));
        }
        return image.getRootDeviceName();
    }

    private boolean setStackState(Stack stack, boolean stopped) {
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
                    instances = removeInstanceIdsWhichAreNotInCorrectState(instances, amazonEC2Client, "Stopped");
                    amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName));
                    amazonEC2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instances));
                    awsPollingService.pollWithTimeout(
                            awsInstanceStatusCheckerTask,
                            new AwsInstances(stack, amazonEC2Client, new ArrayList(instances), "Stopped"),
                            AmbariOperationService.AMBARI_POLLING_INTERVAL,
                            AmbariOperationService.MAX_ATTEMPTS_FOR_AMBARI_OPS);
                } else {
                    instances = removeInstanceIdsWhichAreNotInCorrectState(instances, amazonEC2Client, "Running");
                    amazonEC2Client.startInstances(new StartInstancesRequest().withInstanceIds(instances));
                    PollingResult pollingResult = awsPollingService.pollWithTimeout(
                            awsInstanceStatusCheckerTask,
                            new AwsInstances(stack, amazonEC2Client, new ArrayList(instances), "Running"),
                            AmbariOperationService.AMBARI_POLLING_INTERVAL,
                            AmbariOperationService.MAX_ATTEMPTS_FOR_AMBARI_OPS);
                    if (!isSuccess(pollingResult)) {
                        LOGGER.warn("Instances are not in Running state; polling result: {} ", pollingResult);
                        throw new AwsResourceException("Instances are not in running state!");
                    }
                    amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                    updateInstanceMetadata(stack, amazonEC2Client, stack.getRunningInstanceMetaData(), instances);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to %s AWS instances on stack: %s", stopped ? "stop" : "start", stack.getId()), e);
                result = false;
            }
        }
        return result;
    }

    private Collection<String> removeInstanceIdsWhichAreNotInCorrectState(Collection<String> instances, AmazonEC2Client amazonEC2Client, String state) {
        DescribeInstancesResult describeInstances = amazonEC2Client.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instances));
        for (Reservation reservation : describeInstances.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (state.equalsIgnoreCase(instance.getState().getName())) {
                    instances.remove(instance.getInstanceId());
                }
            }
        }
        return instances;
    }

    private void updateInstanceMetadata(Stack stack, AmazonEC2Client amazonEC2Client, Set<InstanceMetaData> instanceMetaData, Collection<String> instances) {
        DescribeInstancesResult describeResult = amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instances));
        for (Reservation reservation : describeResult.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                for (InstanceMetaData metaData : instanceMetaData) {
                    if (metaData.getInstanceId().equals(instance.getInstanceId())) {
                        String publicIp = instance.getPublicIpAddress();
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
                    LOGGER.info(e.getMessage());
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
