package com.sequenceiq.cloudbreak.core.flow2.stack

import java.util.Arrays

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService

@Service
class FlowMessageService {

    @Inject
    private val messagesService: CloudbreakMessagesService? = null
    @Inject
    private val cloudbreakEventService: CloudbreakEventService? = null

    fun fireEventAndLog(stackId: Long?, msgCode: Msg, eventType: String, vararg args: Any) {
        LOGGER.debug("{} [STACK_FLOW_STEP].", msgCode)
        val message = messagesService!!.getMessage(msgCode.code(), Arrays.asList(*args))
        cloudbreakEventService!!.fireCloudbreakEvent(stackId, eventType, message)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FlowMessageService::class.java)
    }
}
