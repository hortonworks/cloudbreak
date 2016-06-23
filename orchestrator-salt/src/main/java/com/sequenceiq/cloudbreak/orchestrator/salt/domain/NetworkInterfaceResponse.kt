package com.sequenceiq.cloudbreak.orchestrator.salt.domain

import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class NetworkInterfaceResponse {

    @JsonProperty("return")
    private var result: List<Map<String, String>>? = null

    val resultGroupByHost: Map<String, String>
        get() {
            val res = HashMap<String, String>()
            result!!.stream().forEach({ map -> map.forEach(BiConsumer<String, String> { key, value -> res.put(key, value) }) })
            return res
        }

    val resultGroupByIP: Map<String, String>
        get() {
            val res = HashMap<String, String>()
            result!!.stream().forEach({ map -> map.forEach({ k, v -> res.put(v, k) }) })
            return res
        }

    fun setResult(result: List<Map<String, String>>) {
        this.result = result
    }

    override fun toString(): String {
        val sb = StringBuilder("NetworkInterfaceResponse{")
        sb.append("result=").append(result)
        sb.append('}')
        return sb.toString()
    }
}
