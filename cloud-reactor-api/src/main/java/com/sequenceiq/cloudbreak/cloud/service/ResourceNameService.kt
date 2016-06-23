package com.sequenceiq.cloudbreak.cloud.service

import com.sequenceiq.cloudbreak.common.type.ResourceType

/**
 * Contract for resource name generation.
 * Cloud providers may have specific rules regarding cloud resource names; implementers
 * are expected to follow these.
 */
interface ResourceNameService {

    /**
     * Generates a name based on the provided parts

     * @param parts information to be used in the generated name
     * *
     * @return the resourceName compliant with the provider's requirements
     */
    fun resourceName(resourceType: ResourceType, vararg parts: Any): String
}
