package com.sequenceiq.cloudbreak.shell.commands.base

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

import org.apache.commons.io.IOUtils
import org.apache.http.MethodNotSupportedException
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.TopologyRequest
import com.sequenceiq.cloudbreak.api.model.TopologyResponse
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext


class BasePlatformCommands(private val shellContext: ShellContext) : BaseCommands, PlatformCommands {

    @CliAvailabilityIndicator(value = "platform list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "platform list", help = "Shows the currently available platform")
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().topologyEndpoint().publics
            val map = shellContext.responseTransformer().transformToMap(publics, "id", "name")
            return shellContext.outputTransformer().render<Map<String, String>>(map, "ID", "INFO")
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliAvailabilityIndicator(value = "platform show")
    override fun showAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    override fun show(id: Long?, name: String?): String {
        try {
            if (id != null) {
                val topologyResponse = shellContext.cloudbreakClient().topologyEndpoint()[id]
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(topologyResponse), "FIELD", "VALUE")
            } else if (name != null) {
                val topologyResponse = selectByName(shellContext.cloudbreakClient().topologyEndpoint().publics, name)
                if (topologyResponse != null) {
                    return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(topologyResponse), "FIELD", "VALUE")
                }
            }
            return "No platform specified."
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliCommand(value = "platform show --id", help = "Show the platform by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "platform show --name", help = "Show the platform by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    override fun selectAvailable(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun select(id: Long?, name: String?): String {
        throw MethodNotSupportedException("Platform select command not available.")
    }

    @Throws(Exception::class)
    override fun selectById(id: Long?): String {
        return select(id, null)
    }

    @Throws(Exception::class)
    override fun selectByName(name: String): String {
        return select(null, name)
    }

    @CliAvailabilityIndicator(value = "platform delete")
    override fun deleteAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    override fun delete(id: Long?, name: String?): String {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().topologyEndpoint().delete(java.lang.Long.valueOf(id), false)
                return String.format("Platform has been deleted, id: %s", id)
            } else if (name != null) {
                val idForName = getIdForName(shellContext.cloudbreakClient().topologyEndpoint().publics, name)
                if (idForName != null) {
                    shellContext.cloudbreakClient().topologyEndpoint().delete(idForName, false)
                    return String.format("Platform has been deleted, name: %s", name)
                }
            }
            return "No platform specified."
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    @CliCommand(value = "platform delete --id", help = "Delete the platform by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "platform delete --name", help = "Delete the platform by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    override fun createPlatformAvailable(platform: String): Boolean {
        return true
    }

    override fun create(name: String, description: String, cloudPlatform: String, mapping: Map<String, String>): String {
        try {
            val req = TopologyRequest()
            req.cloudPlatform = cloudPlatform
            req.name = name
            req.description = description
            req.nodes = mapping
            val id = shellContext.cloudbreakClient().topologyEndpoint().postPublic(req)
            shellContext.setHint(Hints.CREATE_CREDENTIAL_WITH_TOPOLOGY)
            return String.format(CREATE_SUCCESS_MSG, id.id, name)
        } catch (e: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    override fun convertMappingFile(file: File?, url: String?): Map<String, String> {
        val result = Maps.newHashMap<String, String>()
        if (file != null || url != null) {
            var bf: BufferedReader? = null
            try {
                bf = getReader(file, url)
                var line: String
                while ((line = bf.readLine()) != null) {
                    val mapping = line.split("\\s+".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (mapping.size != 2) {
                        continue
                    }
                    result.put(mapping[0], mapping[1])
                }
            } catch (e: IOException) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(e)
            } finally {
                IOUtils.closeQuietly(bf)
            }
        }
        return result
    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    private fun getReader(file: File?, url: String): BufferedReader {
        try {
            if (file != null) {
                return IOUtils.toBufferedReader(FileReader(file))
            }
            return IOUtils.toBufferedReader(InputStreamReader(URL(url).openStream()))
        } catch (e: IOException) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e)
        }

    }

    private fun getIdForName(publics: Set<TopologyResponse>, name: String): Long? {
        val t = selectByName(publics, name)
        if (t != null) {
            return t.id
        }
        return null
    }

    private fun selectByName(publics: Set<TopologyResponse>?, name: String): TopologyResponse? {
        if (publics != null) {
            for (res in publics) {
                if (res.name == name) {
                    return res
                }
            }
        }
        return null
    }

    companion object {
        private val CREATE_SUCCESS_MSG = "Platform created with id: '%d' and name: '%s'"
    }


}
