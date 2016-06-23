package com.sequenceiq.cloudbreak.cloud.arm

import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND

import java.util.ArrayList
import java.util.Arrays

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.arm.context.NetworkInterfaceCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.context.ResourceGroupCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStackView
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStorageView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.common.type.ResourceType

import groovyx.net.http.HttpResponseException

@Service
class ArmResourceConnector : ResourceConnector {

    @Inject
    private val armClient: ArmClient? = null
    @Inject
    private val armTemplateBuilder: ArmTemplateBuilder? = null
    @Inject
    private val armUtils: ArmUtils? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null
    @Inject
    private val armPollTaskFactory: ArmPollTaskFactory? = null
    @Inject
    private val armStorage: ArmStorage? = null

    override fun launch(ac: AuthenticatedContext, stack: CloudStack, notifier: PersistenceNotifier,
                        adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {
        val armCredentialView = ArmCredentialView(ac.cloudCredential)
        val stackName = armUtils!!.getStackName(ac.cloudContext)
        val resourceGroupName = armUtils.getResourceGroupName(ac.cloudContext)
        val armStackView = getArmStack(armCredentialView, ac.cloudContext, stack)
        armUtils.validateStorageType(stack)
        val template = armTemplateBuilder!!.build(stackName, armCredentialView, armStackView, ac.cloudContext, stack)
        val parameters = armTemplateBuilder.buildParameters(ac.cloudCredential, stack.network, stack.image)
        val client = armClient!!.getClient(ac.cloudCredential)
        armUtils.validateSubnetRules(client, stack.network)
        try {
            val region = ac.cloudContext.location!!.region.value()
            val storageAccounts = armStackView.storageAccounts
            for (name in storageAccounts.keys) {
                armStorage!!.createStorage(ac, client, name, storageAccounts[name], resourceGroupName, region)
            }
            client.createTemplateDeployment(resourceGroupName, stackName, template, parameters)
        } catch (e: HttpResponseException) {
            throw CloudConnectorException(String.format("Error occurred when creating stack: %s", e.response.data.toString()))
        } catch (e: Exception) {
            throw CloudConnectorException(String.format("Invalid provisioning type: %s", stackName))
        }

        val cloudResource = CloudResource.Builder().type(ResourceType.ARM_TEMPLATE).name(stackName).build()
        val resources = check(ac, Arrays.asList(cloudResource))
        LOGGER.debug("Launched resources: {}", resources)
        return resources
    }

    override fun check(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        val result = ArrayList<CloudResourceStatus>()
        val access = armClient!!.getClient(authenticatedContext.cloudCredential)
        val stackName = armUtils!!.getStackName(authenticatedContext.cloudContext)

        for (resource in resources) {
            when (resource.type) {
                ResourceType.ARM_TEMPLATE -> {
                    LOGGER.info("Checking Arm group stack status of: {}", stackName)
                    try {
                        val resourceGroup = access.getTemplateDeployment(stackName, stackName)
                        val templateResourceStatus = armUtils.templateStatus(resource, resourceGroup, access, stackName)
                        result.add(templateResourceStatus)
                    } catch (e: HttpResponseException) {
                        if (e.statusCode == NOT_FOUND) {
                            result.add(CloudResourceStatus(resource, ResourceStatus.DELETED))
                        } else {
                            throw CloudConnectorException(e.response.data.toString(), e)
                        }
                    } catch (e: Exception) {
                        throw CloudConnectorException(String.format("Invalid resource exception: %s", e.message), e)
                    }

                }
                else -> throw CloudConnectorException(String.format("Invalid resource type: %s", resource.type))
            }
        }

        return result
    }

    override fun terminate(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        val azureRMClient = armClient!!.getClient(authenticatedContext.cloudCredential)
        for (resource in resources) {
            try {
                azureRMClient.deleteResourceGroup(resource.name)
                val task = armPollTaskFactory!!.newResourceGroupDeleteStatusCheckerTask(authenticatedContext, armClient,
                        ResourceGroupCheckerContext(ArmCredentialView(authenticatedContext.cloudCredential), resource.name))
                val statePollerResult = task.call()
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler!!.schedule(task)
                }
                if (armStorage!!.isPersistentStorage(armStorage.getPersistentStorageName(stack.parameters))) {
                    val cloudCtx = authenticatedContext.cloudContext
                    val imageStorageName = armStorage.getImageStorageName(ArmCredentialView(authenticatedContext.cloudCredential), cloudCtx,
                            armStorage.getPersistentStorageName(stack.parameters), armStorage.getArmAttachedStorageOption(stack.parameters))
                    val imageResourceGroupName = armStorage.getImageResourceGroupName(cloudCtx, stack.parameters)
                    val diskContainer = armStorage.getDiskContainerName(cloudCtx)
                    deleteContainer(azureRMClient, imageResourceGroupName, imageStorageName, diskContainer)
                }
            } catch (e: HttpResponseException) {
                if (e.statusCode != NOT_FOUND) {
                    throw CloudConnectorException(e.response.data.toString(), e)
                } else {
                    return check(authenticatedContext, ArrayList<CloudResource>())
                }
            } catch (e: Exception) {
                throw CloudConnectorException(String.format("Could not delete resource group: %s", resource.name), e)
            }

        }
        return check(authenticatedContext, resources)
    }

