package com.sequenceiq.cloudbreak.cloud.reactor.config

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification

import reactor.bus.Event
import reactor.bus.EventBus
import reactor.bus.selector.Selectors
import reactor.fn.Consumer

@Component
class CloudReactorInitializer {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val resourcePersistenceHandler: Consumer<Event<ResourceNotification>>? = null

    @PostConstruct
    fun initialize() {
        eventBus!!.on(Selectors.`$`<String>("resource-persisted"), resourcePersistenceHandler)
    }

}
