package com.sequenceiq.cloudbreak.shell.commands.provider

import java.io.File
import java.util.Collections
import java.util.HashMap

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.util.StringUtils

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.InstanceProfileStrategy
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands
import com.sequenceiq.cloudbreak.shell.commands.StackCommands
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands
import com.sequenceiq.cloudbreak.shell.completion.AwsInstanceType
import com.sequenceiq.cloudbreak.shell.completion.AwsOrchestratorType
import com.sequenceiq.cloudbreak.shell.completion.AwsVolumeType
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone
import com.sequenceiq.cloudbreak.shell.completion.StackRegion
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class AwsCommands(private val shellContext: ShellContext,
                  private val baseCredentialCommands: CredentialCommands,
                  private val baseNetworkCommands: NetworkCommands,
                  private val baseTemplateCommands: TemplateCommands,
                  private val basePlatformCommands: PlatformCommands,
                  private val stackCommands: StackCommands) : CommandMarker {

    @CliAvailabilityIndicator(value = "stack create --AWS")
    fun createStackAvailable(): Boolean {
        return stackCommands.createStackAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "template create --AWS")
    fun createTemplateAvailable(): Boolean {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "platform create --AWS")
    fun createPlatformAvailable(): Boolean {
        return basePlatformCommands.createPlatformAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = *arrayOf("network create --AWS --NEW_SUBNET", "network create --AWS --NEW", "network create --AWS --EXISTING_SUBNET"))
    fun createNetworkAvailable(): Boolean {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = *arrayOf("credential create --AWS", "template create --EC2"))
    fun createCredentialAvailable(): Boolean {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM)
    }

    @CliCommand(value = "credential create --AWS", help = "Create a new AWS credential")
    fun createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") name: String,
            @CliOption(key = "roleArn", mandatory = false, help = "roleArn for assuming roles or use access and secret based authentication") roleArn: String?,
            @CliOption(key = "accessKey", mandatory = false, help = "accessKey of the credential") accessKey: String?,
            @CliOption(key = "secretKey", mandatory = false, help = "secretKey of the credential") secretKey: String?,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") sshKeyPath: File,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") sshKeyUrl: String,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") sshKeyString: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") platformId: Long?,
            @CliOption(key = "existingKeyPairName", mandatory = false, help = "Name of an existing SSH key pair that should be exist on EC2") keyPairName: String): String {
        val parameters = HashMap<String, Any>()
        if (roleArn != null) {
            parameters.put("selector", "role-based")
            parameters.put("roleArn", roleArn)
        } else if (accessKey != null && secretKey != null) {
            parameters.put("selector", "key-based")
            parameters.put("accessKey", accessKey)
            parameters.put("secretKey", secretKey)
        } else {
            return "Please specify the roleArn or both the access and secret key"
        }
        if (!StringUtils.isEmpty(keyPairName)) {
            parameters.put("existingKeyPairName", keyPairName)
        }
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --AWS --NEW", help = "Create an AWS network configuration with a new network and a new subnet")
    fun createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --AWS --NEW_SUBNET", help = "Create an AWS network configuration with a new subnet in an existing network")
    fun createNetworkWithNewSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "vpcID", mandatory = true, help = "The ID of the virtual private cloud (VPC)") vpcId: String,
            @CliOption(key = "internetGatewayID", mandatory = true, help = "The ID of the internet gateway that is attached to the VPC (configured via 'vpcID' option)")
            internetGatewayId: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        parameters.put("vpcId", vpcId)
        parameters.put("internetGatewayId", internetGatewayId)
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --AWS --EXISTING_SUBNET", help = "Create an AWS network which use an existing subnet in an existing network")
    fun createNetworkWithExistingSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "vpcID", mandatory = true, help = "The ID of the virtual private cloud (VPC)") vpcId: String,
            @CliOption(key = "subnetId", mandatory = true, help = "The ID of the subnet which belongs to the custom VPC") subnetId: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        parameters.put("vpcId", vpcId)
        parameters.put("subnetId", subnetId)
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "template create --AWS", help = "Create a new AWS template")
    fun createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") name: String,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") instanceType: AwsInstanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") volumeCount: Int?,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") volumeSize: Int?,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") volumeType: AwsVolumeType,
            @CliOption(key = "encrypted", mandatory = false, help = "use encrypted disks") encrypted: Boolean?,
            @CliOption(key = "spotPrice", mandatory = false, help = "spotPrice of the template") spotPrice: Double?,
            @CliOption(key = "sshLocation", mandatory = false, specifiedDefaultValue = "0.0.0.0/0", help = "sshLocation of the template") sshLocation: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") description: String,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the template belongs to") platformId: Long?): String {
        return createEc2Template(name, instanceType, volumeCount, volumeSize, volumeType, encrypted, spotPrice, sshLocation, publicInAccount,
                description, platformId)
    }

    @CliCommand(value = "template create --EC2", help = "Create a new AWS template")
    fun createEc2Template(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") name: String,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") instanceType: AwsInstanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") volumeCount: Int?,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") volumeSize: Int?,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") volumeType: AwsVolumeType?,
            @CliOption(key = "encrypted", mandatory = false, help = "use encrypted disks") encrypted: Boolean?,
            @CliOption(key = "spotPrice", mandatory = false, help = "spotPrice of the template") spotPrice: Double?,
            @CliOption(key = "sshLocation", mandatory = false, specifiedDefaultValue = "0.0.0.0/0", help = "sshLocation of the template") sshLocation: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") platformId: Long?): String {
        val params = HashMap<String, Any>()
        params.put("sshLocation", sshLocation ?: "0.0.0.0/0")
        params.put("spotPrice", spotPrice?.toString())
        params.put("encrypted", encrypted?.toString())
        return baseTemplateCommands.create(name, instanceType.name, volumeCount, volumeSize, if (volumeType == null) "gp2" else volumeType.name,
                publicInAccount, description, params, platformId, PLATFORM)
    }

    @CliCommand(value = "platform create --AWS", help = "Create a new AWS platform configuration")
    fun createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") name: String,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") description: String): String {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, emptyMap<String, String>())
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliCommand(value = "stack create --AWS", help = "Create a new AWS stack based on a template")
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
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") platformVariant: PlatformVariant,
            @CliOption(key = "orchestrator", mandatory = false, help = "select orchestrator variant version") orchestratorType: AwsOrchestratorType?,
            @CliOption(key = "dedicatedInstances", mandatory = false, help = "request dedicated instances on AWS") dedicatedInstances: Boolean?,
            @CliOption(key = "instanceProfileStrategy", mandatory = false, help = "seamless S3 access type", specifiedDefaultValue = "false")
            instanceProfileStrategy: InstanceProfileStrategy?,
            @CliOption(key = "s3Role", mandatory = false, help = "seamless S3 access role", specifiedDefaultValue = "false") s3Role: String?,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") wait: Boolean?): String {

        val params = HashMap<String, String>()
        if (dedicatedInstances != null) {
            params.put("dedicatedInstances", dedicatedInstances.toString())
        }
        if (instanceProfileStrategy != null) {
            params.put("instanceProfileStrategy", instanceProfileStrategy.toString())
        }
        if (s3Role != null && InstanceProfileStrategy.USE_EXISTING == instanceProfileStrategy) {
            params.put("s3Role", s3Role.toString())
        }
        if (s3Role != null && InstanceProfileStrategy.USE_EXISTING != instanceProfileStrategy) {
            return "Please specify the role for S3 connection if you are using 'USE_EXISTING' profile type."
        }
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold, false,
                wait, platformVariant, if (orchestratorType == null) "SALT" else orchestratorType.name, PLATFORM, ambariVersion, hdpVersion, params)
    }

    companion object {

        val PLATFORM = "AWS"
    }
}
