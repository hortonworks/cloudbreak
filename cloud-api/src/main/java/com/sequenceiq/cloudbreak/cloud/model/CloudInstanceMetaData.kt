package com.sequenceiq.cloudbreak.cloud.model

class CloudInstanceMetaData(val privateIp: String, val publicIp: String, val sshPort: Int, val hypervisor: String) {

    @JvmOverloads constructor(privateIp: String, publicIp: String, hypervisor: String? = null) : this(privateIp, publicIp, DEFAULT_SSH_PORT, hypervisor) {
    }

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "InstanceMetaData{, privateIp='$privateIp\', publicIp='$publicIp\', hypervisor='$hypervisor\'}"
    }

    companion object {

        val EMPTY_METADATA = CloudInstanceMetaData(null, null, null)
        private val DEFAULT_SSH_PORT = 22
    }
    //END GENERATED CODE
}
