package com.sequenceiq.cloudbreak.core.flow2.service

import javax.inject.Inject

import org.springframework.stereotype.Service

import reactor.bus.Event

/**
 * Event factory that registers an error handler into the event.
 */
@Service
class ErrorHandlerAwareFlowEventFactory : FlowEventFactory<Any> {

    @Inject
    private val errorHandler: CloudbreakErrorHandler? = null

    override fun createEvent(payLoad: Any, eventKey: String): Event<Any> {
        return Event(null, payLoad, errorHandler)
    }

}
