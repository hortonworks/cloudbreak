package com.sequenceiq.cloudbreak.shell.transformer

import javax.ws.rs.ClientErrorException

import org.springframework.shell.support.util.StringUtils
import org.springframework.stereotype.Component

@Component
class ExceptionTransformer {

    fun transformToRuntimeException(e: Exception): RuntimeException {
        if (e is ClientErrorException) {
            if (e.response != null && e.response.hasEntity()) {
                val response = e.response.readEntity<String>(String::class.java)
                if (response != null) {
                    val split = response!!.replace("}".toRegex(), "").replace("\\{".toRegex(), "").split("\"".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    val splitResponse = split[split.size - 1]
                    if (StringUtils.isEmpty(response)) {
                        return RuntimeException(response)
                    } else {
                        return RuntimeException(splitResponse)
                    }
                }
            }
        }
        return RuntimeException(e.message)
    }

    fun transformToRuntimeException(errorMessage: String): RuntimeException {
        return RuntimeException(errorMessage)
    }
}
