package com.sequenceiq.cloudbreak.service.cluster.filter

import java.util.ArrayList
import java.util.HashSet

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType

import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam
import com.sequenceiq.cloudbreak.util.JsonUtil

@Component
class AppMasterFilter : HostFilter {

    @Inject
    private val restClient: Client? = null

    @SuppressWarnings("unchecked")
    @Throws(HostFilterException::class)
    override fun filter(clusterId: Long, config: Map<String, String>, hosts: List<HostMetadata>): List<HostMetadata> {
        var result: List<HostMetadata> = ArrayList(hosts)
        try {
            val resourceManagerAddress = config[ConfigParam.YARN_RM_WEB_ADDRESS.key()]
            val target = restClient!!.target("http://" + resourceManagerAddress + HostFilterService.RM_WS_PATH).path("apps").queryParam("state", "RUNNING")
            val appResponse = target.request(MediaType.APPLICATION_JSON).get<String>(String::class.java)
            val jsonNode = JsonUtil.readTree(appResponse)
            val apps = jsonNode.get(APPS_NODE)
            if (apps != null && apps.has(APP_NODE)) {
                val app = apps.get(APP_NODE)
                val hostsWithAM = HashSet<String>()
                for (node in app) {
                    val hostName = node.get(AM_KEY).textValue()
                    hostsWithAM.add(hostName.substring(0, hostName.lastIndexOf(':')))
                }
                result = filter(hostsWithAM, result)
            }
        } catch (e: Exception) {
            throw HostFilterException("Error filtering based on ApplicationMaster location", e)
        }

        return result
    }

    private fun filter(hostsWithAM: Set<String>, hosts: List<HostMetadata>): List<HostMetadata> {
        val iterator = hosts.iterator()
        while (iterator.hasNext()) {
            val host = iterator.next()
            if (hostsWithAM.contains(host.hostName)) {
                iterator.remove()
            }
        }
        return hosts
    }

    companion object {

        private val AM_KEY = "amHostHttpAddress"
        private val APPS_NODE = "apps"
        private val APP_NODE = "app"
    }

}
