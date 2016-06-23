package com.sequenceiq.cloudbreak.reactor.init

import reactor.bus.selector.Selectors.`$`

import java.util.HashMap

import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler

import reactor.bus.EventBus

@Component
class ReactorEventHandlerInitializer {

    @Resource
    private val handlers: List<ReactorEventHandler<Any>>? = null

    @Inject
    private val eventBus: EventBus? = null

    @PostConstruct
    @Throws(Exception::class)
    fun init() {
        validateSelectors()
        LOGGER.info("Registering ReactorEventHandlers")
        for (handler in handlers!!) {
            val selector = handler.selector()
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.javaClass, selector)
            eventBus!!.on(`$`<String>(selector), handler)
        }
    }

    private fun validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers!!.size)
        val handlerMap = HashMap<String, ReactorEventHandler<Any>>()
        for (handler in handlers) {
            val entry = handlerMap.put(handler.selector(), handler)
            if (null != entry) {
                LOGGER.error("Duplicated handlers! actual: {}, existing: {}", handler, entry)
                throw IllegalStateException("Duplicate handlers! first: $handler second: $entry")
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ReactorEventHandlerInitializer::class.java)
    }
}
