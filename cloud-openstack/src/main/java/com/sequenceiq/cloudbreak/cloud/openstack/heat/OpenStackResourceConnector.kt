package com.sequenceiq.cloudbreak.cloud.openstack.heat

import java.util.ArrayList

import javax.inject.Inject

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.model.heat.Stack
import org.openstack4j.model.heat.StackUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackResourceConnector : ResourceConnector {

    @Inject
    private val openStackClient: OpenStackClient? = null
    @Inject
    private val heatTemplateBuilder: HeatTemplateBuilder? = null
    @Inject
    private val utils: OpenStackUtils? = null

    @SuppressWarnings("unchecked")
    override fun launch(authenticatedContext: AuthenticatedContext, stack: CloudStack, notifier: PersistenceNotifier,
                        adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {
        val stackName = authenticatedContext.cloudContext.name
        val existingNetwork = isExistingNetwork(stack)
        val assignFloatingIp = assignFloatingIp(stack)
        val existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack)
        val heatTemplate = heatTemplateBuilder!!.build(
                stackName, stack.groups, stack.security, stack.image, existingNetwork, existingSubnetCidr != null, assignFloatingIp)
        val parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack.network, stack.image, existingNetwork, existingSubnetCidr)

        val client = openStackClient!!.createOSClient(authenticatedContext)

        val heatStack = client.heat().stacks().create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false).parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build())


        val cloudResource = CloudResource.Builder().type(ResourceType.HEAT_STACK).name(heatStack.id).build()
        try {
            notifier.notifyAllocation(cloudResource, authenticatedContext.cloudContext)
        } catch (e: Exception) {
            //Rollback
            terminate(authenticatedContext, stack, Lists.newArrayList(cloudResource))
        }

        val resources = check(authenticatedContext, Lists.newArrayList(cloudResource))
        LOGGER.debug("Launched resources: {}", resources)
        return resources
    }


    override fun check(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        val result = ArrayList<CloudResourceStatus>()
        val client = openStackClient!!.createOSClient(authenticatedContext)

        for (resource in resources) {
            when (resource.type) {
                ResourceType.HEAT_STACK -> {
                    val heatStackId = resource.name
                    val stackName = authenticatedContext.cloudContext.name
                    LOGGER.info("Checking OpenStack Heat stack status of: {}", stackName)
                    val heatStack = client.heat().stacks().getDetails(stackName, heatStackId)
                    val heatResourceStatus = utils!!.heatStatus(resource, heatStack)
                    result.add(heatResourceStatus)
                }
                else -> throw CloudConnectorException(String.format("Invalid resource type: %s", resource.type))
            }
        }

        return result
    }

    override fun terminate(authenticatedContext: AuthenticatedContext, cloudStack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {

        for (resource in resources) {
            when (resource.type) {
                ResourceType.HEAT_STACK -> {
                    val heatStackId = resource.name
                    val stackName = authenticatedContext.cloudContext.name
                    LOGGER.info("Terminate stack: {}", stackName)
                    val client = openStackClient!!.createOSClient(authenticatedContext)
                    client.heat().stacks().delete(stackName, heatStackId)
                }
                else -> throw CloudConnectorException(String.format("Invalid resource type: %s", resource.type))
            }
        }

        return check(authenticatedContext, resources)
    }

    override fun upscale(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        val stackName = authenticatedContext.cloudContext.name
        val existingNetwork = isExistingNetwork(stack)
        val assignFloatingIp = assignFloatingIp(stack)
        val existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack)
        val heatTemplate = heatTemplateBuilder!!.build(
                stackName, stack.groups, stack.security, stack.image, existingNetwork, existingSubnetCidr != null, assignFloatingIp)
        val parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack.network, stack.image, existingNetwork, existingSubnetCidr)
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters)
    }

    override fun downscale(auth: AuthenticatedContext, cloudStack: CloudStack, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudResourceStatus> {
        val stack = removeDeleteRequestedInstances(cloudStack)
        val stackName = auth.cloudContext.name
        val existingNetwork = isExistingNetwork(stack)
        val assignFloatingIp = assignFloatingIp(stack)
        val existingSubnetCidr = getExistingSubnetCidr(auth, stack)
        val heatTemplate = heatTemplateBuilder!!.build(
                stackName, stack.groups, stack.security, stack.image, existingNetwork, existingSubnetCidr != null, assignFloatingIp)
        val parameters = heatTemplateBuilder.buildParameters(
                auth, stack.network, stack.image, existingNetwork, existingSubnetCidr)
        return updateHeatStack(auth, resources, heatTemplate, parameters)
    }

    override fun update(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        val stackName = authenticatedContext.cloudContext.name
        val existingNetwork = isExistingNetwork(stack)
        val assignFloatingIp = assignFloatingIp(stack)
        val existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack)
        val heatTemplate = heatTemplateBuilder!!.build(
                stackName, stack.groups, stack.security, stack.image, existingNetwork, existingSubnetCidr != null, assignFloatingIp)
        val parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack.network, stack.image, existingNetwork, existingSubnetCidr)
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters)
    }

    private fun updateHeatStack(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, heatTemplate: String,
                                parameters: Map<String, String>): List<CloudResourceStatus> {
        val resource = utils!!.getHeatResource(resources)
        val stackName = authenticatedContext.cloudContext.name
        val heatStackId = resource.name

        val client = openStackClient!!.createOSClient(authenticatedContext)
        val updateRequest = Builders.stackUpdate().template(heatTemplate).parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build()
        client.heat().stacks().update(stackName, heatStackId, updateRequest)
        LOGGER.info("Heat stack update request sent with stack name: '{}' for Heat stack: '{}'", stackName, heatStackId)
        return check(authenticatedContext, resources)
    }

    private fun removeDeleteRequestedInstances(stack: CloudStack): CloudStack {
        val groups = ArrayList<Group>()
        for (group in stack.groups) {
            val instances = ArrayList(group.instances)
            for (instance in group.instances) {
                if (InstanceStatus.DELETE_REQUESTED === instance.template!!.status) {
                    instances.remove(instance)
                }
            }
            groups.add(Group(group.name, group.type, instances))
        }
        return CloudStack(groups, stack.network, stack.security, stack.image, stack.parameters)
    }

    private fun isExistingNetwork(stack: CloudStack): Boolean {
        return utils!!.isExistingNetwork(stack.network)
    }

    private fun assignFloatingIp(stack: CloudStack): Boolean {
        return utils!!.assignFloatingIp(stack.network)
    }

    private fun getExistingSubnetCidr(authenticatedContext: AuthenticatedContext, stack: CloudStack): String? {
        val network = stack.network
        return if (utils!!.isExistingSubnet(network)) utils.getExistingSubnetCidr(authenticatedContext, network) else null
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenStackResourceConnector::class.java)
        private val OPERATION_TIMEOUT = 60L
    }

}
