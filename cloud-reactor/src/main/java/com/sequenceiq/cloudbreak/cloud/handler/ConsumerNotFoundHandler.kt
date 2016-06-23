package com.sequenceiq.cloudbreak.cloud.handler

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import reactor.fn.Consumer

class ConsumerNotFoundHandler : Consumer<Any> {

    override fun accept(event: Any) {
        LOGGER.error("Event not delivered! There is no registered consumer for the key: [ \"{}\" ]", event)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ConsumerNotFoundHandler::class.java)
    }
}
