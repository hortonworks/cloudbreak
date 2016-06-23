package com.sequenceiq.cloudbreak.service.cluster.flow

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component

import com.ecwid.consul.v1.ConsulClient
import com.ecwid.consul.v1.event.model.EventParams
import com.google.common.collect.FluentIterable
import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.PluginFailureException
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

@Component
class ConsulPluginManager : PluginManager {

    @Inject
    private val consulKVCheckerTask: ConsulKVCheckerTask? = null

    @Inject
    private val keyValuePollingService: PollingService<ConsulKVCheckerContext>? = null

    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    override fun prepareKeyValues(clientConfig: HttpClientConfig, keyValues: Map<String, String>) {
        val client = ConsulUtils.createClient(clientConfig)
        for (kv in keyValues.entries) {
            if (!ConsulUtils.putKVValue(Arrays.asList(client), kv.key, kv.value, null)) {
                throw PluginFailureException("Failed to put values in Consul's key-value store.")
            }
        }
    }

    override fun installPlugins(clientConfig: HttpClientConfig, plugins: Map<String, ExecutionType>, hosts: Set<String>,
                                existingHostGroup: Boolean): Map<String, Set<String>> {
        val eventIdMap = HashMap<String, Set<String>>()
        val client = ConsulUtils.createClient(clientConfig)
        for (plugin in plugins.entries) {
            val installedHosts = HashSet<String>()
            if (ExecutionType.ONE_NODE == plugin.value) {
                if (!existingHostGroup) {
                    val installedHost = FluentIterable.from(hosts).first().get()
                    installedHosts.add(installedHost)
                } else {
                    continue
                }
            } else {
                installedHosts.addAll(hosts)
            }

            for (nodeFilter in getNodeFilters(installedHosts).entries) {
                val eventParams = EventParams()
                eventParams.node = nodeFilter.key
                val eventId = ConsulUtils.fireEvent(client, INSTALL_PLUGIN_EVENT, "TRIGGER_PLUGN " + plugin.key + " " + getPluginName(plugin.key),
                        eventParams, null) ?: throw PluginFailureException("Failed to install plugins, Consul client couldn't fire the event or failed to retrieve an event ID." + "Maybe the payload was too long (max. 512 bytes)?")
                eventIdMap.put(eventId, nodeFilter.value)
            }
        }
        return eventIdMap
    }

    override fun cleanupPlugins(clientConfig: HttpClientConfig, hosts: Set<String>): Map<String, Set<String>> {
        val eventIdMap = HashMap<String, Set<String>>()
        val client = ConsulUtils.createClient(clientConfig)
        for (nodeFilter in getNodeFilters(hosts).entries) {
            val eventParams = EventParams()
            eventParams.node = nodeFilter.key
            val eventId = ConsulUtils.fireEvent(client, CLEANUP_PLUGIN_EVENT, "TRIGGER_PLUGN", eventParams, null) ?: throw PluginFailureException("Failed to cleanup plugins, Consul client couldn't fire the event or failed to retrieve an event ID.")
            eventIdMap.put(eventId, nodeFilter.value)
        }
        return eventIdMap
    }

    private fun getNodeFilters(hosts: Set<String>): Map<String, Set<String>> {
        val nodeFilters = HashMap<String, Set<String>>()
        val nodeFilter = StringBuilder("")
        val hostsForNodeFilter = HashSet<String>()
        for (host in hosts) {
            val shortHost = host.replace(ConsulUtils.CONSUL_DOMAIN, "")
            if (nodeFilter.length >= MAX_NODE_FILTER_LENGTH - shortHost.length) {
                nodeFilters.put(nodeFilter.deleteCharAt(nodeFilter.length - 1).toString(), Sets.newHashSet(hostsForNodeFilter))
                nodeFilter.setLength(0)
                hostsForNodeFilter.clear()
            }
            hostsForNodeFilter.add(host)
            nodeFilter.append(shortHost).append("|")
        }
        nodeFilters.put(nodeFilter.deleteCharAt(nodeFilter.length - 1).toString(), Sets.newHashSet(hostsForNodeFilter))
        return nodeFilters
    }

