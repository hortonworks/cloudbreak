package com.sequenceiq.periscope.monitor.evaluator

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher

abstract class AbstractEventPublisher : EventPublisher {

    private var eventPublisher: ApplicationEventPublisher? = null

    override fun publishEvent(event: Any) {
        this.eventPublisher!!.publishEvent(event)
    }

    override fun publishEvent(event: ApplicationEvent) {
        this.eventPublisher!!.publishEvent(event)
    }

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher
    }
}
