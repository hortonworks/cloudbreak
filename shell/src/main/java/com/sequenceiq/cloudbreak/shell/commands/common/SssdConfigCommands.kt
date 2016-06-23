package com.sequenceiq.cloudbreak.shell.commands.common

import java.io.File
import java.io.FileInputStream
import java.util.HashMap

import org.apache.commons.io.IOUtils
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.completion.SssdProviderType
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType
import com.sequenceiq.cloudbreak.shell.completion.SssdTlsReqcertType
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class SssdConfigCommands(private val shellContext: ShellContext) : BaseCommands {

    @CliAvailabilityIndicator(value = "sssdconfig list")
    override fun listAvailable(): Boolean {
        return true
    }

    @CliCommand(value = "sssdconfig list", help = "Shows the currently available configs")
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().sssdConfigEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("sssdconfig select --id", "sssdconfig select --name"))
    override fun selectAvailable(): Boolean {
        return shellContext.isSssdConfigAccessible
    }

    @CliCommand(value = "sssdconfig select --id", help = "Delete the config by its id")
    @Throws(Exception::class)
    override fun selectById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return select(id, null)
    }

    @CliCommand(value = "sssdconfig select --name", help = "Delete the config by its name")
    @Throws(Exception::class)
    override fun selectByName(@CliOption(key = "", mandatory = true) name: String): String {
        return select(null, name)
    }

    override fun select(id: Long?, name: String?): String {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().sssdConfigEndpoint()[id] != null) {
                    shellContext.addSssdConfig(id.toString())
                    return String.format("SSSD config has been selected, id: %s", id)
                }
            } else if (name != null) {
                val config = shellContext.cloudbreakClient().sssdConfigEndpoint().getPublic(name)
                if (config != null) {
                    shellContext.addSssdConfig(config.id!!.toString())
                    return String.format("SSSD config has been selected, name: %s", name)
                }
            }
            return "No SSSD config specified (select a config by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("sssdconfig create --withParameters", "sssdconfig create --withFile"))
    fun createAvailable(): Boolean {
        return true
    }

    @CliCommand(value = "sssdconfig create --withParameters", help = "Add a new config")
    fun create(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") name: String,
            @CliOption(key = "description", help = "Description of the config") description: String,
            @CliOption(key = "providerType", mandatory = true, help = "Type of the provider") providerType: SssdProviderType,
            @CliOption(key = "url", mandatory = true, help = "comma-separated list of URIs of the LDAP servers") url: String,
            @CliOption(key = "schema", mandatory = true, help = "Schema of the database") schema: SssdSchemaType,
            @CliOption(key = "baseSearch", mandatory = true, help = "Search base of the database") baseSearch: String,
            @CliOption(key = "tlsReqcert", mandatory = true, unspecifiedDefaultValue = "hard", specifiedDefaultValue = "hard", help = "TLS behavior of connection")
            tlsReqcert: SssdTlsReqcertType,
            @CliOption(key = "adServer", mandatory = false, help = "comma-separated list of IP addresses or hostnames of the AD servers") adServer: String,
            @CliOption(key = "kerberosServer", mandatory = false, help = "comma-separated list of IP addresses or hostnames of the Kerberos servers")
            kerberosServer: String,
            @CliOption(key = "kerberosRealm", mandatory = false, help = "name of the Kerberos realm") kerberosRealm: String,
            @CliOption(key = "publicInAccount", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "flags if the config is public in the account")
            publicInAccount: Boolean?): String {
        try {
            val request = SssdConfigRequest()
            request.name = name
            request.description = description
            request.providerType = com.sequenceiq.cloudbreak.api.model.SssdProviderType.valueOf(providerType.name)
            request.url = url
            request.schema = com.sequenceiq.cloudbreak.api.model.SssdSchemaType.valueOf(schema.name)
            request.baseSearch = baseSearch
            request.tlsReqcert = com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType.valueOf(tlsReqcert.name)
            request.adServer = adServer
            request.kerberosServer = kerberosServer
            request.kerberosRealm = kerberosRealm
            val id: IdJson
            if (publicInAccount!!) {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPublic(request)
            } else {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPrivate(request)
            }
            return String.format("SSSD config created with id: '%d' and name: '%s'", id.id, request.name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "sssdconfig create --withFile", help = "Upload a new config")
    fun upload(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") name: String,
            @CliOption(key = "description", help = "Description of the config") description: String,
            @CliOption(key = "file", mandatory = true, help = "Path of the configuration file") configFile: File?,
            @CliOption(key = "publicInAccount", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "flags if the config is public in the account")
            publicInAccount: Boolean?): String {
        try {
            if (configFile != null && !configFile.exists()) {
                return "Configuration file not exists."
            }
            val request = SssdConfigRequest()
            request.name = name
            request.description = description
            val config = IOUtils.toString(FileInputStream(configFile))
            request.configuration = config
            val id: IdJson
            if (publicInAccount!!) {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPublic(request)
            } else {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPrivate(request)
            }
            return String.format("SSSD config created with id: '%d' and name: '%s'", id.id, request.name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("sssdconfig show --id", "sssdconfig show --name"))
    override fun showAvailable(): Boolean {
        return true
    }

    override fun show(id: Long?, name: String?): String {
        try {
            val response: SssdConfigResponse
            if (id != null) {
                response = shellContext.cloudbreakClient().sssdConfigEndpoint()[id]
            } else if (name != null) {
                response = shellContext.cloudbreakClient().sssdConfigEndpoint().getPublic(name)
            } else {
                return "SSSD config not specified."
            }
            val map = HashMap<String, String>()
            map.put("id", response.id!!.toString())
            map.put("name", response.name)
            map.put("description", response.description)
            return shellContext.outputTransformer().render<Map<String, String>>(map, "FIELD", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }


    @CliCommand(value = "sssdconfig show --id", help = "Show the config by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "sssdconfig show --name", help = "Show the config by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("sssdconfig delete --id", "sssdconfig delete --name"))
    override fun deleteAvailable(): Boolean {
        return true
    }

    override fun delete(id: Long?, name: String?): String {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().sssdConfigEndpoint().delete(id)
                return String.format("SSSD config deleted with %s id", id)
            } else if (name != null) {
                shellContext.cloudbreakClient().sssdConfigEndpoint().deletePublic(name)
                return String.format("SSSD config deleted with %s name", name)
            }
            return "SSSD config not specified (select sssd by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "sssdconfig delete --id", help = "Delete the config by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "sssdconfig delete --name", help = "Delete the config by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

}
