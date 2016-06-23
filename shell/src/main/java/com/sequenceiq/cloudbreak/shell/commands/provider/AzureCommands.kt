package com.sequenceiq.cloudbreak.shell.commands.provider

import java.io.File
import java.util.Collections
import java.util.HashMap

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands
import com.sequenceiq.cloudbreak.shell.commands.StackCommands
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands
import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType
import com.sequenceiq.cloudbreak.shell.completion.ArmOrchestratorType
import com.sequenceiq.cloudbreak.shell.completion.AzureVolumeType
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone
import com.sequenceiq.cloudbreak.shell.completion.StackRegion
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class AzureCommands(private val shellContext: ShellContext,
                    private val baseCredentialCommands: CredentialCommands,
                    private val baseNetworkCommands: NetworkCommands,
                    private val baseTemplateCommands: TemplateCommands,
                    private val basePlatformCommands: PlatformCommands,
                    private val stackCommands: StackCommands) : CommandMarker {

    @CliAvailabilityIndicator(value = "stack create --AZURE")
    fun createStackAvailable(): Boolean {
        return stackCommands.createStackAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "template create --AZURE")
    fun createTemplateAvailable(): Boolean {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "platform create --AZURE")
    fun createPlatformAvailable(): Boolean {
        return basePlatformCommands.createPlatformAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = *arrayOf("network create --AZURE --NEW", "network create --AZURE --EXISTING_SUBNET"))
    fun createNetworkAvailable(): Boolean {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "credential create --AZURE")
    fun createCredentialAvailable(): Boolean {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM)
    }


    @CliCommand(value = "credential create --AZURE", help = "Create a new Azure credential")
    fun createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") name: String,
            @CliOption(key = "subscriptionId", mandatory = true, help = "subscriptionId of the credential") subscriptionId: String,
            @CliOption(key = "tenantId", mandatory = true, help = "tenantId of the credential") tenantId: String,
            @CliOption(key = "appId", mandatory = true, help = "appId of the credential") appId: String,
            @CliOption(key = "password", mandatory = true, help = "password of the credential") password: String,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "sshKeyPath of the template") sshKeyPath: File,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "sshKeyUrl of the template") sshKeyUrl: String,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") sshKeyString: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        parameters.put("subscriptionId", subscriptionId)
        parameters.put("secretKey", password)
        parameters.put("tenantId", tenantId)
        parameters.put("accessKey", appId)
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --AZURE --NEW", help = "Create an Azure network configuration with a new network and a new subnet")
    fun createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --AZURE --EXISTING_SUBNET", help = "Create an Azure network which use an existing subnet in an existing network")
    fun createNetworkWithExistingSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "resourceGroupName", mandatory = true, help = "Name of the custom resource group in case of existing virtual network and subnet")
            rgName: String?,
            @CliOption(key = "networkId", mandatory = true, help = "Name of the custom network within the custom resource group") networkId: String?,
            @CliOption(key = "subnetId", mandatory = true, help = "Name of the custom subnet within the custom resource group") subnetId: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        if (rgName != null && networkId != null && subnetId != null) {
            parameters.put("resourceGroupName", rgName)
            parameters.put("networkId", networkId)
            parameters.put("subnetId", subnetId)
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "template create --AZURE", help = "Create a new Azure template")
    fun createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") name: String,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") instanceType: AzureInstanceType,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") volumeType: AzureVolumeType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") volumeCount: Int?,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") volumeSize: Int?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        return baseTemplateCommands.create(name, instanceType.name, volumeCount, volumeSize, volumeType.name, publicInAccount, description,
                parameters, platformId, PLATFORM)
    }

    @CliCommand(value = "cluster fileSystem --DASH", help = "Set Windows Azure Blob Storage filesystem with DASH on cluster")
    fun setAzureRmFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default filesystem") defaultFileSystem: Boolean?,
            @CliOption(key = "accountName", mandatory = true, help = "accountName of the DASH service") accountName: String,
            @CliOption(key = "accountKey", mandatory = true, help = "access key of the DASH service") accountKey: String): String {
        shellContext.defaultFileSystem = defaultFileSystem
        shellContext.fileSystemType = FileSystemType.DASH
        val props = HashMap<String, Any>()
        props.put("accountName", accountName)
        props.put("accountKey", accountKey)
        shellContext.fileSystemParameters = props
        return "Windows Azure Blob Storage with DASH configured as the filesystem"
    }

    @CliCommand(value = "cluster fileSystem --WASB", help = "Set Windows Azure Blob Storage filesystem on cluster")
    fun setWasbFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default filesystem") defaultFileSystem: Boolean?,
            @CliOption(key = "accountName", mandatory = true, help = "name of the storage account") accountName: String,
            @CliOption(key = "accountKey", mandatory = true, help = "primary access key to the storage account") accountKey: String): String {
        shellContext.defaultFileSystem = defaultFileSystem
        shellContext.fileSystemType = FileSystemType.WASB
        val props = HashMap<String, Any>()
        props.put("accountName", accountName)
        props.put("accountKey", accountKey)
        shellContext.fileSystemParameters = props
        return "Windows Azure Blob Storage filesystem configured"
    }

    @CliCommand(value = "platform create --AZURE", help = "Create a new Azure platform configuration")
    fun createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") name: String,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") description: String): String {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, emptyMap<String, String>())
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliCommand(value = "stack create --AZURE", help = "Create a new Azure stack based on a template")
    fun create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") name: String,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") region: StackRegion,
            @CliOption(key = "availabilityZone", mandatory = false, help = "availabilityZone of the stack") availabilityZone: StackAvailabilityZone,
            @CliOption(key = "publicInAccount", mandatory = false, help = "marks the stack as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "onFailureAction", mandatory = false, help = "onFailureAction which is ROLLBACK or DO_NOTHING.") onFailureAction: OnFailureAction,
            @CliOption(key = "adjustmentType", mandatory = false, help = "adjustmentType which is EXACT or PERCENTAGE.") adjustmentType: AdjustmentType,
            @CliOption(key = "ambariVersion", mandatory = false, help = "Ambari version") ambariVersion: String,
            @CliOption(key = "hdpVersion", mandatory = false, help = "HDP version") hdpVersion: String,
            @CliOption(key = "threshold", mandatory = false, help = "threshold of failure") threshold: Long?,
            @CliOption(key = "diskPerStorage", mandatory = false, help = "disk per Storage Account on Azure") diskPerStorage: Int?,
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") platformVariant: PlatformVariant,
            @CliOption(key = "relocateDocker", mandatory = false, help = "relocate docker in startup time") relocateDocker: Boolean?,
            @CliOption(key = "orchestrator", mandatory = false, help = "select orchestrator variant version") orchestratorType: ArmOrchestratorType?,
            @CliOption(key = "attachedStorageType", mandatory = false, help = "type of the storage creation") attachedStorageOption: ArmAttachedStorageOption?,
            @CliOption(key = "persistentStorage", mandatory = false, help = "name of the persistent storage")
            persistentStorage: String?,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") wait: Boolean?): String {
        var relocateDocker = relocateDocker
        var orchestratorType = orchestratorType

        orchestratorType = if (orchestratorType == null) ArmOrchestratorType(SALT) else orchestratorType
        if (SALT == orchestratorType.name) {
            relocateDocker = if (relocateDocker == null) false else relocateDocker
            if (relocateDocker) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("Relocate docker could not be 'yes' if you are not using containers in the cluster")
            }
        } else {
            relocateDocker = if (relocateDocker == null) true else relocateDocker
        }
        val params = HashMap<String, String>()

        if (diskPerStorage != null) {
            params.put("diskPerStorage", diskPerStorage.toString())
        }
        if (attachedStorageOption != null && shellContext.isAzureActiveCredential) {
            params.put("attachedStorageOption", attachedStorageOption.name)
        } else if (shellContext.isAzureActiveCredential) {
            params.put("attachedStorageOption", ArmAttachedStorageOption.SINGLE.name)
        }
        if (persistentStorage != null && shellContext.isAzureActiveCredential) {
            params.put("persistentStorage", persistentStorage)
        } else if (shellContext.isAzureActiveCredential) {
            params.put("persistentStorage", "cbstore")
        }
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold,
                relocateDocker, wait, platformVariant, orchestratorType.name, PLATFORM, ambariVersion, hdpVersion, params)
    }

    companion object {

        val PLATFORM = "AZURE_RM"
        val SALT = "SALT"
    }
}
