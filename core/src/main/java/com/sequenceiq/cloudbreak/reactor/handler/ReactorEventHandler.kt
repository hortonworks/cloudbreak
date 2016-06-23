package com.sequenceiq.cloudbreak.reactor.handler

import reactor.bus.Event
import reactor.fn.Consumer

interface ReactorEventHandler<T> : Consumer<Event<T>> {
    fun selector(): String
}
