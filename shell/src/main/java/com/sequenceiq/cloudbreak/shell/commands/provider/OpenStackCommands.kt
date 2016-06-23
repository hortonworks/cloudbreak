package com.sequenceiq.cloudbreak.shell.commands.provider

import java.io.File
import java.util.HashMap

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands
import com.sequenceiq.cloudbreak.shell.commands.StackCommands
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands
import com.sequenceiq.cloudbreak.shell.completion.OpenStackFacing
import com.sequenceiq.cloudbreak.shell.completion.OpenStackOrchestratorType
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone
import com.sequenceiq.cloudbreak.shell.completion.StackRegion
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class OpenStackCommands(private val shellContext: ShellContext,
                        private val baseCredentialCommands: CredentialCommands,
                        private val baseNetworkCommands: NetworkCommands,
                        private val baseTemplateCommands: TemplateCommands,
                        private val basePlatformCommands: PlatformCommands,
                        private val stackCommands: StackCommands) : CommandMarker {

    @CliAvailabilityIndicator(value = "stack create --OPENSTACK")
    fun createStackAvailable(): Boolean {
        return stackCommands.createStackAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "template create --OPENSTACK")
    fun createTemplateAvailable(): Boolean {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "platform create --OPENSTACK")
    fun createPlatformAvailable(): Boolean {
        return basePlatformCommands.createPlatformAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = *arrayOf("network create --OPENSTACK --NEW", "network create --OPENSTACK --EXISTING", "network create --OPENSTACK --NEW_SUBNET"))
    fun createNetworkAvailable(): Boolean {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM)
    }

    @CliAvailabilityIndicator(value = "credential create --OPENSTACK")
    fun createCredentialAvailable(): Boolean {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM)
    }

    @CliCommand(value = "credential create --OPENSTACK", help = "Create a new OpenStack credential")
    fun createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") name: String,
            @CliOption(key = "userName", mandatory = true, help = "Username of the credential") userName: String,
            @CliOption(key = "password", mandatory = true, help = "password of the credential") password: String,
            @CliOption(key = "endPoint", mandatory = true, help = "endPoint of the credential") endPoint: String,
            @CliOption(key = "tenantName", mandatory = false, help = "tenantName of the credential for cb-keystone-v2") tenantName: String?,
            @CliOption(key = "userDomain", mandatory = false, help = "userDomain of the credential for cb-keystone-v3*") userDomain: String?,
            @CliOption(key = "keystoneAuthScope", mandatory = false, help = "keystoneAuthScope of the credential for cb-keystone-v3*") keystoneAuthScope: String?,
            @CliOption(key = "domainName", mandatory = false, help = "domainName of the credential for cb-keystone-v3-default-scope") domainName: String?,
            @CliOption(key = "projectDomainName", mandatory = false, help = "projectDomainName of the credential for cb-keystone-v3-project-scope")
            projectDomainName: String?,
            @CliOption(key = "projectName", mandatory = false, help = "projectName of the credential for cb-keystone-v3-project-scope") projectName: String?,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") sshKeyPath: File,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") sshKeyUrl: String,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") sshKeyString: String,
            @CliOption(key = "facing", mandatory = false, help = "URL perspective in which the API is accessing data") facing: OpenStackFacing?,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") description: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") platformId: Long?): String {
        var selector: String? = null
        var keyStoneVersion: String? = null
        if (tenantName != null) {
            selector = "cb-keystone-v2"
            keyStoneVersion = "cb-keystone-v2"
        }
        if (userDomain != null && keystoneAuthScope != null) {
            if (domainName != null) {
                selector = "cb-keystone-v3-domain-scope"
            } else if (projectDomainName != null && projectName != null) {
                selector = "cb-keystone-v3-project-scope"
            } else {
                selector = "cb-keystone-v3-default-scope"
            }
            keyStoneVersion = "cb-keystone-v3"
        }
        if (selector == null || keyStoneVersion == null) {
            return "Selector not found for specified parameters."
        }
        val parameters = HashMap<String, Any>()
        parameters.put("userName", userName)
        parameters.put("password", password)
        parameters.put("endpoint", endPoint)
        parameters.put("keystoneVersion", keyStoneVersion)
        parameters.put("selector", selector)
        parameters.put("tenantName", tenantName)
        parameters.put("userDomain", userDomain)
        parameters.put("keystoneAuthScope", keystoneAuthScope)
        parameters.put("domainName", domainName)
        parameters.put("projectDomainName", projectDomainName)
        parameters.put("projectName", projectName)
        if (facing != null) {
            parameters.put("facing", facing.name)
        }
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --OPENSTACK --EXISTING_SUBNET", help = "Create an OpenStack network which use an existing subnet in an existing network")
    fun createExisitngNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "networkId", mandatory = true, help = "ID of the custom network to use") networkId: String,
            @CliOption(key = "subnetId", mandatory = true, help = "ID of the custom subnet to use") subnetId: String,
            @CliOption(key = "publicNetID", mandatory = false, help = "ID of the available and desired OpenStack public network") publicNetID: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        parameters.put("networkId", networkId)
        parameters.put("subnetId", subnetId)
        if (publicNetID != null) {
            parameters.put("publicNetId", publicNetID)
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --OPENSTACK --NEW", help = "Create an OpenStack network configuration with a new network and a new subnet")
    fun createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "publicNetID", mandatory = false, help = "ID of the available and desired OpenStack public network") publicNetID: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        if (publicNetID != null) {
            parameters.put("publicNetId", publicNetID)
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "network create --OPENSTACK --NEW_SUBNET", help = "Create an OpenStack network configuration with a new subnet in an existing network")
    fun createNetworkWithNewSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") name: String,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") subnet: String,
            @CliOption(key = "networkId", mandatory = true, help = "ID of the custom network to use") networkId: String,
            @CliOption(key = "routerId", mandatory = true, help = "ID of the custom router to use") routerId: String,
            @CliOption(key = "publicNetID", mandatory = false, help = "ID of the available and desired OpenStack public network") publicNetID: String?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        parameters.put("networkId", networkId)
        parameters.put("routerId", routerId)
        if (publicNetID != null) {
            parameters.put("publicNetId", publicNetID)
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM)
    }

    @CliCommand(value = "template create --OPENSTACK", help = "Create a new OpenStack template")
    fun createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") name: String,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") instanceType: String,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") volumeCount: Int?,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") volumeSize: Int?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") publicInAccount: Boolean?,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") description: String,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") platformId: Long?): String {
        val parameters = HashMap<String, Any>()
        if (volumeCount < 1) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Count of volumes could not be smaller than 1.")
        }
        if (volumeSize < TEN) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Size of volumes could not be smaller than 10 Gb.")
        }
        return baseTemplateCommands.create(name, instanceType, volumeCount, volumeSize, "HDD", publicInAccount, description, parameters, platformId, PLATFORM)
    }

    @CliCommand(value = "platform create --OPENSTACK", help = "Create a new OpenStack platform configuration")
    fun createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") name: String,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") description: String,
            @CliOption(key = "url", mandatory = false, help = "URL of the topology mapping file to download from") url: String,
            @CliOption(key = "file", mandatory = false, help = "File which contains the topology mapping") file: File): String {
        try {
            return basePlatformCommands.create(name, description, "OPENSTACK", basePlatformCommands.convertMappingFile(file, url))
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliCommand(value = "stack create --OPENSTACK", help = "Create a new OpenStack stack based on a template")
    fun create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") name: String,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") region: StackRegion,
            @CliOption(key = "availabilityZone", mandatory = false, help = "availabilityZone of the stack") availabilityZone: StackAvailabilityZone,
            @CliOption(key = "publicInAccount", mandatory = false, help = "marks the stack as visible for all members of the account") publicInAccount: Boolean?,
            @CliOption(key = "ambariVersion", mandatory = false, help = "Ambari version") ambariVersion: String,
            @CliOption(key = "hdpVersion", mandatory = false, help = "HDP version") hdpVersion: String,
            @CliOption(key = "onFailureAction", mandatory = false, help = "onFailureAction which is ROLLBACK or DO_NOTHING.") onFailureAction: OnFailureAction,
            @CliOption(key = "adjustmentType", mandatory = false, help = "adjustmentType which is EXACT or PERCENTAGE.") adjustmentType: AdjustmentType,
            @CliOption(key = "threshold", mandatory = false, help = "threshold of failure") threshold: Long?,
            @CliOption(key = "orchestrator", mandatory = false, help = "select orchestrator variant version") orchestratorType: OpenStackOrchestratorType?,
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") platformVariant: PlatformVariant,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") wait: Boolean?): String {
        val params = HashMap<String, String>()
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold,
                false, wait, platformVariant, if (orchestratorType == null) "SALT" else orchestratorType.name, PLATFORM, ambariVersion, hdpVersion, params)
    }

    companion object {

        val PLATFORM = "OPENSTACK"
        private val TEN = 10
    }
}
