package com.sequenceiq.periscope.monitor.event

import org.springframework.context.ApplicationEvent

import com.sequenceiq.periscope.domain.BaseAlert

class ScalingEvent(alert: BaseAlert) : ApplicationEvent(alert) {

    val alert: BaseAlert
        get() = getSource() as BaseAlert

}
