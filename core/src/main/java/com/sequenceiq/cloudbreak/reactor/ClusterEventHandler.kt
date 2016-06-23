package com.sequenceiq.cloudbreak.reactor

import reactor.bus.Event
import reactor.fn.Consumer

interface ClusterEventHandler<T> : Consumer<Event<T>> {
    fun type(): Class<T>
}
