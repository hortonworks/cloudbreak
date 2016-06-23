package com.sequenceiq.cloudbreak.reactor.init

import reactor.bus.selector.Selectors.`$`

import java.util.HashMap

import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil

import reactor.bus.EventBus

@Component
class ClusterPlatformInitializer {

    @Resource
    private val handlers: List<ClusterEventHandler<Any>>? = null

    @Inject
    private val eventBus: EventBus? = null

    @PostConstruct
    @Throws(Exception::class)
    fun init() {
        validateSelectors()
        LOGGER.info("Registering ClusterEventHandler")
        for (handler in handlers!!) {
            val selector = EventSelectorUtil.selector(handler.type())
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.javaClass, selector)
            eventBus!!.on(`$`<String>(selector), handler)
        }
    }

    private fun validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers!!.size)
        val handlerMap = HashMap<Class<Any>, ClusterEventHandler<Any>>()
        for (handler in handlers) {
            val entry = handlerMap.put(handler.type(), handler)
            if (null != entry) {
                LOGGER.error("Duplicated handlers! actual: {}, existing: {}", handler, entry)
                throw IllegalStateException("Duplicate handlers! first: $handler second: $entry")
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ClusterPlatformInitializer::class.java)
    }
}
