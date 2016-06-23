package com.sequenceiq.cloudbreak.service.cluster.filter

import java.net.ConnectException
import java.util.ArrayList

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider
import com.sequenceiq.cloudbreak.service.cluster.AmbariConfigurationService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

@Service
class HostFilterService {

    @Inject
    private val hostFilters: List<HostFilter>? = null

    @Inject
    private val configurationService: AmbariConfigurationService? = null

    @Inject
    private val ambariClientProvider: AmbariClientProvider? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Throws(CloudbreakSecuritySetupException::class)
    fun filterHostsForDecommission(cluster: Cluster, hosts: Set<HostMetadata>, hostGroup: String): List<HostMetadata> {
        var filteredList: List<HostMetadata> = ArrayList(hosts)
        try {
            val clientConfig = tlsSecurityService!!.buildTLSClientConfig(cluster.stack.id, cluster.ambariIp)
            val ambariClient = ambariClientProvider!!.getAmbariClient(clientConfig, cluster.stack.gatewayPort, cluster.userName,
                    cluster.password)
            val config = configurationService!!.getConfiguration(ambariClient, hostGroup)
            for (hostFilter in hostFilters!!) {
                try {
                    filteredList = hostFilter.filter(cluster.id!!, config, filteredList)
                } catch (e: HostFilterException) {
                    LOGGER.warn("Filter didn't succeed, moving to next filter", e)
                }

            }
        } catch (e: ConnectException) {
            LOGGER.error("Error retrieving the configuration from Ambari, no host filtering is provided", e)
        }

        return filteredList
    }

    companion object {

        val RM_WS_PATH = "/ws/v1/cluster"
        private val LOGGER = LoggerFactory.getLogger(HostFilterService::class.java)
    }
}
