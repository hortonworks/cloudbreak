package com.sequenceiq.cloudbreak.shell.commands.common

import java.util.ArrayList
import java.util.HashMap

import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class SecurityGroupCommands(private val shellContext: ShellContext) : BaseCommands {

    @CliAvailabilityIndicator(value = "securitygroup create")
    fun createAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "securitygroup create", help = "Creates a new security group")
    @Throws(Exception::class)
    fun create(
            @CliOption(key = "name", mandatory = true, help = "Name of the security group") name: String,
            @CliOption(key = "description", mandatory = false, help = "Description of the security group") description: String,
            @CliOption(key = "rules", mandatory = true, help = "Security rules in the following format: ';' separated list of <cidr>:<protocol>:<comma separated port list>")
            rules: SecurityRules,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the securitygroup as visible for all members of the account", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false")
            publicInAccount: Boolean?): String {
        var publicInAccount = publicInAccount
        try {
            val tcpRules = HashMap<String, String>()
            val udpRules = HashMap<String, String>()
            for (rule in rules.rules) {
                if ("tcp" == rule["protocol"]) {
                    tcpRules.put(rule["subnet"], rule["ports"])
                } else {
                    udpRules.put(rule["subnet"], rule["ports"])
                }
            }
            val id: IdJson
            val securityGroupJson = SecurityGroupJson()
            securityGroupJson.name = name
            securityGroupJson.description = description
            securityGroupJson.isPublicInAccount = publicInAccount
            val securityRuleJsonList = ArrayList<SecurityRuleJson>()

            for (stringStringEntry in tcpRules.entries) {
                val securityRuleJson = SecurityRuleJson()
                securityRuleJson.ports = stringStringEntry.value
                securityRuleJson.subnet = stringStringEntry.key
                securityRuleJson.protocol = "tcp"
                securityRuleJsonList.add(securityRuleJson)
            }

            for (stringStringEntry in udpRules.entries) {
                val securityRuleJson = SecurityRuleJson()
                securityRuleJson.ports = stringStringEntry.value
                securityRuleJson.subnet = stringStringEntry.key
                securityRuleJson.protocol = "udp"
                securityRuleJsonList.add(securityRuleJson)
            }

            securityGroupJson.securityRules = securityRuleJsonList
            publicInAccount = if (publicInAccount == null) false else publicInAccount

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().securityGroupEndpoint().postPublic(securityGroupJson)
            } else {
                id = shellContext.cloudbreakClient().securityGroupEndpoint().postPrivate(securityGroupJson)
            }
            shellContext.putSecurityGroup(id.id, name)
            shellContext.activeSecurityGroupId = id.id
            setHint()
            return String.format(CREATE_SUCCESS_MSG, id.id, name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("securitygroup delete --id", "securitygroup delete --name"))
    override fun deleteAvailable(): Boolean {
        return !shellContext.securityGroups.isEmpty() && !shellContext.isMarathonMode
    }

    override fun delete(securityGroupId: Long?, securityGroupName: String?): String {
        try {
            val id = if (securityGroupId == null) null else securityGroupId
            val name = if (securityGroupName == null) null else securityGroupName
            if (id != null) {
                shellContext.cloudbreakClient().securityGroupEndpoint().delete(java.lang.Long.valueOf(id))
                refreshSecurityGroupsInContext()
                return String.format("SecurityGroup deleted with %s id", name)
            } else if (name != null) {
                shellContext.cloudbreakClient().securityGroupEndpoint().deletePublic(name)
                refreshSecurityGroupsInContext()
                return String.format("SecurityGroup deleted with %s name", name)
            }
            return "No security group specified."
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "securitygroup delete --id", help = "Delete the securitygroup by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "securitygroup delete --name", help = "Delete the securitygroup by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("securitygroup select --id", "securitygroup select --name"))
    override fun selectAvailable(): Boolean {
        return !shellContext.securityGroups.isEmpty() && !shellContext.isMarathonMode
    }

    override fun select(secId: Long?, secName: String?): String {
        if (secId == null && secName == null) {
            return "Both ID and name cannot be null"
        }
        var message = "No security group has been selected"
        val id = if (secId == null) null else secId
        val name = if (secName == null) null else secName
        val securityGroups = shellContext.securityGroups
        if (id != null && securityGroups.containsKey(id)) {
            shellContext.activeSecurityGroupId = id
            setHint()
            message = "Security group has been selected with id: " + id
        } else if (securityGroups.containsValue(name)) {
            for (groupId in securityGroups.keys) {
                if (securityGroups[groupId] == name) {
                    shellContext.activeSecurityGroupId = groupId
                    setHint()
                    message = "Security group has been selected with name: " + name!!
                    break
                }
            }
        }
        return message
    }

    @CliCommand(value = "securitygroup select --id", help = "Delete the securitygroup by its id")
    @Throws(Exception::class)
    override fun selectById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return select(id, null)
    }

    @CliCommand(value = "securitygroup select --name", help = "Delete the securitygroup by its name")
    @Throws(Exception::class)
    override fun selectByName(@CliOption(key = "", mandatory = true) name: String): String {
        return select(null, name)
    }

    @CliAvailabilityIndicator(value = "securitygroup list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "securitygroup list", help = "Shows the currently available security groups")
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().securityGroupEndpoint().publics
            val updatedGroups = HashMap<Long, String>()
            for (aPublic in publics) {
                updatedGroups.put(aPublic.id, aPublic.name)
            }
            shellContext.securityGroups = updatedGroups
            return shellContext.outputTransformer().render<Map<Long, String>>(updatedGroups, "ID", "NAME")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("securitygroup show --id", "securitygroup show --name"))
    override fun showAvailable(): Boolean {
        return !shellContext.securityGroups.isEmpty() && !shellContext.isMarathonMode
    }

    override fun show(groupId: Long?, groupName: String?): String {
        try {
            val id = if (groupId == null) null else groupId
            val name = if (groupName == null) null else groupName
            if (id != null) {
                val securityGroupJson = shellContext.cloudbreakClient().securityGroupEndpoint()[java.lang.Long.valueOf(id)]
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(securityGroupJson),
                        "FIELD", "VALUE")
            } else if (name != null) {
                val aPublic = shellContext.cloudbreakClient().securityGroupEndpoint().getPublic(name)
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE")
            }
            return "Security group could not be found!"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "securitygroup show --id", help = "Show the securitygroup by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "securitygroup show --name", help = "Show the securitygroup by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    @Throws(Exception::class)
    private fun refreshSecurityGroupsInContext() {
        shellContext.securityGroups.clear()
        val publics = shellContext.cloudbreakClient().securityGroupEndpoint().publics
        for (securityGroup in publics) {
            shellContext.putSecurityGroup(securityGroup.id, securityGroup.name)
        }
        if (!shellContext.securityGroups.containsKey(shellContext.activeSecurityGroupId)) {
            shellContext.activeSecurityGroupId = null
        }
    }

    private fun setHint() {
        shellContext.setHint(Hints.CREATE_STACK)
    }

    companion object {
        private val CREATE_SUCCESS_MSG = "Security group created and selected successfully, with id: '%d' and name: '%s'"
    }

}
