package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_CF_TEMPLATE_PATH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
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
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.AdjustmentType;
import com.sequenceiq.cloudbreak.common.type.AwsEncryption;
import com.sequenceiq.cloudbreak.common.type.CloudRegion;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
//import com.sequenceiq.cloudbreak.domain.Resource;

@Service
public class AwsResourceConnector implements ResourceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceConnector.class);
    private static final String CLOUDBREAK_EBS_SNAPSHOT = "cloudbreak-ebs-snapshot";
    private static final int SNAPSHOT_VOLUME_SIZE = 10;

    private static final List<String> SUSPENDED_PROCESSES = Arrays.asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");
    private static final List<StackStatus> ERROR_STATUSES = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);

    @Inject
    private AwsClient awsClient;
    @Inject
    private CloudFormationStackUtil cfStackUtil;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;
    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;
    @Inject
    private ResourceNotifier resourceNotifier;

    @Value("${cb.aws.cf.template.new.path:" + CB_AWS_CF_TEMPLATE_PATH + "}")
    private String awsCloudformationTemplatePath;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        Long stackId = ac.getCloudContext().getId();
        AmazonCloudFormationClient client = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), stack.getRegion());
        String cFStackName = cfStackUtil.getCfStackName(ac);
        String snapshotId = getEbsSnapshotIdIfNeeded(ac, stack);
        String cfTemplate = cloudFormationTemplateBuilder.build(stack, snapshotId, isExistingVPC(stack.getNetwork()), awsCloudformationTemplatePath);
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
        importKeyPairs(ac, stack.getRegion());
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cfTemplate)
                .withParameters(
                        getStackParameters(
                                ac,
                                stack.getImage().getUserData(InstanceGroupType.CORE),
                                stack.getImage().getUserData(InstanceGroupType.GATEWAY),
                                stack,
                                cFStackName,
                                isExistingVPC(stack.getNetwork())
                        )
                );
        client.createStack(createStackRequest);
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, stackId);
        PollTask<Boolean> task = awsPollTaskFactory.newAwsCloudformationStatusCheckerTask(ac, client,
                CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, cFStackName, true);
        try {
            Boolean statePollerResult = task.call();
            if (!task.completed(statePollerResult)) {
                syncPollingScheduler.schedule(task);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), stack.getRegion());
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), stack.getRegion());
        AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest().withDomain(DomainType.Vpc);
        AllocateAddressResult allocateAddressResult = amazonEC2Client.allocateAddress(allocateAddressRequest);

        CloudResource reservedIp = new CloudResource.Builder().type(ResourceType.AWS_RESERVED_IP).name(allocateAddressResult.getAllocationId()).build();
        resourceNotifier.notifyAllocation(reservedIp, ac.getCloudContext());
        List<String> instanceIds = cfStackUtil.getInstanceIds(ac, amazonASClient, client,
                cfStackUtil.getAutoscalingGroupName(ac, client, stack.getGroups().get(0).getName()));
        if (!instanceIds.isEmpty()) {
            AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest()
                    .withAllocationId(allocateAddressResult.getAllocationId())
                    .withInstanceId(instanceIds.get(0));
            amazonEC2Client.associateAddress(associateAddressRequest);
        }
        suspendAutoScaling(ac, stack);
        return check(ac, Arrays.asList(reservedIp));
    }

    private void suspendAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), stack.getRegion());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), stack.getRegion());
            amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private void resumeAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), stack.getRegion());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), stack.getRegion());
            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private List<Parameter> getStackParameters(AuthenticatedContext ac, String coreGroupUserData, String gateWayUserData, CloudStack stack, String stackName,
            boolean existingVPC) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(ac.getCloudCredential());

        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("CBUserData").withParameterValue(coreGroupUserData),
                new Parameter().withParameterKey("CBGateWayUserData").withParameterValue(gateWayUserData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(ac.getCloudContext().getOwner()),
                new Parameter().withParameterKey("KeyName").withParameterValue(getKeyPairName(ac)),
                new Parameter().withParameterKey("AMI").withParameterValue(stack.getImage().getImageName()),
                new Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(ac, stack))
        ));
        if (existingVPC) {
            parameters.add(new Parameter().withParameterKey("VPCId").withParameterValue(stack.getNetwork().getStringParameter("vpcId")));
            parameters.add(new Parameter().withParameterKey("SubnetCIDR").withParameterValue(stack.getNetwork().getSubnet().getCidr()));
            parameters.add(new Parameter().withParameterKey("InternetGatewayId").withParameterValue(stack.getNetwork().getStringParameter("internetGatewayId")));
        }
        return parameters;
    }

    private String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), cloudStack.getRegion());
        DescribeImagesResult images = ec2Client.describeImages(new DescribeImagesRequest().withImageIds(cloudStack.getImage().getImageName()));
        if (images.getImages().isEmpty()) {
            throw new CloudConnectorException(String.format("AMI is not available: '%s'.", cloudStack.getImage().getImageName()));
        }
        Image image = images.getImages().get(0);
        if (image == null) {
            throw new CloudConnectorException(String.format("Couldn't describe AMI '%s'.", cloudStack.getImage().getImageName()));
        }
        return image.getRootDeviceName();
    }

    private String getEbsSnapshotIdIfNeeded(AuthenticatedContext ac, CloudStack cloudStack) {
        if (isEncryptedVolumeRequested(cloudStack)) {
            Optional<String> snapshot = createSnapshotIfNeeded(ac, cloudStack);
            if (snapshot.isPresent()) {
                return snapshot.orNull();
            } else {
                throw new CloudConnectorException(String.format("Failed to create Ebs encrypted volume on stack: %s", ac.getCloudContext().getId()));
            }
        } else {
            return null;
        }
    }

    private Optional<String> createSnapshotIfNeeded(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEC2Client client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
        DescribeSnapshotsRequest describeSnapshotsRequest = new DescribeSnapshotsRequest()
                .withFilters(new Filter().withName("tag-key").withValues(CLOUDBREAK_EBS_SNAPSHOT));
        DescribeSnapshotsResult describeSnapshotsResult = client.describeSnapshots(describeSnapshotsRequest);
        if (describeSnapshotsResult.getSnapshots().isEmpty()) {
            DescribeAvailabilityZonesResult availabilityZonesResult = client.describeAvailabilityZones(new DescribeAvailabilityZonesRequest()
                    .withFilters(new Filter().withName("region-name").withValues(CloudRegion.valueOf(cloudStack.getRegion()).value())));
            CreateVolumeResult volumeResult = client.createVolume(new CreateVolumeRequest()
                    .withSize(SNAPSHOT_VOLUME_SIZE)
                    .withAvailabilityZone(availabilityZonesResult.getAvailabilityZones().get(0).getZoneName())
                    .withEncrypted(true));
            PollTask<Boolean> newEbsVolumeStatusCheckerTask = awsPollTaskFactory
                    .newEbsVolumeStatusCheckerTask(ac, cloudStack, client, volumeResult.getVolume().getVolumeId());
            try {
                Boolean statePollerResult = newEbsVolumeStatusCheckerTask.call();
                if (!newEbsVolumeStatusCheckerTask.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(newEbsVolumeStatusCheckerTask);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
            CreateSnapshotResult snapshotResult = client.createSnapshot(
                    new CreateSnapshotRequest().withVolumeId(volumeResult.getVolume().getVolumeId()).withDescription("Encrypted snapshot"));
            PollTask<Boolean> newCreateSnapshotReadyStatusCheckerTask = awsPollTaskFactory.newCreateSnapshotReadyStatusCheckerTask(ac, snapshotResult,
                    snapshotResult.getSnapshot().getSnapshotId(), client);
            try {
                Boolean statePollerResult = newCreateSnapshotReadyStatusCheckerTask.call();
                if (!newCreateSnapshotReadyStatusCheckerTask.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(newCreateSnapshotReadyStatusCheckerTask);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
            CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                    .withTags(ImmutableList.of(new Tag().withKey(CLOUDBREAK_EBS_SNAPSHOT).withValue(CLOUDBREAK_EBS_SNAPSHOT)))
                    .withResources(snapshotResult.getSnapshot().getSnapshotId());
            client.createTags(createTagsRequest);
            return Optional.of(snapshotResult.getSnapshot().getSnapshotId());
        } else {
            return Optional.of(describeSnapshotsResult.getSnapshots().get(0).getSnapshotId());
        }
    }

    private boolean isEncryptedVolumeRequested(CloudStack stack) {
        for (Group group : stack.getGroups()) {
            for (InstanceTemplate instanceTemplate : group.getInstances()) {
                if (instanceTemplate.getParameter("encrypted", AwsEncryption.class).equals(AwsEncryption.TRUE)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isExistingVPC(Network network) {
        return network.getStringParameter("subnetCIDR") != null
                && network.getStringParameter("vpcId") != null
                && network.getStringParameter("internetGatewayId") != null;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.info("Deleting stack: {}", ac.getCloudContext().getId());
        if (resources != null && !resources.isEmpty()) {
            AmazonCloudFormationClient client = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), stack.getRegion());
            String cFStackName = getCloudFormationStackResource(resources).getName();
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", ac.getCloudContext().getId(), cFStackName);
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                client.describeStacks(describeStacksRequest);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().contains(cFStackName + " does not exist")) {
                    AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
                    releaseReservedIp(amazonEC2Client, resources);
                    return Arrays.asList();
                } else {
                    throw e;
                }
            }
            resumeAutoScalingPolicies(ac, stack, getReservedIp(resources));
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
            client.deleteStack(deleteStackRequest);
            PollTask<Boolean> task = awsPollTaskFactory.newAwsCloudformationStatusCheckerTask(ac, client,
                    DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, cFStackName, false);
            try {
                Boolean statePollerResult = task.call();
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
            releaseReservedIp(amazonEC2Client, resources);
            deleteKeyPair(ac, stack.getRegion());
        } else {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
            releaseReservedIp(amazonEC2Client, resources);
            LOGGER.info("No CloudFormation stack saved for stack.");
        }
        return check(ac, resources);
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack, CloudResource cloudResource) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), stack.getRegion());
                if (asGroupName != null) {
                    AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                            stack.getRegion());
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
                } else {
                    LOGGER.info("Autoscaling Group's physical id is null (the resource doesn't exist), it is not needed to resume scaling policies.");
                }
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().matches("Resource.*does not exist for stack.*") || e.getErrorMessage().matches("Stack '.*' does not exist.*")) {
                    LOGGER.info(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    private void releaseReservedIp(AmazonEC2Client client, List<CloudResource> resources) {
        CloudResource elasticIpResource = getReservedIp(resources);
        if (elasticIpResource != null && elasticIpResource.getName() != null) {
            Address address;
            try {
                DescribeAddressesResult describeResult = client.describeAddresses(
                        new DescribeAddressesRequest().withAllocationIds(elasticIpResource.getName()));
                address = describeResult.getAddresses().get(0);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().equals("The allocation ID '" + elasticIpResource.getName() + "' does not exist")) {
                    LOGGER.warn("Elastic IP with allocation ID '{}' not found. Ignoring IP release.");
                    return;
                } else {
                    throw e;
                }
            }
            if (address.getAssociationId() != null) {
                client.disassociateAddress(new DisassociateAddressRequest().withAssociationId(elasticIpResource.getName()));
            }
            client.releaseAddress(new ReleaseAddressRequest().withAllocationId(elasticIpResource.getName()));
        }
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        resumeAutoScaling(ac, stack);

        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                stack.getRegion());
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                stack.getRegion());

        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());

            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(asGroupName)
                    .withMaxSize(group.getInstances().size())
                    .withDesiredCapacity(group.getInstances().size()));
            LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', to: '{}']", ac.getCloudContext().getId(),
                    resources.size());
            LOGGER.info("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", ac.getCloudContext().getId(),
                    asGroupName);
            PollTask<Boolean> task = awsPollTaskFactory.newASGroupStatusCheckerTask(ac, asGroupName, group.getInstances().size(), awsClient, cfStackUtil);
            try {
                Boolean statePollerResult = task.call();
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
        }
        suspendAutoScaling(ac, stack);
        return Arrays.asList(new CloudResourceStatus(getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(auth.getCloudCredential()), stack.getRegion());
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(auth.getCloudCredential()), auth.getCloudContext().getRegion());

        String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(), stack.getRegion());
        List<String> instanceIds = new ArrayList<>();
        for (CloudInstance vm : vms) {
            instanceIds.add(vm.getInstanceId());
        }
        DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds)
                .withShouldDecrementDesiredCapacity(true);
        amazonASClient.detachInstances(detachInstancesRequest);
        amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        LOGGER.info("Terminated instances in stack '{}': '{}'", auth.getCloudContext().getId(), instanceIds);
        return check(auth, resources);
    }

    private CloudResource getCloudFormationStackResource(List<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getType().equals(ResourceType.CLOUDFORMATION_STACK)) {
                return cloudResource;
            }
        }
        return null;
    }

    private CloudResource getReservedIp(List<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getType().equals(ResourceType.AWS_RESERVED_IP)) {
                return cloudResource;
            }
        }
        return null;
    }

    private void importKeyPairs(AuthenticatedContext ac, String region) {
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        try {
            LOGGER.info(String.format("Importing publickey to %s region on AWS", region));
            AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
            ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(getKeyPairName(ac), awsCredential.getPublicKey());
            client.importKeyPair(importKeyPairRequest);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CloudConnectorException(errorMessage, e);
        }
    }

    private void deleteKeyPair(AuthenticatedContext ac, String region) {
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        try {
            AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
            DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(getKeyPairName(ac));
            client.deleteKeyPair(deleteKeyPairRequest);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                    awsCredential.getRoleArn(), region, e.getMessage());
            LOGGER.error(errorMessage, e);
        }
    }

    private String getKeyPairName(AuthenticatedContext ac) {
        return String.format("%s%s%s%s", ac.getCloudCredential().getName(), ac.getCloudCredential().getId(),
                ac.getCloudContext().getName(), ac.getCloudContext().getId());
    }

}
