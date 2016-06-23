package com.sequenceiq.it.spark.salt

import java.util.ArrayList
import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse
import com.sequenceiq.it.spark.ITResponse
import com.sequenceiq.it.util.ServerAddressGenerator

import spark.Request
import spark.Response

open class SaltApiRunPostResponse(private val numberOfServers: Int) : ITResponse() {
    override val objectMapper = ObjectMapper()

    init {
        objectMapper.setVisibility(objectMapper.visibilityChecker.withGetterVisibility(JsonAutoDetect.Visibility.NONE))
    }

    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        val serverAddressGenerator = ServerAddressGenerator(numberOfServers)
        if (request.body().contains("grains.append")) {
            return grainsResponse(serverAddressGenerator)
        }
        if (request.body().contains("grains.remove")) {
            return grainsResponse(serverAddressGenerator)
        }
        if (request.body().contains("network.interface_ip")) {
            return networkInterfaceIp(serverAddressGenerator)
        }
        if (request.body().contains("saltutil.sync_grains")) {
            return saltUtilSyncGrainsResponse(serverAddressGenerator)
        }
        if (request.body().contains("state.highstate")) {
            return stateHighState()
        }
        if (request.body().contains("jobs.active")) {
            return jobsActive()
        }
        if (request.body().contains("jobs.lookup_jid")) {
            return jobsLookupJid()
        }
        if (request.body().contains("state.apply")) {
            return stateApply()
        }
        LOGGER.error("no response for this SALT RUN request: " + request.body())
        throw IllegalStateException("no response for this SALT RUN request: " + request.body())
    }

    protected fun stateApply(): Any {
        return ITResponse.responseFromJsonFile("saltapi/state_apply_response.json")
    }

    protected open fun jobsLookupJid(): Any {
        return ITResponse.responseFromJsonFile("saltapi/lookup_jid_response.json")
    }

    protected fun jobsActive(): Any {
        return ITResponse.responseFromJsonFile("saltapi/runningjobs_response.json")
    }

    protected fun stateHighState(): Any {
        return ITResponse.responseFromJsonFile("saltapi/high_state_response.json")
    }

    @Throws(JsonProcessingException::class)
    protected fun networkInterfaceIp(serverAddressGenerator: ServerAddressGenerator): Any {
        val networkInterfaceResponse = NetworkInterfaceResponse()
        val result = ArrayList<Map<String, String>>()
        serverAddressGenerator.iterateOver { address ->
            val networkHashMap = HashMap<String, String>()
            networkHashMap.put("host-" + address.replace(".", "-"), address)
            result.add(networkHashMap)
        }
        networkInterfaceResponse.setResult(result)
        return objectMapper.writeValueAsString(networkInterfaceResponse)
    }

    @Throws(JsonProcessingException::class)
    protected fun saltUtilSyncGrainsResponse(serverAddressGenerator: ServerAddressGenerator): Any {
        val applyResponse = ApplyResponse()
        val responseList = ArrayList<Map<String, Any>>()

        val hostMap = HashMap<String, Any>()
        serverAddressGenerator.iterateOver { address -> hostMap.put("host-" + address.replace(".", "-"), address) }
        responseList.add(hostMap)

        applyResponse.result = responseList
        return objectMapper.writeValueAsString(applyResponse)
    }

    @Throws(JsonProcessingException::class)
    protected fun grainsResponse(serverAddressGenerator: ServerAddressGenerator): Any {
        val applyResponse = ApplyResponse()
        val responseList = ArrayList<Map<String, Any>>()

        val hostMap = HashMap<String, Any>()
        serverAddressGenerator.iterateOver { address -> hostMap.put("host-" + address.replace(".", "-"), address) }
        responseList.add(hostMap)

        applyResponse.result = responseList
        return objectMapper.writeValueAsString(applyResponse)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SaltApiRunPostResponse::class.java)
    }
}
