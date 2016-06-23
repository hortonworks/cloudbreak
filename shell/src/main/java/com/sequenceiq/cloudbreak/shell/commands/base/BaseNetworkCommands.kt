package com.sequenceiq.cloudbreak.shell.commands.base

import com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource

import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class BaseNetworkCommands(private val shellContext: ShellContext) : BaseCommands, NetworkCommands {

    @CliAvailabilityIndicator(value = *arrayOf("network delete --id", "network delete --name"))
    override fun deleteAvailable(): Boolean {
        return !shellContext.networksByProvider.isEmpty() && !shellContext.isMarathonMode
    }

    @CliCommand(value = "network delete --id", help = "Delete the network by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "network delete --name", help = "Delete the network by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    @Throws(Exception::class)
    override fun delete(id: Long?, name: String?): String {
        try {
            val networkId = if (id == null) null else id
            val networkName = if (name == null) null else name
            if (networkId != null) {
                shellContext.cloudbreakClient().networkEndpoint().delete(networkId)
                refreshNetworksInContext()
                return String.format("Network deleted with %s id", networkId)
            } else if (networkName != null) {
                shellContext.cloudbreakClient().networkEndpoint().deletePublic(networkName)
                refreshNetworksInContext()
                return String.format("Network deleted with %s name", networkName)
            }
            return "No network specified."
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("network select --id", "network select --name"))
    override fun selectAvailable(): Boolean {
        return !shellContext.networksByProvider.isEmpty() && !shellContext.isMarathonMode
    }

    @CliCommand(value = "network select --id", help = "Delete the network by its id")
    @Throws(Exception::class)
    override fun selectById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return select(id, null)
    }

    @CliCommand(value = "network select --name", help = "Delete the network by its name")
    @Throws(Exception::class)
    override fun selectByName(@CliOption(key = "", mandatory = true) name: String): String {
        return select(null, name)
    }

    override fun select(idOfNetwork: Long?, networkName: String?): String {
        try {
            var msg = "Network could not be found."
            val id = if (idOfNetwork == null) null else idOfNetwork
            val name = if (networkName == null) null else networkName
            if (id != null && shellContext.networksByProvider.containsKey(id)) {
                val provider = shellContext.networksByProvider[id]
                createHintAndAddNetworkToContext(id, provider)
                msg = "Network is selected with id: " + id
            } else if (name != null) {
                val aPublic = shellContext.cloudbreakClient().networkEndpoint().getPublic(name)
                if (aPublic != null) {
                    createHintAndAddNetworkToContext(java.lang.Long.valueOf(aPublic.id), name)
                    msg = "Network is selected with name: " + name
                }
            }
            return msg
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "network list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "network list", help = "Shows the currently available networks configurations")
    @Throws(Exception::class)
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().networkEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("network show --id", "network show --name"))
    override fun showAvailable(): Boolean {
        return !shellContext.networksByProvider.isEmpty() && !shellContext.isMarathonMode
    }

    @CliCommand(value = "network show --id", help = "Show the network by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "network show --name", help = "Show the network by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    @Throws(Exception::class)
    override fun show(id: Long?, name: String?): String {
        try {
            if (id != null) {
                val networkJson = shellContext.cloudbreakClient().networkEndpoint()[id]
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(networkJson), "FIELD", "VALUE")
            } else if (name != null) {
                val aPublic = shellContext.cloudbreakClient().networkEndpoint().getPublic(name)
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE")
            }
            return "Network could not be found!"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun createNetworkAvailable(platform: String): Boolean {
        return !shellContext.isMarathonMode
    }

    override fun create(name: String, subnet: String, publicInAccount: Boolean?, description: String, platformId: Long?, parameters: Map<String, Any>,
                        platform: String): String {
        var publicInAccount = publicInAccount
        try {
            val networkJson = NetworkJson()
            networkJson.name = name
            networkJson.description = description
            networkJson.isPublicInAccount = if (publicInAccount == null) false else publicInAccount
            networkJson.cloudPlatform = platform
            networkJson.parameters = parameters
            networkJson.subnetCIDR = subnet
            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().publics, platformId, platform)
            }
            networkJson.topologyId = platformId

            val id: IdJson
            publicInAccount = if (publicInAccount == null) false else publicInAccount
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().networkEndpoint().postPublic(networkJson)
            } else {
                id = shellContext.cloudbreakClient().networkEndpoint().postPrivate(networkJson)
            }
            createHintAndAddNetworkToContext(id.id, platform)
            return String.format(CREATE_SUCCESS_MSG, id.id, name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    @Throws(Exception::class)
    private fun createHintAndAddNetworkToContext(id: Long?, provider: String) {
        shellContext.setHint(Hints.SELECT_SECURITY_GROUP)
        shellContext.putNetwork(id, provider)
        shellContext.activeNetworkId = id
    }

    @Throws(Exception::class)
    private fun refreshNetworksInContext() {
        shellContext.networksByProvider.clear()
        val publics = shellContext.cloudbreakClient().networkEndpoint().publics
        for (network in publics) {
            shellContext.putNetwork(java.lang.Long.valueOf(network.id), network.cloudPlatform)
        }
        if (!shellContext.networksByProvider.containsKey(shellContext.activeNetworkId)) {
            shellContext.activeNetworkId = null
        }
    }

    companion object {
        private val CREATE_SUCCESS_MSG = "Network created and selected successfully, with id: '%s' and name: '%s'"
    }

}
