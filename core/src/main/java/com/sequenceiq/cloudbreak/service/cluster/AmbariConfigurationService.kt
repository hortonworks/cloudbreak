package com.sequenceiq.cloudbreak.service.cluster

import java.net.ConnectException
import java.util.ArrayList
import java.util.HashMap

import org.springframework.stereotype.Service

import com.sequenceiq.ambari.client.AmbariClient

@Service
class AmbariConfigurationService {

    @Throws(ConnectException::class)
    fun getConfiguration(ambariClient: AmbariClient, hostGroup: String): Map<String, String> {
        val configuration = HashMap<String, String>()
        val serviceConfigs = ambariClient.getServiceConfigMapByHostGroup(hostGroup).entries
        for (serviceEntry in serviceConfigs) {
            for (configEntry in serviceEntry.value.entries) {
                if (CONFIG_LIST.contains(configEntry.key)) {
                    configuration.put(configEntry.key, replaceHostName(ambariClient, configEntry))
                }
            }
        }
        return configuration
    }

    private fun replaceHostName(ambariClient: AmbariClient, entry: Entry<String, String>): String {
        var result = entry.value
        if (entry.key.startsWith("yarn.resourcemanager")) {
            val portStartIndex = result.indexOf(":")
            val internalAddress = result.substring(0, portStartIndex)
            var publicAddress = ambariClient.resolveInternalHostName(internalAddress)
            if (internalAddress == publicAddress) {
                if (internalAddress.contains(AZURE_ADDRESS_SUFFIX)) {
                    publicAddress = internalAddress.substring(0, internalAddress.indexOf(".") + 1) + AZURE_ADDRESS_SUFFIX
                }
            }
            result = publicAddress + result.substring(portStartIndex)
        }
        return result
    }

    companion object {

        private val CONFIG_LIST = ArrayList<String>(ConfigParam.values().size)
        private val AZURE_ADDRESS_SUFFIX = "cloudapp.net"

        init {
            for (param in ConfigParam.values()) {
                CONFIG_LIST.add(param.key())
            }
        }
    }

}