    @Throws(CloudbreakSecuritySetupException::class)
    override fun waitForEventFinish(stack: Stack, instanceMetaData: Collection<InstanceMetaData>, eventIds: Map<String, Set<String>>, timeout: Int?) {
        val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
        val client = ConsulUtils.createClient(clientConfig)
        val keys = generateKeys(eventIds)
        val calculatedMaxAttempt = timeout!! * ONE_THOUSAND * SECONDS_IN_MINUTE / POLLING_INTERVAL
        keyValuePollingService!!.pollWithTimeoutSingleFailure(
                consulKVCheckerTask,
                ConsulKVCheckerContext(stack, client, keys, FINISH_SIGNAL, FAILED_SIGNAL),
                POLLING_INTERVAL, calculatedMaxAttempt)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    override fun triggerAndWaitForPlugins(stack: Stack, event: ConsulPluginEvent, timeout: Int?, container: DockerContainer) {
        triggerAndWaitForPlugins(stack, event, timeout, container, emptyList<String>(), null)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    override fun triggerAndWaitForPlugins(stack: Stack, event: ConsulPluginEvent, timeout: Int?, container: DockerContainer,
                                          payload: List<String>, hosts: Set<String>?) {
        val instances = stack.runningInstanceMetaData
        var targetHosts: Set<String> = hosts
        if (hosts == null || hosts.isEmpty()) {
            targetHosts = getHostnames(hostMetadataRepository!!.findHostsInCluster(stack.cluster.id))
        }
        val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
        val triggerEventIds = triggerPlugins(clientConfig, event, container, payload, targetHosts)
        val eventIdMap = HashMap<String, Set<String>>()
        for (eventId in triggerEventIds.keys) {
            var eventHosts = triggerEventIds[eventId]
            if (eventHosts.isEmpty()) {
                eventHosts = targetHosts
            }
            eventIdMap.put(eventId, eventHosts)
        }
        waitForEventFinish(stack, instances, eventIdMap, timeout)
    }

    private fun triggerPlugins(clientConfig: HttpClientConfig, event: ConsulPluginEvent,
                               container: DockerContainer, payload: List<String>, hosts: Set<String>?): Map<String, Set<String>> {
        val client = ConsulUtils.createClient(clientConfig)
        if (hosts == null || hosts.isEmpty()) {
            return Collections.singletonMap<String, Set<String>>(fireEvent(client, event, container, payload, null), emptySet<String>())
        }
        val result = HashMap<String, Set<String>>()
        for (nodeFilter in getNodeFilters(hosts).entries) {
            val eventParams = EventParams()
            eventParams.node = nodeFilter.key
            result.put(fireEvent(client, event, container, payload, eventParams), nodeFilter.value)
        }
        return result
    }

    private fun fireEvent(client: ConsulClient, event: ConsulPluginEvent, container: DockerContainer, payload: List<String>, eventParams: EventParams?): String {
        val eventId = ConsulUtils.fireEvent(Arrays.asList(client), event.name,
                "TRIGGER_PLUGN_IN_CONTAINER " + container.name + " " + StringUtils.join(payload, " "), eventParams, null) ?: throw PluginFailureException("Failed to trigger plugins, Consul client couldn't fire the "
                + event.name + " event or failed to retrieve an event ID.")
        return eventId
    }

    private fun generateKeys(eventIds: Map<String, Set<String>>): List<String> {
        val keys = ArrayList<String>()
        for (event in eventIds.entries) {
            for (host in event.value) {
                keys.add(String.format("events/%s/%s", event.key, host))
            }
        }
        return keys
    }

    private fun getPluginName(url: String): String {
        val splits = url.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        return splits[splits.size - 1].replace("consul-plugins-", "").replace(".git", "")
    }

    private fun getHostnames(hostMetadata: Set<HostMetadata>): Set<String> {
        return hostMetadata.stream().map(Function<HostMetadata, String> { it.getHostName() }).collect(Collectors.toSet<String>())
    }

    companion object {

        val INSTALL_PLUGIN_EVENT = "install-plugin"
        val CLEANUP_PLUGIN_EVENT = "cleanup-plugin"
        val FINISH_SIGNAL = "FINISHED"
        val FAILED_SIGNAL = "FAILED"
        val POLLING_INTERVAL = 5000
        val MAX_NODE_FILTER_LENGTH = 300
        val ONE_THOUSAND = 1000
        val SECONDS_IN_MINUTE = 60
    }
}
