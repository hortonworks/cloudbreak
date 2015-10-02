package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_CF_TEMPLATE_PATH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
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
    @Inject
    private AwsClient awsClient;
    @Inject
    private CloudFormationStackUtil cfStackUtil;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Value("${cb.aws.cf.template.path:" + CB_AWS_CF_TEMPLATE_PATH + "}")
    private String awsCloudformationTemplatePath;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        Long stackId = ac.getCloudContext().getId();
        CloudCredential awsCredential = ac.getCloudCredential();
        AmazonCloudFormationClient client = awsClient.createCloudFormationClient(ac.getCloudCredential());
        String cFStackName = cfStackUtil.getCfStackName(ac);
        String snapshotId = getEbsSnapshotIdIfNeeded(ac, stack);
        String cfTemplate = cloudFormationTemplateBuilder.build(stack, snapshotId, isExistingVPC(stack.getNetwork()), awsCloudformationTemplatePath);
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.valueOf(stack.getParameters().get("onFailureActionAction")))
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
        CloudResource cloudFormationStack = new CloudResource.Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();

        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, stackId);

        AmazonEC2Client amazonEC2Client = awsClient.createAccess(ac.getCloudCredential());
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(ac.getCloudCredential());
        AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest().withDomain(DomainType.Vpc);
        AllocateAddressResult allocateAddressResult = amazonEC2Client.allocateAddress(allocateAddressRequest);


        //Resource reservedIp = new Resource(ResourceType.AWS_RESERVED_IP, allocateAddressResult.getAllocationId(), stack, null);
        //stack = stackUpdater.addStackResources(stackId, Arrays.asList(reservedIp));
        //String gateWayGroupName = stack.getGatewayInstanceGroup().getGroupName();
        //List<String> instanceIds = cfStackUtil.getInstanceIds(stack, amazonASClient, client, gateWayGroupName);
        List<String> instanceIds = new ArrayList<>();
        if (!instanceIds.isEmpty()) {
            AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest()
                    .withAllocationId(allocateAddressResult.getAllocationId())
                    .withInstanceId(instanceIds.get(0));
            amazonEC2Client.associateAddress(associateAddressRequest);
        }
        suspendAutoScaling(ac, stack, cloudFormationStack);
        return check(ac, Arrays.asList(cloudFormationStack));
    }

    private void suspendAutoScaling(AuthenticatedContext ac, CloudStack stack, CloudResource cloudResource) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(ac.getCloudCredential());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), cloudResource);
            amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private void resumeAutoScaling(AuthenticatedContext ac, CloudStack stack, CloudResource cloudResource) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(ac.getCloudCredential());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), cloudResource);
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
                new Parameter().withParameterKey("KeyName").withParameterValue(awsCredentialView.getKeyPairName()),
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
        AmazonEC2Client ec2Client = awsClient.createAccess(ac.getCloudCredential());
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
        AmazonEC2Client client = awsClient.createAccess(ac.getCloudCredential());
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
            /*
            EbsVolumeContext ebsVolumeContext = new EbsVolumeContext(stack, volumeResult.getVolume().getVolumeId());
            PollingResult pollingResult = ebsVolumeStatePollingService
                    .pollWithTimeout(ebsVolumeStateCheckerTask, ebsVolumeContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
            if (PollingResult.isSuccess(pollingResult)) {
                CreateSnapshotResult snapshotResult = client.createSnapshot(
                        new CreateSnapshotRequest().withVolumeId(volumeResult.getVolume().getVolumeId()).withDescription("Encrypted snapshot"));
                SnapshotReadyContext snapshotReadyContext =
                        new SnapshotReadyContext(stack, snapshotResult, snapshotResult.getSnapshot().getSnapshotId());
                pollingResult = snapshotReadyPollingService
                        .pollWithTimeout(snapshotReadyCheckerTask, snapshotReadyContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
                if (PollingResult.isSuccess(pollingResult)) {
                    CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                            .withTags(ImmutableList.of(new Tag().withKey(CLOUDBREAK_EBS_SNAPSHOT).withValue(CLOUDBREAK_EBS_SNAPSHOT)))
                            .withResources(snapshotResult.getSnapshot().getSnapshotId());
                    client.createTags(createTagsRequest);
                    return Optional.of(snapshotResult.getSnapshot().getSnapshotId());
                }
            }*/
        } else {
            return Optional.of(describeSnapshotsResult.getSnapshots().get(0).getSnapshotId());
        }
        return Optional.absent();
    }

    private boolean isEncryptedVolumeRequested(CloudStack stack) {
        for (Group group : stack.getGroups()) {
            for (InstanceTemplate instanceTemplate : group.getInstances()) {
                if (instanceTemplate.getStringParameter("encrypted").equals(AwsEncryption.TRUE.name())) {
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
        if (resources != null) {
            AmazonCloudFormationClient client = awsClient.createCloudFormationClient(ac.getCloudCredential());
            String cFStackName = resources.get(0).getName();
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", ac.getCloudContext().getId(), cFStackName);
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                client.describeStacks(describeStacksRequest);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().contains(cFStackName + " does not exist")) {
                    AmazonEC2Client amazonEC2Client = awsClient.createAccess(ac.getCloudCredential());
                    releaseReservedIp(stack, amazonEC2Client);
                    return Arrays.asList();
                } else {
                    throw e;
                }
            }
            resumeAutoScalingPolicies(ac, stack, resources.get(0));
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
            client.deleteStack(deleteStackRequest);
            List<StackStatus> errorStatuses = Arrays.asList(DELETE_FAILED);
            //CloudFormationStackContext cfStackContext = new CloudFormationStackContext(DELETE_COMPLETE, DELETE_FAILED, errorStatuses, stack);
            //PollingResult pollingResult = cloudFormationPollingService
            //        .pollWithTimeout(cloudFormationStackStatusChecker, cfStackContext, POLLING_INTERVAL, INFINITE_ATTEMPTS);
            //if (!isSuccess(pollingResult)) {
            //    LOGGER.error(String.format("Failed to delete CloudFormation stack: %s, id:%s", cFStackName, stack.getId()));
            //    throw new AwsResourceException(String.format("Failed to delete CloudFormation stack; polling result: '%s'.", pollingResult));
            //}
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(ac.getCloudCredential());
            releaseReservedIp(stack, amazonEC2Client);
        } else {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(ac.getCloudCredential());
            releaseReservedIp(stack, amazonEC2Client);
            LOGGER.info("No CloudFormation stack saved for stack.");
        }
        /*
        for (CloudResource resource : resources) {

        }*/

        return check(ac, resources);
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack, CloudResource cloudResource) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), cloudResource);
                if (asGroupName != null) {
                    AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(ac.getCloudCredential());
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

    private void releaseReservedIp(CloudStack stack, AmazonEC2Client client) {
        /*
        Resource elasticIpResource = new Resource(); //= stack.getResourceByType(ResourceType.AWS_RESERVED_IP);
        if (elasticIpResource != null && elasticIpResource.getResourceName() != null) {
            Address address;
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
        }*/
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        // InstanceGroup instanceGroupByInstanceGroupName = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        // Integer requiredInstances = instanceGroupByInstanceGroupName.getNodeCount() + instanceCount;

        resumeAutoScaling(authenticatedContext, stack, resources.get(0));

        //AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(authenticatedContext.getCloudCredential());
        //String asGroupName = cfStackUtil.getAutoscalingGroupName(stack, instanceGroup);

        // amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
        //         .withAutoScalingGroupName(asGroupName)
        //         .withMaxSize(requiredInstances)
        //         .withDesiredCapacity(requiredInstances));
        // LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', from: '{}', to: '{}']", stack.getId(),
        //         instanceGroupByInstanceGroupName.getNodeCount(),
        //         instanceGroupByInstanceGroupName.getNodeCount() + instanceCount);
        // AutoScalingGroupReadyContext asGroupReady = new AutoScalingGroupReadyContext(stack, asGroupName, requiredInstances);
        // LOGGER.info("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", stack.getId(), asGroupName);
        // PollingResult pollingResult = autoScalingGroupReadyPollingService
        //         .pollWithTimeout(asGroupStatusCheckerTask, asGroupReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        // if (!isSuccess(pollingResult)) {
        //     throw new AwsResourceException("Failed to create CloudFormation stack, because polling reached an invalid end state.");
        // }
        suspendAutoScaling(authenticatedContext, stack, resources.get(0));
        return Collections.emptyList();
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(auth.getCloudCredential());
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(auth.getCloudCredential());

        String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(), resources.get(0));
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

}
