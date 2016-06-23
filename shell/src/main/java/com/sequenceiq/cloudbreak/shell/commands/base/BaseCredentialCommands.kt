package com.sequenceiq.cloudbreak.shell.commands.base

import com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

import org.apache.commons.io.IOUtils
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.CredentialRequest
import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class BaseCredentialCommands(private val shellContext: ShellContext) : BaseCommands, CredentialCommands {

    @CliAvailabilityIndicator(value = *arrayOf("credential delete --id", "credential delete --name"))
    override fun deleteAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "credential delete --id", help = "Delete the credential by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "credential delete --name", help = "Delete the credential by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    override fun delete(id: Long?, name: String?): String {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().credentialEndpoint().delete(id)
                return String.format("Credential deleted, id: %s", id)
            } else if (name != null) {
                shellContext.cloudbreakClient().credentialEndpoint().deletePublic(name)
                return String.format("Credential deleted, name: %s", name)
            }
            return "No credential specified (select a credential by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("credential select --id", "credential select --name"))
    override fun selectAvailable(): Boolean {
        return shellContext.isCredentialAccessible && !shellContext.isMarathonMode
    }

    @CliCommand(value = "credential select --id", help = "Delete the credential by its id")
    @Throws(Exception::class)
    override fun selectById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return select(id, null)
    }

    @CliCommand(value = "credential select --name", help = "Delete the credential by its name")
    @Throws(Exception::class)
    override fun selectByName(@CliOption(key = "", mandatory = true) name: String): String {
        return select(null, name)
    }

    override fun select(id: Long?, name: String?): String {
        try {

            if (id != null) {
                if (shellContext.cloudbreakClient().credentialEndpoint()[java.lang.Long.valueOf(id)] != null) {
                    shellContext.setCredential(id.toString())
                    createOrSelectTemplateHint()
                    return "Credential selected, id: " + id
                }
            } else if (name != null) {
                val aPublic = shellContext.cloudbreakClient().credentialEndpoint().getPublic(name)
                shellContext.setCredential(aPublic.id!!.toString())
                createOrSelectTemplateHint()
                return "Credential selected, name: " + name
            }
            return "No credential specified (select a credential by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "credential list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "credential list", help = "Shows all of your credentials")
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().credentialEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("credential show --id", "credential show --name"))
    override fun showAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "credential show --id", help = "Show the credential by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "credential show --name", help = "Show the credential by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    override fun show(id: Long?, name: String?): String {
        try {
            if (id != null) {
                val credentialResponse = shellContext.cloudbreakClient().credentialEndpoint()[java.lang.Long.valueOf(id)]
                val map = shellContext.responseTransformer().transformObjectToStringMap(credentialResponse)
                return shellContext.outputTransformer().render<Map<String, String>>(map, "FIELD", "VALUE")
            } else if (name != null) {
                val aPublic = shellContext.cloudbreakClient().credentialEndpoint().getPublic(name)
                if (aPublic != null) {
                    return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE")
                }
            }
            return "No credential specified (select a credential by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun createCredentialAvailable(platform: String): Boolean {
        return !shellContext.isMarathonMode
    }

    override fun create(name: String, sshKeyPath: File?, sshKeyUrl: String?, sshKeyString: String?, description: String, publicInAccount: Boolean?, platformId: Long?,
                        parameters: Map<String, Any>, platform: String): String {
        var publicInAccount = publicInAccount
        if (sshKeyPath == null && (sshKeyUrl == null || sshKeyUrl.isEmpty()) && sshKeyString == null) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl or --sshKeyString"
        }
        val sshKey: String
        if (sshKeyPath != null) {
            try {
                sshKey = IOUtils.toString(FileReader(File(sshKeyPath.path)))
            } catch (ex: IOException) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(FILE_NOT_FOUND)
            }

        } else if (sshKeyUrl != null) {
            try {
                sshKey = readUrl(sshKeyUrl)
            } catch (ex: IOException) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(URL_NOT_FOUND)
            }

        } else {
            sshKey = sshKeyString
        }
        try {

            val credentialRequest = CredentialRequest()
            credentialRequest.name = name
            credentialRequest.description = description
            credentialRequest.cloudPlatform = platform
            credentialRequest.publicKey = sshKey
            credentialRequest.parameters = parameters

            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().publics, platformId, platform)
            }
            credentialRequest.topologyId = platformId
            val id: IdJson
            publicInAccount = if (publicInAccount == null) false else publicInAccount
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().credentialEndpoint().postPublic(credentialRequest)
            } else {
                id = shellContext.cloudbreakClient().credentialEndpoint().postPrivate(credentialRequest)
            }
            shellContext.setCredential(id.id!!.toString())
            createOrSelectTemplateHint()
            return String.format(CREATE_SUCCESS_MESSAGE, id.id, name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    @Throws(Exception::class)
    protected fun createOrSelectTemplateHint() {
        if (shellContext.cloudbreakClient().credentialEndpoint().publics.isEmpty()) {
            shellContext.setHint(Hints.ADD_BLUEPRINT)
        } else {
            shellContext.setHint(Hints.SELECT_BLUEPRINT)
        }
    }

    @Throws(IOException::class)
    protected fun readUrl(url: String): String {
        var url = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url
        }
        val `in` = BufferedReader(InputStreamReader(URL(url).openStream()))
        var str: String
        val sb = StringBuffer()
        while ((str = `in`.readLine()) != null) {
            sb.append(str)
        }
        `in`.close()
        return sb.toString()
    }

    companion object {

        private val FILE_NOT_FOUND = "File not found with ssh key."
        private val URL_NOT_FOUND = "Url not Available for ssh key."
        private val CREATE_SUCCESS_MESSAGE = "Credential created with id: '%d' and name: '%s'"
    }
}
