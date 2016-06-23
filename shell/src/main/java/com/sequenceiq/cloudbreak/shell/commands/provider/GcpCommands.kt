package com.sequenceiq.cloudbreak.shell.commands.provider

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Collections
import java.util.HashMap

import org.apache.commons.codec.binary.Base64
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands
import com.sequenceiq.cloudbreak.shell.commands.StackCommands
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands
import com.sequenceiq.cloudbreak.shell.completion.GcpInstanceType
import com.sequenceiq.cloudbreak.shell.completion.GcpOrchestratorType
import com.sequenceiq.cloudbreak.shell.completion.GcpVolumeType
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone
import com.sequenceiq.cloudbreak.shell.completion.StackRegion
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class GcpCommands(private val shellContext: ShellContext,
                  private val baseCredentialCommands: CredentialCommands,
                  private val baseNetworkCommands: NetworkCommands,
                  private val baseTemplateCommands: TemplateCommands,
                  private val basePlatformCommands: PlatformCommands,
                  private val stackCommands: StackCommands) : CommandMarker {

    @CliAvailabilityIndicator(value = "stack create --GCP")
    fun createStackAvailable(): Boolean {
        return stackCommands.createStackAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "template create --GCP")
    fun createTemplateAvailable(): Boolean {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "platform create --GCP")
    fun createPlatformAvailable(): Boolean {
        return basePlatformCommands.createPlatformAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = *arrayOf("network create --GCP --NEW", "network create --GCP --NEW_SUBNET", "network create --GCP --EXISTING_SUBNET", "network create --GCP --LEGACY"))
    fun createNetworkAvailable(): Boolean {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "credential create --GCP")
    fun createCredentialAvailable(): Boolean {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM)
    }

    @CliCommand(value = "credential create --GCP", help = "Create a new GCP credential")
    fun createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") name: String,
            @CliOption(key = "projectId", mandatory = true, help = "projectId of the credential") projectId: String,
            @CliOption(key = "serviceAccountId", mandatory = true, help = "serviceAccountId of the credential") serviceAccountId: String,
            @CliOption(key = "serviceAccountPrivateKeyPath", mandatory = true, help = "path of a service account private key (p12) file")
            serviceAccountPrivateKeyPath: File,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") sshKeyPath: File,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key url") sshKeyUrl: String,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") sshKeyString: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") platformId: Long?): String {
        val serviceAccountPrivateKey: String

        try {
            serviceAccountPrivateKey = Base64.encodeBase64String(Files.readAllBytes(serviceAccountPrivateKeyPath.toPath())).replace("\n".toRegex(), "")
        } catch (ex: IOException) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(P12_FILE_NOT_FOUND)
        }

        val parameters = HashMap<String, Any>()
        parameters.put("projectId", projectId)
        parameters.put("serviceAccountId", serviceAccountId)
        parameters.put("serviceAccountPrivateKey", serviceAccountPrivateKey)
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --GCP --NEW", help = "Create a GCP network configuration with a new network and a new subnet")
    fun createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --GCP --NEW_SUBNET", help = "Create a GCP network configuration with a new subnet in an existing network")
    fun createNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "networkId", mandatory = true, help = "Id of a custom network") networkId: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        if (networkId != null) {
            parameters.put("networkId", networkId)
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --GCP --EXISTING_SUBNET", help = "Create a GCP network which use an existing subnet in an existing network")
    fun createExistingSubnetNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "networkId", mandatory = true, help = "Id of a custom network") networkId: String?,
            @CliOption(key = "subnetId", mandatory = true, help = "Id of a custom subnet") subnetId: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        if (networkId != null) {
            parameters.put("networkId", networkId)
        }
        if (subnetId != null) {
            parameters.put("subnetId", subnetId)
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --GCP --LEGACY", help = "Create a legacy GCP network configuration without subnet")
    fun createLegacyNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "networkId", mandatory = false, help = "Id of a custom network") networkId: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        if (networkId != null) {
            parameters.put("networkId", networkId)
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "template create --GCP", help = "Create a new GCP template")
    fun createGcpTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") name: String,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") instanceType: GcpInstanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") volumeCount: Int?,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") volumeSize: Int?,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") volumeType: GcpVolumeType?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        return baseTemplateCommands.create(name, instanceType.name, volumeCount, volumeSize, if (volumeType == null) "pd-standard" else volumeType.name,
                publicInAccount, description, parameters, platformId, PLATFORM)
    }

    @CliCommand(value = "cluster fileSystem --GCS", help = "Set GCS fileSystem on cluster")
    fun setGcsFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default fileSystem") defaultFileSystem: Boolean?,
            @CliOption(key = "projectId", mandatory = true, help = "projectId of the GCS") projectId: String,
            @CliOption(key = "serviceAccountEmail", mandatory = true, help = "serviceAccountEmail of the GCS") serviceAccountEmail: String,
            @CliOption(key = "privateKeyEncoded", mandatory = true, help = "privateKeyEncoded of the GCS") privateKeyEncoded: String,
            @CliOption(key = "defaultBucketName", mandatory = true, help = "defaultBucketName of the GCS") defaultBucketName: String): String {
        shellContext.defaultFileSystem = defaultFileSystem
        shellContext.fileSystemType = FileSystemType.GCS
        val props = HashMap<String, Any>()
        props.put("projectId", projectId)
        props.put("serviceAccountEmail", serviceAccountEmail)
        props.put("privateKeyEncoded", privateKeyEncoded)
        props.put("defaultBucketName", defaultBucketName)
        shellContext.fileSystemParameters = props
        return "GCS filesystem configured"
    }

    @CliCommand(value = "platform create --GCP", help = "Create a new GCP platform configuration")
    fun createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") name: String,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") description: String): String {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, emptyMap<String, String>())
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliCommand(value = "stack create --GCP", help = "Create a new GCP stack based on a template")
    fun create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") name: String,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") region: StackRegion,
            @CliOption(key = "availabilityZone", mandatory = false, help = "availabilityZone of the stack") availabilityZone: StackAvailabilityZone?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "marks the stack as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "onFailureAction", mandatory = false, help = "onFailureAction which is ROLLBACK or DO_NOTHING.") onFailureAction: OnFailureAction,
            @CliOption(key = "adjustmentType", mandatory = false, help = "adjustmentType which is EXACT or PERCENTAGE.") adjustmentType: AdjustmentType,
            @CliOption(key = "ambariVersion", mandatory = false, help = "Ambari version") ambariVersion: String,
            @CliOption(key = "hdpVersion", mandatory = false, help = "HDP version") hdpVersion: String,
            @CliOption(key = "threshold", mandatory = false, help = "threshold of failure") threshold: Long?,
            @CliOption(key = "orchestrator", mandatory = false, help = "select orchestrator variant version") orchestratorType: GcpOrchestratorType?,
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") platformVariant: PlatformVariant,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") wait: Boolean?): String {
        var availabilityZone = availabilityZone
        val params = HashMap<String, String>()
        if (availabilityZone == null) {
            val availabilityZonesByRegion = shellContext.getAvailabilityZonesByRegion(shellContext.activeCloudPlatform, region.name)
            availabilityZone = StackAvailabilityZone(availabilityZonesByRegion.iterator().next())
        }
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold,
                false, wait, platformVariant, if (orchestratorType == null) "SALT" else orchestratorType.name, PLATFORM, ambariVersion, hdpVersion, params)
    }

    companion object {

        val PLATFORM = "GCP"
        private val P12_FILE_NOT_FOUND = "File not found with service account private key (p12) file."
    }

}
