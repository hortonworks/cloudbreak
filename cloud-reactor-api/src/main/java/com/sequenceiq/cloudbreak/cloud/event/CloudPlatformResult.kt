package com.sequenceiq.cloudbreak.cloud.event

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus

open class CloudPlatformResult<R : CloudPlatformRequest<Any>> : Payload {

    var status: EventStatus? = null
        private set
    var statusReason: String? = null
        private set
    var errorDetails: Exception? = null
        private set
    var request: R? = null
        private set

    protected constructor() {
    }

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

    fun selector(): String {
        return if (status == EventStatus.OK) selector(javaClass) else failureSelector(javaClass)
    }

    override fun toString(): String {
        return "CloudPlatformResult{"
        +"status=" + status
        +", statusReason='" + statusReason + '\''
        +", errorDetails=" + errorDetails
        +", request=" + request
        +'}'
    }

    override val stackId: Long?
        get() = request!!.cloudContext.id

    companion object {

        fun selector(clazz: Class<Any>): String {
            return clazz.simpleName.toUpperCase()
        }

        fun failureSelector(clazz: Class<Any>): String {
            return clazz.simpleName.toUpperCase() + "_ERROR"
        }
    }
}
