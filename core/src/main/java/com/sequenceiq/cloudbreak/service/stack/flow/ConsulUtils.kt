package com.sequenceiq.cloudbreak.service.stack.flow

import java.util.Collections
import java.util.HashMap

import org.apache.commons.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ecwid.consul.v1.ConsulClient
import com.ecwid.consul.v1.OperationException
import com.ecwid.consul.v1.QueryParams
import com.ecwid.consul.v1.agent.model.Member
import com.ecwid.consul.v1.catalog.model.CatalogService
import com.ecwid.consul.v1.event.model.Event
import com.ecwid.consul.v1.event.model.EventParams
import com.ecwid.consul.v1.kv.model.GetValue
import com.ecwid.consul.v1.kv.model.PutParams

class ConsulUtils private constructor() {

    init {
        throw IllegalStateException()
    }

    enum class ConsulServers private constructor(val min: Int, val max: Int, val consulServerCount: Int) {
        SINGLE_NODE_COUNT_LOW(1, 2, 1),
        NODE_COUNT_LOW(3, 1000, 3),
        NODE_COUNT_MEDIUM(1001, 5000, 5),
        NODE_COUNT_HIGH(5001, 100000, 7)
    }

    companion object {

        val CONSUL_DOMAIN = ".node.dc1.consul"
        private val LOGGER = LoggerFactory.getLogger(ConsulUtils::class.java)

        private val DEFAULT_TIMEOUT_MS = 5000
        private val ALIVE_STATUS = 1
        private val LEFT_STATUS = 3

        fun getService(clients: List<ConsulClient>, serviceName: String): List<CatalogService> {
            for (consul in clients) {
                val service = getService(consul, serviceName)
                if (!service.isEmpty()) {
                    return service
                }
            }
            return emptyList<CatalogService>()
        }

        fun getService(client: ConsulClient, serviceName: String): List<CatalogService> {
            try {
                return client.getCatalogService(serviceName, QueryParams.DEFAULT).value
            } catch (e: Exception) {
                return emptyList<CatalogService>()
            }

        }

        fun getAliveMembers(clients: List<ConsulClient>): Map<String, String> {
            return getMembers(clients, ALIVE_STATUS)
        }

        fun getLeftMembers(clients: List<ConsulClient>): Map<String, String> {
            return getMembers(clients, LEFT_STATUS)
        }

        fun getMembers(clients: List<ConsulClient>, status: Int): Map<String, String> {
            for (client in clients) {
                val members = getMembers(client, status)
                if (!members.isEmpty()) {
                    return members
                }
            }
            return emptyMap<String, String>()
        }

        fun getMembers(client: ConsulClient, status: Int): Map<String, String> {
            try {
                val result = HashMap<String, String>()
                val members = client.agentMembers.value
                for (member in members) {
                    if (member.status == status) {
                        result.put(member.address, member.name)
                    }
                }
                return result
            } catch (e: Exception) {
                return emptyMap<String, String>()
            }

        }

        fun fireEvent(clients: List<ConsulClient>, event: String, payload: String, eventParams: EventParams, queryParams: QueryParams): String? {
            for (client in clients) {
                val eventId = fireEvent(client, event, payload, eventParams, queryParams)
                if (eventId != null) {
                    return eventId
                }
            }
            return null
        }

        fun fireEvent(client: ConsulClient, event: String, payload: String, eventParams: EventParams, queryParams: QueryParams): String? {
            try {
                val response = client.eventFire(event, payload, eventParams, queryParams).value
                return response.id
            } catch (e: OperationException) {
                LOGGER.info("Failed to fire Consul event '{}'. Status code: {}, Message: {}", event, e.statusCode, e.statusMessage)
                return null
            } catch (e: Exception) {
                LOGGER.info("Failed to fire Consul event '{}'. Message: {}", event, e.message)
                return null
            }

        }

        fun getKVValue(clients: List<ConsulClient>, key: String, queryParams: QueryParams): String? {
            for (client in clients) {
                val value = getKVValue(client, key, queryParams)
                if (value != null) {
                    return value
                }
            }
            return null
        }

        fun getKVValue(client: ConsulClient, key: String, queryParams: QueryParams): String? {
            try {
                val getValue = client.getKVValue(key, queryParams).value
                return if (getValue == null) null else String(Base64.decodeBase64(getValue.value))
            } catch (e: OperationException) {
                LOGGER.info("Failed to get entry '{}' from Consul's key-value store. Status code: {}, Message: {}", key, e.statusCode, e.statusMessage)
                return null
            } catch (e: Exception) {
                LOGGER.info("Failed to get entry '{}' from Consul's key-value store. Error message: {}", key, e.message)
                return null
            }

        }

        fun putKVValue(clients: List<ConsulClient>, key: String, value: String, putParams: PutParams): Boolean? {
            for (client in clients) {
                val result = putKVValue(client, key, value, putParams)
                if (result!!) {
                    return result
                }
            }
            return false
        }

        fun putKVValue(client: ConsulClient, key: String, value: String, putParams: PutParams): Boolean? {
            try {
                return client.setKVValue(key, value, putParams).value
            } catch (e: OperationException) {
                LOGGER.info("Failed to put entry '{}' in Consul's key-value store. Status code: {}, Message: {}", key, e.statusCode, e.statusMessage)
                return false
            } catch (e: Exception) {
                LOGGER.info("Failed to put entry '{}' in Consul's key-value store. Error message: {}", key, e.message)
                return false
            }

        }

        @JvmOverloads fun createClient(httpClientConfig: HttpClientConfig, timeout: Int = DEFAULT_TIMEOUT_MS): ConsulClient {
            return ConsulClient("https://" + httpClientConfig.apiAddress, httpClientConfig.apiPort!!,
                    httpClientConfig.clientCert,
                    httpClientConfig.clientKey,
                    httpClientConfig.serverCert,
                    timeout)
        }

        fun agentForceLeave(clients: List<ConsulClient>, nodeName: String) {
            for (client in clients) {
                try {
                    client.agentForceLeave(nodeName)
                } catch (e: Exception) {
                    return
                }

            }
        }

        fun getConsulServerCount(nodeCount: Int): Int {
            if (nodeCount < ConsulServers.SINGLE_NODE_COUNT_LOW.max) {
                return ConsulServers.SINGLE_NODE_COUNT_LOW.consulServerCount
            } else if (nodeCount < ConsulServers.NODE_COUNT_LOW.max) {
                return ConsulServers.NODE_COUNT_LOW.consulServerCount
            } else if (nodeCount < ConsulServers.NODE_COUNT_MEDIUM.max) {
                return ConsulServers.NODE_COUNT_MEDIUM.consulServerCount
            } else {
                return ConsulServers.NODE_COUNT_HIGH.consulServerCount
            }
        }
    }
}
