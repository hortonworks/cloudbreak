package com.sequenceiq.cloudbreak.cloud.model

class CloudVmInstanceStatus @JvmOverloads constructor(val cloudInstance: CloudInstance, val status: InstanceStatus, val statusReason: String? = null) {

    override fun toString(): String {
        return "CloudVmInstanceStatus{"
        +"instance=" + cloudInstance
        +", status=" + status
        +", statusReason='" + statusReason + '\''
        +'}'
    }
}
