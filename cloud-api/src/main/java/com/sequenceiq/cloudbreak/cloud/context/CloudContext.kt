package com.sequenceiq.cloudbreak.cloud.context

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

/**
 * Context object is used to identify messages exchanged between core and Cloud Platfrom. This context object passed along
 * with the flow to all methods and also sent back in the Response objects.

 */
class CloudContext {

    val id: Long?
    val name: String
    val platform: Platform
    val owner: String
    val variant: Variant?
    val location: Location?

    constructor(id: Long?, name: String, platform: String, owner: String) {
        this.id = id
        this.name = name
        this.platform = Platform.platform(platform)
        this.owner = owner
        this.variant = null
        this.location = null
    }

    constructor(id: Long?, name: String, platform: String, owner: String, variant: String, location: Location) {
        this.id = id
        this.name = name
        this.platform = Platform.platform(platform)
        this.owner = owner
        this.variant = Variant.variant(variant)
        this.location = location
    }

    val platformVariant: CloudPlatformVariant
        get() = CloudPlatformVariant(platform, variant)

    override fun toString(): String {
        val sb = StringBuilder("CloudContext{")
        sb.append("id=").append(id)
        sb.append(", name='").append(name).append('\'')
        sb.append(", platform='").append(platform).append('\'')
        sb.append(", owner='").append(owner).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
