package com.sequenceiq.cloudbreak.cloud.model

import java.util.HashMap

import com.google.common.base.Preconditions
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel
import com.sequenceiq.cloudbreak.common.type.CommonStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType

/**
 * Cloudbreak handles the entities on the Cloud provider side as Generic resources, and this class represent a generic resource.
 */
class CloudResource private constructor(val type: ResourceType, val status: CommonStatus, val name: String, val reference: String, val isPersistent: Boolean, params: MutableMap<String, Any>) : DynamicModel(params) {

    override fun toString(): String {
        val sb = StringBuilder("CloudResource{")
        sb.append("type=").append(type)
        sb.append(", status=").append(status)
        sb.append(", name='").append(name).append('\'')
        sb.append(", reference='").append(reference).append('\'')
        sb.append(", persistent='").append(isPersistent).append('\'')
        sb.append('}')
        return sb.toString()
    }

    class Builder {
        private var type: ResourceType? = null
        private var status = CommonStatus.CREATED
        private var name: String? = null
        private var reference: String? = null
        private var persistent = true
        private var parameters: MutableMap<String, Any> = HashMap()

        fun cloudResource(cloudResource: CloudResource): Builder {
            this.type = cloudResource.type
            this.status = cloudResource.status
            this.name = cloudResource.name
            this.reference = cloudResource.reference
            this.persistent = cloudResource.isPersistent
            return this
        }

        fun type(type: ResourceType): Builder {
            this.type = type
            return this
        }

        fun status(status: CommonStatus): Builder {
            this.status = status
            return this
        }

        fun name(name: String): Builder {
            this.name = name
            return this
        }

        fun reference(reference: String): Builder {
            this.reference = reference
            return this
        }

        fun persistent(persistent: Boolean): Builder {
            this.persistent = persistent
            return this
        }

        fun params(parameters: MutableMap<String, Any>): Builder {
            this.parameters = parameters
            return this
        }

        fun build(): CloudResource {
            Preconditions.checkNotNull<ResourceType>(type)
            Preconditions.checkNotNull(status)
            Preconditions.checkNotNull<String>(name)
            Preconditions.checkNotNull<Map<String, Any>>(parameters)
            return CloudResource(type, status, name, reference, persistent, parameters)
        }
    }
}
