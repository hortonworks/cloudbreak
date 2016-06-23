package com.sequenceiq.cloudbreak.service.cluster

import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils.buildVolumePathString
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils.getLogVolume

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.springframework.stereotype.Service

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

@Service
class HadoopConfigurationService {

    @Inject
    private val hostGroupRepository: HostGroupRepository? = null
    private val serviceConfigs = HashMap<String, ServiceConfig>()
    private val bpConfigs = HashMap<String, Map<String, String>>()

    @PostConstruct
    @Throws(IOException::class)
    fun init() {
        val serviceConfigJson = FileReaderUtils.readFileFromClasspath("hdp/service-config.json")
        val services = JsonUtil.readTree(serviceConfigJson).get("services")
        for (service in services) {
            val serviceName = service.get("name").asText()
            val configurations = service.get("configurations")
            val globalConfig = HashMap<String, List<ConfigProperty>>()
            val hostConfig = HashMap<String, List<ConfigProperty>>()
            for (config in configurations) {
                val type = config.get("type").asText()
                val global = toList(config.get("global"))
                if (!global.isEmpty()) {
                    globalConfig.put(type, global)
                }
                val host = toList(config.get("host"))
                if (!host.isEmpty()) {
                    hostConfig.put(type, host)
                }
            }
            serviceConfigs.put(serviceName, ServiceConfig(serviceName, globalConfig, hostConfig))
        }

        val bpConfigJson = FileReaderUtils.readFileFromClasspath("hdp/bp-config.json")
        val bps = JsonUtil.readTree(bpConfigJson).get("sites")
        for (bp in bps) {
            val siteName = bp.get("name").asText()
            val configurations = bp.get("configurations")
            val keyVals = HashMap<String, String>()
            for (config in configurations) {
                val key = config.get("key").asText()
                val value = config.get("value").asText()
                keyVals.put(key, value)
            }
            bpConfigs.put(siteName, keyVals)
        }
    }

    @Throws(IOException::class)
    fun getGlobalConfiguration(cluster: Cluster): Map<String, Map<String, String>> {
        val config = HashMap<String, Map<String, String>>()
        val blueprintNode = JsonUtil.readTree(cluster.blueprint.blueprintText)
        val hostGroups = blueprintNode.path("host_groups")
        for (hostGroup in hostGroups) {
            val components = hostGroup.path("components")
            for (component in components) {
                val name = component.path("name").asText()
                config.putAll(getProperties(name, true, null))
            }
        }
        for (entry in bpConfigs.entries) {
            if (config.containsKey(entry.key)) {
                for (inEntry in entry.value.entries) {
                    config[entry.key].put(inEntry.key, inEntry.value)
                }
            } else {
                config.put(entry.key, entry.value)
            }
        }
        return config
    }

    fun getHostGroupConfiguration(cluster: Cluster): Map<String, Map<String, Map<String, String>>> {
        val hostGroups = hostGroupRepository!!.findHostGroupsInCluster(cluster.id)
        val hadoopConfig = HashMap<String, Map<String, Map<String, String>>>()
        for (hostGroup in hostGroups) {
            if (hostGroup.constraint.instanceGroup != null) {
                val volumeCount = hostGroup.constraint.instanceGroup.template.volumeCount!!
                val componentConfig = HashMap<String, Map<String, String>>()
                for (serviceName in serviceConfigs.keys) {
                    componentConfig.putAll(getProperties(serviceName, false, volumeCount))
                }
                hadoopConfig.put(hostGroup.name, componentConfig)
            }
        }
        return hadoopConfig
    }

    private fun toList(nodes: JsonNode): List<ConfigProperty> {
        val list = ArrayList<ConfigProperty>()
        for (node in nodes) {
            list.add(ConfigProperty(node.get("name").asText(), node.get("directory").asText(), node.get("prefix").asText()))
        }
        return list
    }

    private fun getProperties(name: String, global: Boolean, volumeCount: Int?): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()
        val serviceName = getServiceName(name)
        if (serviceName != null) {
            val serviceConfig = serviceConfigs[serviceName]
            val config = if (global) serviceConfig.globalConfig else serviceConfig.hostGroupConfig
            for (siteConfig in config.keys) {
                val properties = HashMap<String, String>()
                for (property in config[siteConfig]) {
                    val directory = serviceName.toLowerCase() + if (property.directory.isEmpty()) "" else "/" + property.directory
                    val value = if (global) property.prefix + getLogVolume(directory) else buildVolumePathString(volumeCount!!, directory)
                    properties.put(property.name, value)
                }
                result.put(siteConfig, properties)
            }
        }
        return result
    }

    private fun getServiceName(componentName: String): String? {
        for (serviceName in serviceConfigs.keys) {
            if (componentName.toLowerCase().startsWith(serviceName.toLowerCase())) {
                return serviceName
            }
        }
        return null
    }

}
