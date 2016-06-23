package com.sequenceiq.cloudbreak.reactor.api.event

open class StackFailureEvent : StackEvent {
    var exception: Exception? = null
        private set

    constructor(stackId: Long?, exception: Exception) : super(stackId) {
        this.exception = exception
    }

    constructor(selector: String, stackId: Long?, exception: Exception) : super(selector, stackId) {
        this.exception = exception
    }
}
