package com.sequenceiq.cloudbreak.cloud.handler

import reactor.bus.Event
import reactor.fn.Consumer

interface CloudPlatformEventHandler<T> : Consumer<Event<T>> {

    fun type(): Class<T>

}
