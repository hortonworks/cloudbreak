package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

class FileSystem(var name: String?, var type: String?, var isDefaultFs: Boolean, parameters: Map<String, String>) : DynamicModel() {

    init {
        for (key in parameters.keys) {
            putParameter(key, parameters[key])
        }
    }

    override fun toString(): String {
        val sb = StringBuilder("FileSystem{")
        sb.append("name='").append(name).append('\'')
        sb.append(", type='").append(type).append('\'')
        sb.append(", defaultFs=").append(isDefaultFs)
        sb.append('}')
        return sb.toString()
    }
}
