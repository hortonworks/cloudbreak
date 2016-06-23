package com.sequenceiq.cloudbreak.cloud.aws

import java.util.ArrayList
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest
import com.amazonaws.services.ec2.model.GetConsoleOutputResult
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.StartInstancesRequest
import com.amazonaws.services.ec2.model.StopInstancesRequest
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

@Service
class AwsInstanceConnector : InstanceConnector {

    @Inject
    private val awsClient: AwsClient? = null

    @Value("${cb.aws.hostkey.verify:}")
    private val verifyHostKey: Boolean = false

    override fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String {
        if (!verifyHostKey) {
            throw CloudOperationNotSupportedException("Host key verification is disabled on AWS")
        }
        val amazonEC2Client = awsClient!!.createAccess(AwsCredentialView(authenticatedContext.cloudCredential),
                authenticatedContext.cloudContext.location!!.region.value())
        val getConsoleOutputRequest = GetConsoleOutputRequest().withInstanceId(vm.instanceId)
        val getConsoleOutputResult = amazonEC2Client.getConsoleOutput(getConsoleOutputRequest)
        try {
            if (getConsoleOutputResult.output == null) {
                return ""
            } else {
                return getConsoleOutputResult.decodedOutput
            }
        } catch (ex: Exception) {
            LOGGER.debug(ex.message, ex)
            return ""
        }

    }

    override fun start(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val amazonEC2Client = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())

        for (group in getGroups(vms)) {
            var instances: MutableCollection<String> = ArrayList()
            val cloudInstances = ArrayList<CloudInstance>()

            for (vm in vms) {
                if (vm.template!!.groupName == group) {
                    instances.add(vm.instanceId)
                    cloudInstances.add(vm)
                }
            }
            try {
                instances = removeInstanceIdsWhichAreNotInCorrectState(instances, amazonEC2Client, "Running")
                if (instances.size > 0) {
                    amazonEC2Client.startInstances(StartInstancesRequest().withInstanceIds(instances))
                }
                for (cloudInstance in cloudInstances) {
                    statuses.add(CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS))
                }
            } catch (e: Exception) {
                for (cloudInstance in cloudInstances) {
                    statuses.add(CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, e.message))
                }
            }

        }
        return statuses
    }


    override fun stop(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val amazonEC2Client = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential),
                ac.cloudContext.location!!.region.value())

        for (group in getGroups(vms)) {
            var instances: MutableCollection<String> = ArrayList()
            val cloudInstances = ArrayList<CloudInstance>()

            for (vm in vms) {
                if (vm.template!!.groupName == group) {
                    instances.add(vm.instanceId)
                    cloudInstances.add(vm)
                }
            }
            try {
                instances = removeInstanceIdsWhichAreNotInCorrectState(instances, amazonEC2Client, "Stopped")
                if (instances.size > 0) {
                    amazonEC2Client.stopInstances(StopInstancesRequest().withInstanceIds(instances))
                }
                for (cloudInstance in cloudInstances) {
                    statuses.add(CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS))
                }
            } catch (e: Exception) {
                for (cloudInstance in cloudInstances) {
                    statuses.add(CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, e.message))
                }
            }

        }
        return statuses
    }

    override fun check(ac: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val cloudVmInstanceStatuses = ArrayList<CloudVmInstanceStatus>()
        for (vm in vms) {
            val result = awsClient!!.createAccess(AwsCredentialView(ac.cloudCredential),
                    ac.cloudContext.location!!.region.value()).describeInstances(DescribeInstancesRequest().withInstanceIds(vm.instanceId))
            for (reservation in result.reservations) {
                for (instance in reservation.instances) {
                    if ("Stopped".equals(instance.state.name, ignoreCase = true)) {
                        LOGGER.info("AWS instance is in Stopped state, polling stack.")
                        cloudVmInstanceStatuses.add(CloudVmInstanceStatus(vm, InstanceStatus.STOPPED))
                    } else if ("Running".equals(instance.state.name, ignoreCase = true)) {
                        LOGGER.info("AWS instance is in Started state, polling stack.")
                        cloudVmInstanceStatuses.add(CloudVmInstanceStatus(vm, InstanceStatus.STARTED))
                    } else if ("Terminated".equals(instance.state.name, ignoreCase = true)) {
                        LOGGER.info("AWS instance is in Terminated state, polling stack.")
                        cloudVmInstanceStatuses.add(CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED))
                    } else {
                        cloudVmInstanceStatuses.add(CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS))
                    }
                }
            }
        }
        return cloudVmInstanceStatuses
    }

    private fun removeInstanceIdsWhichAreNotInCorrectState(instances: MutableCollection<String>, amazonEC2Client: AmazonEC2Client, state: String): MutableCollection<String> {
        val describeInstances = amazonEC2Client.describeInstances(
                DescribeInstancesRequest().withInstanceIds(instances))
        for (reservation in describeInstances.reservations) {
            for (instance in reservation.instances) {
                if (state.equals(instance.state.name, ignoreCase = true)) {
                    instances.remove(instance.instanceId)
                }
            }
        }
        return instances
    }

    private fun getGroups(vms: List<CloudInstance>): Set<String> {
        val groups = HashSet<String>()
        for (vm in vms) {
            if (!groups.contains(vm.template!!.groupName)) {
                groups.add(vm.template!!.groupName)
            }
        }
        return groups
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AwsInstanceConnector::class.java)
    }

}
