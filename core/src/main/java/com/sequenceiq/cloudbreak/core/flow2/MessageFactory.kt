package com.sequenceiq.cloudbreak.core.flow2

import java.util.HashMap

import org.springframework.messaging.Message
import org.springframework.messaging.support.GenericMessage

class MessageFactory<E> {

    enum class HEADERS {
        FLOW_ID, DATA
    }

    fun createMessage(flowId: String, key: E, data: Any): Message<E> {
        val headers = HashMap<String, Any>()
        headers.put(HEADERS.FLOW_ID.name, flowId)
        headers.put(HEADERS.DATA.name, data)
        return GenericMessage(key, headers)
    }
}
