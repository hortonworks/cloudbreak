package com.sequenceiq.periscope.monitor.event

import org.springframework.context.ApplicationEvent

class UpdateFailedEvent(clusterId: Long) : ApplicationEvent(clusterId) {

    val clusterId: Long
        get() = source as Long

}