package com.sequenceiq.cloudbreak.cloud.init

import reactor.bus.selector.Selectors.`$`

import java.util.HashMap

import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler

import reactor.bus.EventBus

@Component
class CloudPlatformInitializer {

    @Resource
    private val handlers: List<CloudPlatformEventHandler<Any>>? = null

    @Inject
    private val eventBus: EventBus? = null

    @PostConstruct
    @Throws(Exception::class)
    fun init() {
        validateSelectors()
        LOGGER.info("Registering CloudPlatformEventHandlers")
        for (handler in handlers!!) {
            val selector = CloudPlatformRequest.selector(handler.type())
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.javaClass, selector)
            eventBus!!.on(`$`<String>(selector), handler)
        }
    }

    private fun validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers!!.size)
        val handlerMap = HashMap<Class<Any>, CloudPlatformEventHandler<Any>>()
        for (handler in handlers) {
            val entry = handlerMap.put(handler.type(), handler)
            if (null != entry) {
                LOGGER.error("Duplicate handlers! actual: {}, existing: {}", handler, entry)
                throw IllegalStateException("Duplicate handlers! first: $handler second: $entry")
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudPlatformInitializer::class.java)
    }
}
