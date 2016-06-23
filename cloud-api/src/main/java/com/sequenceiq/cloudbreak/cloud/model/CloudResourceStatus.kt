package com.sequenceiq.cloudbreak.cloud.model

class CloudResourceStatus @JvmOverloads constructor(val cloudResource: CloudResource, var status: ResourceStatus?, val statusReason: String = null, var privateId: Long? = null) {

    val isFailed: Boolean
        get() = ResourceStatus.FAILED == status

    val isDeleted: Boolean
        get() = ResourceStatus.DELETED == status

    override fun toString(): String {
        val sb = StringBuilder("CloudResourceStatus{")
        sb.append("cloudResource=").append(cloudResource)
        sb.append(", status=").append(status)
        sb.append(", statusReason='").append(statusReason).append('\'')
        sb.append(", id=").append(privateId)
        sb.append('}')
        return sb.toString()
    }
}
