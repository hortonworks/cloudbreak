package com.sequenceiq.cloudbreak.reactor.api.event

import org.apache.commons.lang3.StringUtils

import com.sequenceiq.cloudbreak.cloud.event.Selectable

open class StackEvent(private val selector: String?, override val stackId: Long?) : Selectable {

    constructor(stackId: Long?) : this(null, stackId) {
    }

    override fun selector(): String {
        return if (StringUtils.isNotEmpty(selector)) selector else EventSelectorUtil.selector(javaClass)
    }
}
