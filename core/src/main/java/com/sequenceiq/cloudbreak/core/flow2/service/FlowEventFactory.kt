package com.sequenceiq.cloudbreak.core.flow2.service

import reactor.bus.Event

interface FlowEventFactory<T> {
    fun createEvent(payLoad: T, eventKey: String): Event<T>
}
