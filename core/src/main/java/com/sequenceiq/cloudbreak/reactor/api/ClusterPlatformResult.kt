package com.sequenceiq.cloudbreak.reactor.api

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil

abstract class ClusterPlatformResult<R : ClusterPlatformRequest> : Payload, Selectable {

    var status: EventStatus? = null
        private set
    var statusReason: String? = null
        private set
    var errorDetails: Exception? = null
        private set
    var request: R? = null
        private set

    constructor(request: R) {
        init(EventStatus.OK, null, null, request)
    }

    constructor(statusReason: String, errorDetails: Exception, request: R) {
        init(EventStatus.FAILED, statusReason, errorDetails, request)
    }

    protected fun init(status: EventStatus, statusReason: String?, errorDetails: Exception?, request: R) {
        this.status = status
        this.statusReason = statusReason
        this.errorDetails = errorDetails
        this.request = request
    }

    override fun selector(): String {
        return if (status === EventStatus.OK) EventSelectorUtil.selector(javaClass) else EventSelectorUtil.failureSelector(javaClass)
    }

    override val stackId: Long?
        get() = request!!.stackId
}
