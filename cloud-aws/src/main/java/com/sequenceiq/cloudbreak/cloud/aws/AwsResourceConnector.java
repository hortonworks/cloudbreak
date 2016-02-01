package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

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
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
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
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class AwsResourceConnector implements ResourceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceConnector.class);
    private static final String CLOUDBREAK_EBS_SNAPSHOT = "cloudbreak-ebs-snapshot";
    private static final int SNAPSHOT_VOLUME_SIZE = 10;

    private static final List<String> SUSPENDED_PROCESSES = Arrays.asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");
    private static final List<StackStatus> ERROR_STATUSES = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);
    private static final String VPC = "vpcId";
    private static final String IGW = "internetGatewayId";
    private static final String SUBNET = "subnetId";

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
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Value("${cb.aws.cf.template.new.path:}")
    private String awsCloudformationTemplatePath;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {

        String cFStackName = cfStackUtil.getCfStackName(ac);
        CloudResource cloudFormationStack = new CloudResource.Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
        resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

        Long stackId = ac.getCloudContext().getId();
        AmazonCloudFormationClient client = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        String snapshotId = getEbsSnapshotIdIfNeeded(ac, stack);
        Network network = stack.getNetwork();
        boolean existingVPC = isExistingVPC(network);
        boolean existingSubnet = isExistingSubnet(network);
        String existingSubnetCidr = existingSubnet ? getExistingSubnetCidr(ac, stack) : null;
        String cfTemplate = cloudFormationTemplateBuilder.build(ac, stack, snapshotId, existingVPC, existingSubnetCidr, awsCloudformationTemplatePath);
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
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
                                existingVPC,
                                existingSubnet
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
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest().withDomain(DomainType.Vpc);
        AllocateAddressResult allocateAddressResult = amazonEC2Client.allocateAddress(allocateAddressRequest);

        CloudResource reservedIp = new CloudResource.Builder().type(ResourceType.AWS_RESERVED_IP).name(allocateAddressResult.getAllocationId()).build();
        resourceNotifier.notifyAllocation(reservedIp, ac.getCloudContext());
        List<String> instanceIds = cfStackUtil.getInstanceIds(amazonASClient, cfStackUtil.getAutoscalingGroupName(ac, client, stack.getGroups().get(0)
                .getName()));
        if (!instanceIds.isEmpty()) {
            AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest()
                    .withAllocationId(allocateAddressResult.getAllocationId())
                    .withInstanceId(instanceIds.get(0));
            amazonEC2Client.associateAddress(associateAddressRequest);
        }
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        scheduleStatusChecks(stack, ac, cloudFormationClient);
        suspendAutoScaling(ac, stack);
        return check(ac, Arrays.asList(reservedIp));
    }

    private void suspendAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
            amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private void resumeAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private List<Parameter> getStackParameters(AuthenticatedContext ac, String coreGroupUserData, String gateWayUserData, CloudStack stack, String stackName,
            boolean existingVPC, boolean existingSubnet) {
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("CBUserData").withParameterValue(coreGroupUserData),
                new Parameter().withParameterKey("CBGateWayUserData").withParameterValue(gateWayUserData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(ac.getCloudContext().getOwner()),
                new Parameter().withParameterKey("KeyName").withParameterValue(awsClient.getKeyPairName(ac)),
                new Parameter().withParameterKey("AMI").withParameterValue(stack.getImage().getImageName()),
                new Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(ac, stack))
        ));
        if (ac.getCloudContext().getLocation().getAvailabilityZone().value() != null) {
            parameters.add(new Parameter().withParameterKey("AvailabilitySet")
                    .withParameterValue(ac.getCloudContext().getLocation().getAvailabilityZone().value()));
        }
        if (existingVPC) {
            parameters.add(new Parameter().withParameterKey("VPCId").withParameterValue(stack.getNetwork().getStringParameter(VPC)));
            parameters.add(new Parameter().withParameterKey("InternetGatewayId").withParameterValue(stack.getNetwork().getStringParameter(IGW)));
            if (existingSubnet) {
                parameters.add(new Parameter().withParameterKey("SubnetId").withParameterValue(stack.getNetwork().getStringParameter(SUBNET)));
            } else {
                parameters.add(new Parameter().withParameterKey("SubnetCIDR").withParameterValue(stack.getNetwork().getSubnet().getCidr()));
            }
        }
        return parameters;
    }

    private String getExistingSubnetCidr(AuthenticatedContext ac, CloudStack stack) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);
        DescribeSubnetsRequest subnetsRequest = new DescribeSubnetsRequest().withSubnetIds(stack.getNetwork().getStringParameter(SUBNET));
        List<Subnet> subnets = ec2Client.describeSubnets(subnetsRequest).getSubnets();
        if (subnets.isEmpty()) {
            throw new CloudConnectorException("The specified subnet does not exist (maybe it's in a different region).");
        }
        return subnets.get(0).getCidrBlock();
    }

    private String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
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
        AmazonEC2Client client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getLocation().getRegion().value());
        DescribeSnapshotsRequest describeSnapshotsRequest = new DescribeSnapshotsRequest()
                .withFilters(new Filter().withName("tag-key").withValues(CLOUDBREAK_EBS_SNAPSHOT));
        DescribeSnapshotsResult describeSnapshotsResult = client.describeSnapshots(describeSnapshotsRequest);
        if (describeSnapshotsResult.getSnapshots().isEmpty()) {
            DescribeAvailabilityZonesResult availabilityZonesResult = client.describeAvailabilityZones(new DescribeAvailabilityZonesRequest()
                    .withFilters(new Filter().withName("region-name")
                            .withValues(ac.getCloudContext().getLocation().getRegion().value())));
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
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                if (instanceTemplate.getParameter("encrypted", Boolean.class).equals(Boolean.TRUE)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isExistingVPC(Network network) {
        return isNoneEmpty(network.getStringParameter(VPC)) && isNoneEmpty(network.getStringParameter(IGW));
    }

    public boolean isExistingSubnet(Network network) {
        return isNoneEmpty(network.getStringParameter(SUBNET));
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.info("Deleting stack: {}", ac.getCloudContext().getId());
        if (resources != null && !resources.isEmpty()) {
            AmazonCloudFormationClient client = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                    ac.getCloudContext().getLocation().getRegion().value());
            String cFStackName = getCloudFormationStackResource(resources).getName();
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", ac.getCloudContext().getId(), cFStackName);
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                client.describeStacks(describeStacksRequest);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().contains(cFStackName + " does not exist")) {
                    AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                            ac.getCloudContext().getLocation().getRegion().value());
                    releaseReservedIp(amazonEC2Client, resources);
                    return Arrays.asList();
                } else {
                    throw e;
                }
            }
            resumeAutoScalingPolicies(ac, stack);
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
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                    ac.getCloudContext().getLocation().getRegion().value());
            releaseReservedIp(amazonEC2Client, resources);
        } else {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                    ac.getCloudContext().getLocation().getRegion().value());
            releaseReservedIp(amazonEC2Client, resources);
            LOGGER.info("No CloudFormation stack saved for stack.");
        }
        return check(ac, resources);
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), ac.getCloudContext().getLocation().getRegion().value());
                if (asGroupName != null) {
                    AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                            ac.getCloudContext().getLocation().getRegion().value());
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
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());

        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());

            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(asGroupName)
                    .withMaxSize(group.getInstances().size())
                    .withDesiredCapacity(group.getInstances().size()));
            LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', to: '{}']", ac.getCloudContext().getId(),
                    resources.size());
        }
        scheduleStatusChecks(stack, ac, cloudFormationClient);
        suspendAutoScaling(ac, stack);

        return Arrays.asList(new CloudResourceStatus(getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(auth.getCloudCredential()),
                auth.getCloudContext().getLocation().getRegion().value());
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(auth.getCloudCredential()),
                auth.getCloudContext().getLocation().getRegion().value());

        String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(),
                auth.getCloudContext().getLocation().getRegion().value());
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

    private void scheduleStatusChecks(CloudStack stack, AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient) {

        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());
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

}
