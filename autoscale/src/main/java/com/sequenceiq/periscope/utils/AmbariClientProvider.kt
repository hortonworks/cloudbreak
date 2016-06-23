package com.sequenceiq.periscope.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.model.TlsConfiguration
import com.sequenceiq.periscope.service.security.TlsSecurityService

@Service
class AmbariClientProvider {

    @Autowired
    private val tlsSecurityService: TlsSecurityService? = null

    fun createAmbariClient(cluster: Cluster): AmbariClient {
        if (cluster.stackId != null) {
            val tlsConfig = tlsSecurityService!!.getConfiguration(cluster)
            return AmbariClient(cluster.host,
                    cluster.port,
                    cluster.ambariUser,
                    cluster.ambariPass,
                    tlsConfig.clientCertPath,
                    tlsConfig.clientKeyPath,
                    tlsConfig.serverCertPath)
        } else {
            return AmbariClient(cluster.host, cluster.port, cluster.ambariUser, cluster.ambariPass)
        }
    }
}