    override fun update(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    override fun upscale(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        val azureRMClient = armClient!!.getClient(authenticatedContext.cloudCredential)
        val armCredentialView = ArmCredentialView(authenticatedContext.cloudCredential)

        val stackName = armUtils!!.getStackName(authenticatedContext.cloudContext)
        val armStackView = getArmStack(armCredentialView, authenticatedContext.cloudContext, stack)
        val template = armTemplateBuilder!!.build(stackName, armCredentialView, armStackView, authenticatedContext.cloudContext, stack)
        val parameters = armTemplateBuilder.buildParameters(authenticatedContext.cloudCredential, stack.network, stack.image)
        val resourceGroupName = armUtils.getResourceGroupName(authenticatedContext.cloudContext)

        try {
            val region = authenticatedContext.cloudContext.location!!.region.value()
            val storageAccounts = armStackView.storageAccounts
            for (name in storageAccounts.keys) {
                armStorage!!.createStorage(authenticatedContext, azureRMClient, name, storageAccounts[name], resourceGroupName, region)
            }
            azureRMClient.createTemplateDeployment(stackName, stackName, template, parameters)
            val check = ArrayList<CloudResourceStatus>()
            check.add(CloudResourceStatus(resources[0], ResourceStatus.IN_PROGRESS))
            return check
        } catch (e: HttpResponseException) {
            throw CloudConnectorException(e.response.data.toString(), e)
        } catch (e: Exception) {
            throw CloudConnectorException(String.format("Could not upscale: %s", stackName), e)
        }

    }

    override fun downscale(ac: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudResourceStatus> {
        val client = armClient!!.getClient(ac.cloudCredential)
        val armCredentialView = ArmCredentialView(ac.cloudCredential)
        val stackName = armUtils!!.getStackName(ac.cloudContext)

        val resourceGroupName = armUtils.getResourceGroupName(ac.cloudContext)
        val diskContainer = armStorage!!.getDiskContainerName(ac.cloudContext)

        for (instance in vms) {
            val networkInterfacesNames = ArrayList<String>()
            val storageProfileDiskNames = ArrayList<String>()
            val instanceId = instance.instanceId
            val privateId = instance.template!!.privateId
            val armDiskType = ArmDiskType.getByValue(instance.template!!.volumeType)
            val attachedDiskStorageName = armStorage.getAttachedDiskStorageName(armStorage.getArmAttachedStorageOption(stack.parameters),
                    armCredentialView, privateId, ac.cloudContext, armDiskType)
            try {
                val virtualMachine = client.getVirtualMachine(stackName, instanceId)

                val properties = virtualMachine["properties"] as Map<Any, Any>

                val networkProfile = properties["networkProfile"] as Map<Any, Any>

                val networkInterfaces = networkProfile["networkInterfaces"] as List<Map<Any, Any>>
                for (networkInterface in networkInterfaces) {
                    networkInterfacesNames.add(getNameFromConnectionString(networkInterface["id"].toString()))
                }

                val storageProfile = properties["storageProfile"] as Map<Any, Any>

                val osDisk = storageProfile["osDisk"] as Map<Any, Any>
                val dataDisks = storageProfile["dataDisks"] as List<Map<Any, Any>>

                for (datadisk in dataDisks) {
                    val vhds = datadisk["vhd"] as Map<Any, Any>
                    storageProfileDiskNames.add(getNameFromConnectionString(vhds["uri"].toString()))
                }
                val vhds = osDisk["vhd"] as Map<Any, Any>
                storageProfileDiskNames.add(getNameFromConnectionString(vhds["uri"].toString()))
            } catch (e: HttpResponseException) {
                if (e.statusCode != NOT_FOUND) {
                    throw CloudConnectorException(e.response.data.toString(), e)
                }
            } catch (e: Exception) {
                throw CloudConnectorException(String.format("Could not downscale: %s", stackName), e)
            }

            try {
                deallocateVirtualMachine(ac, client, stackName, instanceId)
                deleteVirtualMachine(ac, client, stackName, instanceId)
                deleteNetworkInterfaces(ac, client, stackName, networkInterfacesNames)
                deleteDisk(storageProfileDiskNames, client, resourceGroupName, attachedDiskStorageName, diskContainer)
                if (armStorage.getArmAttachedStorageOption(stack.parameters) == ArmAttachedStorageOption.PER_VM) {
                    armStorage.deleteStorage(ac, client, attachedDiskStorageName, resourceGroupName)
                }
            } catch (e: CloudConnectorException) {
                throw e
            } catch (e: Exception) {
                throw CloudConnectorException(String.format("Failed to cleanup resources after downscale: %s", stackName), e)
            }

        }
        return check(ac, resources)
    }

    private fun getArmStack(armCredentialView: ArmCredentialView, cloudContext: CloudContext, cloudStack: CloudStack): ArmStackView {
        return ArmStackView(cloudStack.groups, ArmStorageView(armCredentialView, cloudContext, armStorage,
                armStorage!!.getArmAttachedStorageOption(cloudStack.parameters)))
    }

    private fun deleteContainer(azureRMClient: AzureRMClient, resourceGroup: String, storageName: String, container: String) {
        try {
            azureRMClient.deleteContainerInStorage(resourceGroup, storageName, container)
        } catch (e: HttpResponseException) {
            if (e.statusCode != NOT_FOUND) {
                throw CloudConnectorException(e.response.data.toString())
            }
        } catch (e: Exception) {
            throw CloudConnectorException(String.format("Could not delete container: %s", container), e)
        }

    }

    private fun deleteDisk(storageProfileDiskNames: List<String>, azureRMClient: AzureRMClient, resourceGroup: String, storageName: String, container: String) {
        for (storageProfileDiskName in storageProfileDiskNames) {
            try {
                azureRMClient.deleteBlobInStorageContainer(resourceGroup, storageName, container, storageProfileDiskName)
            } catch (e: HttpResponseException) {
                if (e.statusCode != NOT_FOUND) {
                    throw CloudConnectorException(e.response.data.toString())
                }
            } catch (e: Exception) {
                throw CloudConnectorException(String.format("Could not delete blob: %s", storageProfileDiskName), e)
            }

        }
    }

    @Throws(CloudConnectorException::class)
    private fun deleteNetworkInterfaces(authenticatedContext: AuthenticatedContext, client: AzureRMClient, stackName: String, networkInterfacesNames: List<String>) {
        for (networkInterfacesName in networkInterfacesNames) {
            try {
                client.deleteNetworkInterface(stackName, networkInterfacesName)
                val task = armPollTaskFactory!!.newNetworkInterfaceDeleteStatusCheckerTask(authenticatedContext, armClient,
                        NetworkInterfaceCheckerContext(ArmCredentialView(authenticatedContext.cloudCredential),
                                stackName, networkInterfacesName))

                syncPollingScheduler!!.schedule(task)

            } catch (e: HttpResponseException) {
                if (e.statusCode != NOT_FOUND) {
                    throw CloudConnectorException(e.response.data.toString(), e)
                }
            } catch (e: Exception) {
                throw CloudConnectorException(String.format("Could not delete network interface: %s", networkInterfacesName), e)
            }

        }
    }

    @Throws(CloudConnectorException::class)
    private fun deleteVirtualMachine(authenticatedContext: AuthenticatedContext, client: AzureRMClient, stackName: String, privateInstanceId: String) {
        try {
            client.deleteVirtualMachine(stackName, privateInstanceId)
            val task = armPollTaskFactory!!.newVirtualMachineDeleteStatusCheckerTask(authenticatedContext, armClient,
                    VirtualMachineCheckerContext(ArmCredentialView(authenticatedContext.cloudCredential),
                            stackName, privateInstanceId))
            syncPollingScheduler!!.schedule(task)
        } catch (e: HttpResponseException) {
            if (e.statusCode != NOT_FOUND) {
                throw CloudConnectorException(e.response.data.toString(), e)
            }
        } catch (e: Exception) {
            throw CloudConnectorException(String.format("Could not delete virtual machine: %s", privateInstanceId), e)
        }

    }

    @Throws(CloudConnectorException::class)
    private fun deallocateVirtualMachine(authenticatedContext: AuthenticatedContext, client: AzureRMClient, stackName: String, privateInstanceId: String) {
        try {
            client.deallocateVirtualMachine(stackName, privateInstanceId)
            val task = armPollTaskFactory!!.newVirtualMachineStatusCheckerTask(authenticatedContext, armClient,
                    VirtualMachineCheckerContext(ArmCredentialView(authenticatedContext.cloudCredential),
                            stackName, privateInstanceId, "Succeeded"))
            syncPollingScheduler!!.schedule(task)
        } catch (e: HttpResponseException) {
            if (e.statusCode != NOT_FOUND) {
                throw CloudConnectorException(e.response.data.toString(), e)
            }
        } catch (e: Exception) {
            throw CloudConnectorException(String.format("Could not deallocate machine: %s", privateInstanceId), e)
        }

    }

    private fun getNameFromConnectionString(connection: String): String {
        return connection.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[connection.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size - 1]
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ArmResourceConnector::class.java)
    }

}
