package com.sequenceiq.cloudbreak.service.cluster.filter

import java.util.ArrayList

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam

@Component
class NameNodeFilter : HostFilter {

    @Throws(HostFilterException::class)
    override fun filter(clusterId: Long, config: Map<String, String>, hosts: List<HostMetadata>): List<HostMetadata> {
        val result = ArrayList(hosts)
        try {
            val nameNode = config[ConfigParam.NAMENODE_HTTP_ADDRESS.key()]
            val secondaryNameNode = config[ConfigParam.SECONDARY_NAMENODE_HTTP_ADDRESS.key()]
            val nameNodeHost = nameNode.substring(0, nameNode.lastIndexOf(':'))
            val secondaryNameNodeHost = secondaryNameNode.substring(0, secondaryNameNode.lastIndexOf(':'))
            val iterator = result.iterator()
            while (iterator.hasNext()) {
                val hostName = iterator.next().hostName
                if (hostName == nameNodeHost || hostName == secondaryNameNodeHost) {
                    iterator.remove()
                }
            }
        } catch (e: Exception) {
            throw HostFilterException("Cannot check the address of the NN and SNN", e)
        }

        return result
    }

}
