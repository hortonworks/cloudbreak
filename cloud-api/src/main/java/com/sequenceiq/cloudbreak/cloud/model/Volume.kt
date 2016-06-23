package com.sequenceiq.cloudbreak.cloud.model

class Volume(val mount: String, val type: String, val size: Int) {

    override fun toString(): String {
        val sb = StringBuilder("Volume{")
        sb.append("mount='").append(mount).append('\'')
        sb.append(", type='").append(type).append('\'')
        sb.append(", size=").append(size)
        sb.append('}')
        return sb.toString()
    }
}
