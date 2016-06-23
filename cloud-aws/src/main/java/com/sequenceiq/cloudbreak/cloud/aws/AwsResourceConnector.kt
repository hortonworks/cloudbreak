package com.sequenceiq.cloudbreak.cloud.aws

import com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE
import com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED
import com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE
import com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED
import com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE
import com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED
import com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS

import java.util.ArrayList
import java.util.Arrays
import java.util.function.Supplier

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.CreateStackRequest
import com.amazonaws.services.cloudformation.model.DeleteStackRequest
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.model.OnFailure
import com.amazonaws.services.cloudformation.model.Output
import com.amazonaws.services.cloudformation.model.Parameter
import com.amazonaws.services.cloudformation.model.StackStatus
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.Address
import com.amazonaws.services.ec2.model.AssociateAddressRequest
import com.amazonaws.services.ec2.model.CreateSnapshotRequest
import com.amazonaws.services.ec2.model.CreateSnapshotResult
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.CreateVolumeRequest
import com.amazonaws.services.ec2.model.CreateVolumeResult
import com.amazonaws.services.ec2.model.DescribeAddressesRequest
import com.amazonaws.services.ec2.model.DescribeAddressesResult
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeImagesResult
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest
import com.amazonaws.services.ec2.model.DescribeSubnetsResult
import com.amazonaws.services.ec2.model.DisassociateAddressRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.ReleaseAddressRequest
import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class AwsResourceConnector : ResourceConnector {

    @Inject
    private val awsClient: AwsClient? = null
    @Inject
    private val cfStackUtil: CloudFormationStackUtil? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null
    @Inject
    private val cloudFormationTemplateBuilder: CloudFormationTemplateBuilder? = null
    @Inject
    private val awsPollTaskFactory: AwsPollTaskFactory? = null
    @Inject
    private val cloudFormationStackUtil: CloudFormationStackUtil? = null
    @Inject
    private val awsTagPreparationService: AwsTagPreparationService? = null

    @Value("${cb.aws.cf.template.new.path:}")
    private val awsCloudformationTemplatePath: String? = null

    @Throws(Exception::class)
    override fun launch(ac: AuthenticatedContext, stack: CloudStack, resourceNotifier: PersistenceNotifier,
                        adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {

        val cFStackName = cfStackUtil!!.getCfStackName(ac)
        val cloudFormationStack = CloudResource.Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build()
        resourceNotifier.notifyAllocation(cloudFormationStack, ac.cloudContext)

        val stackId = ac.cloudContext.id
        val client = awsClient!!.createCloudFormationClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        val snapshotId = getEbsSnapshotIdIfNeeded(ac, stack)
        val network = stack.network
        val awsInstanceProfileView = AwsInstanceProfileView(stack.parameters)
        val awsNetworkView = AwsNetworkView(network)
        val existingVPC = awsNetworkView.isExistingVPC
        val existingSubnet = awsNetworkView.isExistingSubnet
        val existingIGW = awsNetworkView.isExistingIGW
        val s3RoleAvailable = awsInstanceProfileView.isS3RoleAvailable
        val enableInstanceProfile = awsInstanceProfileView.isEnableInstanceProfileStrategy
        val existingSubnetCidr = if (existingSubnet) getExistingSubnetCidr(ac, stack) else null
        val amazonEC2Client = awsClient.createAccess(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        val amazonASClient = awsClient.createAutoScalingClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        var mapPublicIpOnLaunch = true
        if (existingVPC && existingSubnet) {
            val describeSubnetsRequest = DescribeSubnetsRequest()
            describeSubnetsRequest.setSubnetIds(Arrays.asList(awsNetworkView.existingSubnet))
            val describeSubnetsResult = amazonEC2Client.describeSubnets(describeSubnetsRequest)
            if (!describeSubnetsResult.subnets.isEmpty()) {
                mapPublicIpOnLaunch = describeSubnetsResult.subnets[0].isMapPublicIpOnLaunch!!
            }
        }

        val modelContext = CloudFormationTemplateBuilder.ModelContext().withAuthenticatedContext(ac).withStack(stack).withSnapshotId(snapshotId).withExistingVpc(existingVPC).withExistingIGW(existingIGW).withExistingSubnetCidr(existingSubnetCidr).mapPublicIpOnLaunch(mapPublicIpOnLaunch).withEnableInstanceProfile(enableInstanceProfile).withS3RoleAvailable(s3RoleAvailable).withTemplatePath(awsCloudformationTemplatePath)
        val cfTemplate = cloudFormationTemplateBuilder!!.build(modelContext)
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate)
        val createStackRequest = CreateStackRequest().withStackName(cFStackName).withOnFailure(OnFailure.DO_NOTHING).withTemplateBody(cfTemplate).withTags(awsTagPreparationService!!.prepareTags(ac)).withCapabilities(CAPABILITY_IAM).withParameters(
                getStackParameters(
                        ac,
                        stack.image.getUserData(InstanceGroupType.CORE),
                        stack.image.getUserData(InstanceGroupType.GATEWAY),
                        stack,
                        cFStackName))
        client.createStack(createStackRequest)
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, stackId)
        val task = awsPollTaskFactory!!.newAwsCloudformationStatusCheckerTask(ac, client,
                CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, cFStackName, true)
        try {
            val statePollerResult = task.call()
            if (!task.completed(statePollerResult)) {
                syncPollingScheduler!!.schedule(task)
            }
        } catch (e: Exception) {
            throw CloudConnectorException(e.message, e)
        }

        val cloudResources = ArrayList<CloudResource>()
        if (mapPublicIpOnLaunch) {
            val eipAllocationId = getElasticIpAllocationId(cFStackName, client)
            val instanceIds = cfStackUtil.getInstanceIds(amazonASClient, cfStackUtil.getAutoscalingGroupName(ac, client, stack.groups[0].name))
            associateElasticIpToInstance(amazonEC2Client, eipAllocationId, instanceIds)
        }
        val cloudFormationClient = awsClient.createCloudFormationClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        scheduleStatusChecks(stack, ac, cloudFormationClient)
        suspendAutoScaling(ac, stack)
        return check(ac, cloudResources)
    }

    private fun getElasticIpAllocationId(cFStackName: String, client: AmazonCloudFormationClient): String {
        val describeStacksRequest = DescribeStacksRequest().withStackName(cFStackName)
        val outputNotFound = String.format("Couldn't get Cloudformation stack's('%s') output to obtain Elastic Ip meta information.", cFStackName)
        val cfStackOutputs = client.describeStacks(describeStacksRequest).stacks.stream().findFirst().orElseThrow(getCloudConnectorExceptionSupplier(outputNotFound)).getOutputs()
        val outputKeyNotFound = String.format("Allocation Id of Elastic IP could not be found in the Cloudformation stack('%s') output.", cFStackName)
        return cfStackOutputs.stream().filter({ output -> CFS_OUTPUT_EIPALLOCATION_ID == output.getOutputKey() }).findFirst().orElseThrow(getCloudConnectorExceptionSupplier(outputKeyNotFound)).getOutputValue()
    }

    private fun associateElasticIpToInstance(amazonEC2Client: AmazonEC2Client, eipAllocationId: String, instanceIds: List<String>) {
        if (!instanceIds.isEmpty()) {
            val associateAddressRequest = AssociateAddressRequest().withAllocationId(eipAllocationId).withInstanceId(instanceIds[0])
            amazonEC2Client.associateAddress(associateAddressRequest)
        }
    }

    private fun getCloudConnectorExceptionSupplier(msg: String): Supplier<CloudConnectorException> {
        return Supplier { CloudConnectorException(msg) }
    }

    private fun suspendAutoScaling(ac: AuthenticatedContext, stack: CloudStack) {
        val amazonASClient = awsClient!!.createAutoScalingClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        for (group in stack.groups) {
            val asGroupName = cfStackUtil!!.getAutoscalingGroupName(ac, group.name, ac.cloudContext.location!!.region.value())
            amazonASClient.suspendProcesses(SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES))
        }
    }

    private fun resumeAutoScaling(ac: AuthenticatedContext, stack: CloudStack) {
        val amazonASClient = awsClient!!.createAutoScalingClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        for (group in stack.groups) {
            val asGroupName = cfStackUtil!!.getAutoscalingGroupName(ac, group.name, ac.cloudContext.location!!.region.value())
            amazonASClient.resumeProcesses(ResumeProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES))
        }
    }

    private fun getStackParameters(ac: AuthenticatedContext, coreGroupUserData: String, gateWayUserData: String, stack: CloudStack, stackName: String): List<Parameter> {
        val awsNetworkView = AwsNetworkView(stack.network)
        val awsInstanceProfileView = AwsInstanceProfileView(stack.parameters)
        var keyPairName = awsClient!!.getKeyPairName(ac)
        if (awsClient.existingKeyPairNameSpecified(ac)) {
            keyPairName = awsClient.getExistingKeyPairName(ac)
        }

        val parameters = ArrayList(Arrays.asList(
                Parameter().withParameterKey("CBUserData").withParameterValue(coreGroupUserData),
                Parameter().withParameterKey("CBGateWayUserData").withParameterValue(gateWayUserData),
                Parameter().withParameterKey("StackName").withParameterValue(stackName),
                Parameter().withParameterKey("StackOwner").withParameterValue(ac.cloudContext.owner),
                Parameter().withParameterKey("KeyName").withParameterValue(keyPairName),
                Parameter().withParameterKey("AMI").withParameterValue(stack.image.imageName),
                Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(ac, stack))))
        if (awsInstanceProfileView.isUseExistingInstanceProfile && awsInstanceProfileView.isEnableInstanceProfileStrategy) {
            parameters.add(Parameter().withParameterKey("RoleName").withParameterValue(awsInstanceProfileView.s3Role))
        }
        if (ac.cloudContext.location!!.availabilityZone.value() != null) {
            parameters.add(Parameter().withParameterKey("AvailabilitySet").withParameterValue(ac.cloudContext.location!!.availabilityZone.value()))
        }
        if (awsNetworkView.isExistingVPC) {
            parameters.add(Parameter().withParameterKey("VPCId").withParameterValue(awsNetworkView.existingVPC))
            if (awsNetworkView.isExistingIGW) {
                parameters.add(Parameter().withParameterKey("InternetGatewayId").withParameterValue(awsNetworkView.existingIGW))
            }
            if (awsNetworkView.isExistingSubnet) {
                parameters.add(Parameter().withParameterKey("SubnetId").withParameterValue(awsNetworkView.existingSubnet))
            } else {
                parameters.add(Parameter().withParameterKey("SubnetCIDR").withParameterValue(stack.network.subnet.cidr))
            }
        }
        return parameters
    }

    private fun getExistingSubnetCidr(ac: AuthenticatedContext, stack: CloudStack): String {
        val awsNetworkView = AwsNetworkView(stack.network)
        val region = ac.cloudContext.location!!.region.value()
        val ec2Client = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential), region)
        val subnetsRequest = DescribeSubnetsRequest().withSubnetIds(awsNetworkView.existingSubnet)
        val subnets = ec2Client.describeSubnets(subnetsRequest).subnets
        if (subnets.isEmpty()) {
            throw CloudConnectorException("The specified subnet does not exist (maybe it's in a different region).")
        }
        return subnets[0].cidrBlock
    }

    private fun getRootDeviceName(ac: AuthenticatedContext, cloudStack: CloudStack): String {
        val ec2Client = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        val images = ec2Client.describeImages(DescribeImagesRequest().withImageIds(cloudStack.image.imageName))
        if (images.images.isEmpty()) {
            throw CloudConnectorException(String.format("AMI is not available: '%s'.", cloudStack.image.imageName))
        }
        val image = images.images[0] ?: throw CloudConnectorException(String.format("Couldn't describe AMI '%s'.", cloudStack.image.imageName))
        return image.rootDeviceName
    }

    private fun getEbsSnapshotIdIfNeeded(ac: AuthenticatedContext, cloudStack: CloudStack): String? {
        if (isEncryptedVolumeRequested(cloudStack)) {
            val snapshot = createSnapshotIfNeeded(ac, cloudStack)
            if (snapshot.isPresent) {
                return snapshot.orNull()
            } else {
                throw CloudConnectorException(String.format("Failed to create Ebs encrypted volume on stack: %s", ac.cloudContext.id))
            }
        } else {
            return null
        }
    }

    private fun createSnapshotIfNeeded(ac: AuthenticatedContext, cloudStack: CloudStack): Optional<String> {
        val client = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential), ac.cloudContext.location!!.region.value())
        val describeSnapshotsRequest = DescribeSnapshotsRequest().withFilters(Filter().withName("tag-key").withValues(CLOUDBREAK_EBS_SNAPSHOT))
        val describeSnapshotsResult = client.describeSnapshots(describeSnapshotsRequest)
        if (describeSnapshotsResult.snapshots.isEmpty()) {
            val availabilityZonesResult = client.describeAvailabilityZones(DescribeAvailabilityZonesRequest().withFilters(Filter().withName("region-name").withValues(ac.cloudContext.location!!.region.value())))
            val volumeResult = client.createVolume(CreateVolumeRequest().withSize(SNAPSHOT_VOLUME_SIZE).withAvailabilityZone(availabilityZonesResult.availabilityZones[0].zoneName).withEncrypted(true))
            val newEbsVolumeStatusCheckerTask = awsPollTaskFactory!!.newEbsVolumeStatusCheckerTask(ac, cloudStack, client, volumeResult.volume.volumeId)
            try {
                val statePollerResult = newEbsVolumeStatusCheckerTask.call()
                if (!newEbsVolumeStatusCheckerTask.completed(statePollerResult)) {
                    syncPollingScheduler!!.schedule(newEbsVolumeStatusCheckerTask)
                }
            } catch (e: Exception) {
                throw CloudConnectorException(e.message, e)
            }

            val snapshotResult = client.createSnapshot(
                    CreateSnapshotRequest().withVolumeId(volumeResult.volume.volumeId).withDescription("Encrypted snapshot"))
            val newCreateSnapshotReadyStatusCheckerTask = awsPollTaskFactory.newCreateSnapshotReadyStatusCheckerTask(ac, snapshotResult,
                    snapshotResult.snapshot.snapshotId, client)
            try {
                val statePollerResult = newCreateSnapshotReadyStatusCheckerTask.call()
                if (!newCreateSnapshotReadyStatusCheckerTask.completed(statePollerResult)) {
                    syncPollingScheduler!!.schedule(newCreateSnapshotReadyStatusCheckerTask)
                }
            } catch (e: Exception) {
                throw CloudConnectorException(e.message, e)
            }

            val createTagsRequest = CreateTagsRequest().withTags(ImmutableList.of(Tag().withKey(CLOUDBREAK_EBS_SNAPSHOT).withValue(CLOUDBREAK_EBS_SNAPSHOT))).withResources(snapshotResult.snapshot.snapshotId)
            client.createTags(createTagsRequest)
            return Optional.of(snapshotResult.snapshot.snapshotId)
        } else {
            return Optional.of(describeSnapshotsResult.snapshots[0].snapshotId)
        }
    }

    private fun isEncryptedVolumeRequested(stack: CloudStack): Boolean {
        for (group in stack.groups) {
            for (cloudInstance in group.instances) {
                val instanceTemplate = cloudInstance.template
                var encrypted: Boolean? = instanceTemplate.getParameter<Boolean>("encrypted", Boolean::class.java)
                encrypted = if (encrypted == null) java.lang.Boolean.FALSE else encrypted
                if (encrypted == java.lang.Boolean.TRUE) {
                    return true
                }
            }
        }
        return false
    }

    override fun check(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    override fun terminate(ac: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        LOGGER.info("Deleting stack: {}", ac.cloudContext.id)
        if (resources != null && !resources.isEmpty()) {
            val client = awsClient!!.createCloudFormationClient(AwsCredentialView(ac.cloudCredential),
                    ac.cloudContext.location!!.region.value())
            val cFStackName = getCloudFormationStackResource(resources)!!.name
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", ac.cloudContext.id, cFStackName)
            val describeStacksRequest = DescribeStacksRequest().withStackName(cFStackName)
            try {
                client.describeStacks(describeStacksRequest)
            } catch (e: AmazonServiceException) {
                if (e.errorMessage.contains(cFStackName + " does not exist")) {
                    val amazonEC2Client = awsClient.createAccess(AwsCredentialView(ac.cloudCredential),
                            ac.cloudContext.location!!.region.value())
                    releaseReservedIp(amazonEC2Client, resources)
                    return Arrays.asList<CloudResourceStatus>()
                } else {
                    throw e
                }
            }

            resumeAutoScalingPolicies(ac, stack)
            val deleteStackRequest = DeleteStackRequest().withStackName(cFStackName)
            client.deleteStack(deleteStackRequest)
            val task = awsPollTaskFactory!!.newAwsCloudformationStatusCheckerTask(ac, client,
                    DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, cFStackName, false)
            try {
                val statePollerResult = task.call()
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler!!.schedule(task)
                }
            } catch (e: Exception) {
                throw CloudConnectorException(e.message, e)
            }

            val amazonEC2Client = awsClient.createAccess(AwsCredentialView(ac.cloudCredential),
                    ac.cloudContext.location!!.region.value())
            releaseReservedIp(amazonEC2Client, resources)
        } else {
            val amazonEC2Client = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential),
                    ac.cloudContext.location!!.region.value())
            releaseReservedIp(amazonEC2Client, resources)
            LOGGER.info("No CloudFormation stack saved for stack.")
        }
        return check(ac, resources)
    }

    private fun resumeAutoScalingPolicies(ac: AuthenticatedContext, stack: CloudStack) {
        for (instanceGroup in stack.groups) {
            try {
                val asGroupName = cfStackUtil!!.getAutoscalingGroupName(ac, instanceGroup.name, ac.cloudContext.location!!.region.value())
                if (asGroupName != null) {
                    val amazonASClient = awsClient!!.createAutoScalingClient(AwsCredentialView(ac.cloudCredential),
                            ac.cloudContext.location!!.region.value())
                    val asGroups = amazonASClient.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asGroupName)).autoScalingGroups
                    if (!asGroups.isEmpty()) {
                        if (!asGroups[0].suspendedProcesses.isEmpty()) {
                            amazonASClient.updateAutoScalingGroup(UpdateAutoScalingGroupRequest().withAutoScalingGroupName(asGroupName).withMinSize(0).withDesiredCapacity(0))
                            amazonASClient.resumeProcesses(ResumeProcessesRequest().withAutoScalingGroupName(asGroupName))
                        }
                    }
                } else {
                    LOGGER.info("Autoscaling Group's physical id is null (the resource doesn't exist), it is not needed to resume scaling policies.")
                }
            } catch (e: AmazonServiceException) {
                if (e.errorMessage.matches("Resource.*does not exist for stack.*".toRegex()) || e.errorMessage.matches("Stack '.*' does not exist.*".toRegex())) {
                    LOGGER.info(e.message)
                } else {
                    throw e
                }
            }

        }
    }

    private fun releaseReservedIp(client: AmazonEC2Client, resources: List<CloudResource>) {
        val elasticIpResource = getReservedIp(resources)
        if (elasticIpResource != null && elasticIpResource.name != null) {
            val address: Address
            try {
                val describeResult = client.describeAddresses(
                        DescribeAddressesRequest().withAllocationIds(elasticIpResource.name))
                address = describeResult.addresses[0]
            } catch (e: AmazonServiceException) {
                if (e.errorMessage == "The allocation ID '" + elasticIpResource.name + "' does not exist") {
                    LOGGER.warn("Elastic IP with allocation ID '{}' not found. Ignoring IP release.")
                    return
                } else {
                    throw e
                }
            }

            if (address.associationId != null) {
                client.disassociateAddress(DisassociateAddressRequest().withAssociationId(elasticIpResource.name))
            }
            client.releaseAddress(ReleaseAddressRequest().withAllocationId(elasticIpResource.name))
        }
    }

    override fun update(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    override fun upscale(ac: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        resumeAutoScaling(ac, stack)

        val amazonASClient = awsClient!!.createAutoScalingClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())
        val cloudFormationClient = awsClient.createCloudFormationClient(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())

        for (group in stack.groups) {
            val asGroupName = cfStackUtil!!.getAutoscalingGroupName(ac, cloudFormationClient, group.name)

            amazonASClient.updateAutoScalingGroup(UpdateAutoScalingGroupRequest().withAutoScalingGroupName(asGroupName).withMaxSize(group.instances.size).withDesiredCapacity(group.instances.size))
            LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', to: '{}']", ac.cloudContext.id,
                    resources.size)
        }
        scheduleStatusChecks(stack, ac, cloudFormationClient)
        suspendAutoScaling(ac, stack)

        return Arrays.asList(CloudResourceStatus(getCloudFormationStackResource(resources), ResourceStatus.UPDATED))
    }

    override fun downscale(auth: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudResourceStatus> {
        val amazonASClient = awsClient!!.createAutoScalingClient(AwsCredentialView(auth.cloudCredential),
                auth.cloudContext.location!!.region.value())
        val amazonEC2Client = awsClient.createAccess(AwsCredentialView(auth.cloudCredential),
                auth.cloudContext.location!!.region.value())

        val asGroupName = cfStackUtil!!.getAutoscalingGroupName(auth, vms[0].template!!.groupName,
                auth.cloudContext.location!!.region.value())
        val instanceIds = ArrayList<String>()
        for (vm in vms) {
            instanceIds.add(vm.instanceId)
        }
        val detachInstancesRequest = DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds).withShouldDecrementDesiredCapacity(true)
        amazonASClient.detachInstances(detachInstancesRequest)
        amazonEC2Client.terminateInstances(TerminateInstancesRequest().withInstanceIds(instanceIds))
        LOGGER.info("Terminated instances in stack '{}': '{}'", auth.cloudContext.id, instanceIds)
        return check(auth, resources)
    }

    private fun scheduleStatusChecks(stack: CloudStack, ac: AuthenticatedContext, cloudFormationClient: AmazonCloudFormationClient) {

        for (group in stack.groups) {
            val asGroupName = cfStackUtil!!.getAutoscalingGroupName(ac, cloudFormationClient, group.name)
            LOGGER.info("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", ac.cloudContext.id,
                    asGroupName)
            val task = awsPollTaskFactory!!.newASGroupStatusCheckerTask(ac, asGroupName, group.instances.size, awsClient, cfStackUtil)
            try {
                val statePollerResult = task.call()
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler!!.schedule(task)
                }
            } catch (e: Exception) {
                throw CloudConnectorException(e.message, e)
            }

        }
    }

    private fun getCloudFormationStackResource(cloudResources: List<CloudResource>): CloudResource? {
        for (cloudResource in cloudResources) {
            if (cloudResource.type == ResourceType.CLOUDFORMATION_STACK) {
                return cloudResource
            }
        }
        return null
    }

    private fun getReservedIp(cloudResources: List<CloudResource>): CloudResource? {
        for (cloudResource in cloudResources) {
            if (cloudResource.type == ResourceType.AWS_RESERVED_IP) {
                return cloudResource
            }
        }
        return null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AwsResourceConnector::class.java)
        private val CLOUDBREAK_EBS_SNAPSHOT = "cloudbreak-ebs-snapshot"
        private val SNAPSHOT_VOLUME_SIZE = 10
        private val CAPABILITY_IAM = Arrays.asList("CAPABILITY_IAM")

        private val SUSPENDED_PROCESSES = Arrays.asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
                "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority")
        private val ERROR_STATUSES = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE)
        private val CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID"
    }

}
