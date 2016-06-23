package com.sequenceiq.cloudbreak.cloud.event.setup

import com.sequenceiq.cloudbreak.cloud.context.CloudContext

class SshUserResponse<T>(val cloudContext: CloudContext, val user: String) {

    override fun toString(): String {
        val sb = StringBuilder("SshUserResponse{")
        sb.append("cloudContext=").append(cloudContext)
        sb.append(", user='").append(user).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
