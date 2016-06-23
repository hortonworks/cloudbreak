package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

class CloudInstance : DynamicModel {

    var instanceId: String? = null
        private set
    var template: InstanceTemplate? = null
        private set

    constructor(instanceId: String, template: InstanceTemplate) {
        this.instanceId = instanceId
        this.template = template
    }

    constructor(instanceId: String, template: InstanceTemplate, params: MutableMap<String, Any>) : super(params) {
        this.instanceId = instanceId
        this.template = template
    }

    override fun toString(): String {
        val sb = StringBuilder("CloudInstance{")
        sb.append("instanceId='").append(instanceId).append('\'')
        sb.append(", template=").append(template)
        sb.append('}')
        return sb.toString()
    }
}
