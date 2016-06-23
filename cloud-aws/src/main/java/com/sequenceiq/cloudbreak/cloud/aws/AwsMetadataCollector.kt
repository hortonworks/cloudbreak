package com.sequenceiq.cloudbreak.cloud.aws

import java.util.ArrayList
import java.util.Arrays
import java.util.Queue

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.Tag
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler

@Service
class AwsMetadataCollector : MetadataCollector {

    @Inject
    private val awsClient: AwsClient? = null
    @Inject
    private val cloudFormationStackUtil: CloudFormationStackUtil? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null
    @Inject
    private val awsPollTaskFactory: AwsPollTaskFactory? = null

    override fun collect(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus> {
        val cloudVmMetaDataStatuses = ArrayList<CloudVmMetaDataStatus>()
        try {
            val region = ac.cloudContext.location!!.region.value()
            val amazonCFClient = awsClient!!.createCloudFormationClient(AwsCredentialView(ac.cloudCredential), region)
            val amazonASClient = awsClient.createAutoScalingClient(AwsCredentialView(ac.cloudCredential), region)
            val amazonEC2Client = awsClient.createAccess(AwsCredentialView(ac.cloudCredential), region)

            //contains all instances
            val groupByInstanceGroup = groupByInstanceGroup(vms)

            for (key in groupByInstanceGroup.keySet()) {
                val cloudInstances = groupByInstanceGroup.get(key)
                cloudVmMetaDataStatuses.addAll(collectGroupMetaData(ac, amazonASClient, amazonEC2Client, amazonCFClient, key, cloudInstances))
            }

            return cloudVmMetaDataStatuses
        } catch (e: Exception) {
            throw CloudConnectorException(e.message, e)
        }

    }

    private fun collectGroupMetaData(ac: AuthenticatedContext, amazonASClient: AmazonAutoScalingClient,
                                     amazonEC2Client: AmazonEC2Client, amazonCFClient: AmazonCloudFormationClient, groupName: String, cloudInstances: List<CloudInstance>): List<CloudVmMetaDataStatus> {

        val cloudVmMetaDataStatuses = ArrayList<CloudVmMetaDataStatus>()

        val asGroupName = cloudFormationStackUtil!!.getAutoscalingGroupName(ac, amazonCFClient, groupName)
        val instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, asGroupName)

        val instancesRequest = cloudFormationStackUtil.createDescribeInstancesRequest(instanceIds)
        val instancesResult = amazonEC2Client.describeInstances(instancesRequest)

        //contains instances with instanceId
        val mapByInstanceId = mapByInstanceId(cloudInstances)

        //contains instances with privateId (without instanceId)
        val untrackedInstances = untrackedInstances(cloudInstances)

        for (reservation in instancesResult.reservations) {
            LOGGER.info("Number of instances found in reservation: {}", reservation.instances.size)
            for (instance in reservation.instances) {

                val instanceId = instance.instanceId
                val cloudInstance = ensureInstanceTag(mapByInstanceId, instance, instanceId, untrackedInstances, amazonEC2Client)
                if (cloudInstance != null) {
                    val md = CloudInstanceMetaData(instance.privateIpAddress, instance.publicIpAddress)
                    val cloudVmInstanceStatus = CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED)
                    val cloudVmMetaDataStatus = CloudVmMetaDataStatus(cloudVmInstanceStatus, md)
                    cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus)
                }
            }
        }

        return cloudVmMetaDataStatuses
    }

    private fun ensureInstanceTag(mapByInstanceId: Map<String, CloudInstance>, instance: Instance, instanceId: String, untrackedInstances: Queue<CloudInstance>, amazonEC2Client: AmazonEC2Client): CloudInstance? {

        // we need to figure out whether it is already tracked or not, if it is already tracked then it has a tag
        val tag = getTag(instance)

        var cloudInstance: CloudInstance? = mapByInstanceId[instanceId]
        if (cloudInstance == null) {
            if (tag == null) {
                // so it is not tracked at the moment, therefore it considered as a new instance, and we shall track it by tagging it, with the private id of
                // an untracked CloudInstance
                cloudInstance = untrackedInstances.remove()
                cloudInstance = CloudInstance(instanceId, cloudInstance!!.template)
            }
        }

        if (cloudInstance != null && tag == null) {
            addTag(amazonEC2Client, cloudInstance, instance)
        }

        return cloudInstance
    }

    private fun getTag(instance: Instance): String? {
        for (tag in instance.tags) {
            if (TAG_NAME == tag.key) {
                val value = tag.value
                LOGGER.info("Instance: {} was already tagged: {}", instance.instanceId, value)
                return value
            }
        }
        return null
    }


    private fun addTag(amazonEC2Client: AmazonEC2Client, cloudInstance: CloudInstance, instance: Instance) {
        val tagName = awsClient!!.getCbName(cloudInstance.template!!.groupName, cloudInstance.template!!.privateId)
        val t = Tag()
        t.key = TAG_NAME
        t.value = tagName
        val ctr = CreateTagsRequest()
        ctr.setTags(Arrays.asList(t))
        ctr.withResources(instance.instanceId)
        amazonEC2Client.createTags(ctr)
    }


    private fun groupByInstanceGroup(vms: List<CloudInstance>): ListMultimap<String, CloudInstance> {
        val groupByInstanceGroup = ArrayListMultimap.create<String, CloudInstance>()
        for (vm in vms) {
            val groupName = vm.template!!.groupName
            groupByInstanceGroup.put(groupName, vm)
        }
        return groupByInstanceGroup
    }


    private fun mapByInstanceId(vms: List<CloudInstance>): Map<String, CloudInstance> {
        val groupByInstanceId = Maps.newHashMap<String, CloudInstance>()
        for (vm in vms) {
            val instanceId = vm.instanceId
            if (instanceId != null) {
                groupByInstanceId.put(instanceId, vm)
            }
        }
        return groupByInstanceId
    }


    private fun untrackedInstances(vms: List<CloudInstance>): Queue<CloudInstance> {
        val cloudInstances = Lists.newLinkedList<CloudInstance>()
        for (vm in vms) {
            if (vm.instanceId == null) {
                cloudInstances.add(vm)
            }
        }
        return cloudInstances
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AwsMetadataCollector::class.java)
        private val TAG_NAME = "cbname"
    }

}
