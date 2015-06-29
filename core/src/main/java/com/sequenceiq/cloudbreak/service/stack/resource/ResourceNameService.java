package com.sequenceiq.cloudbreak.service.stack.resource;

import com.sequenceiq.cloudbreak.domain.ResourceType;

/**
 * Contract for resource name generation.
 * Cloud providers may have specific rules regarding cloud resource names; implementers
 * are expected to follow these.
 */
public interface ResourceNameService {

    /**
     * Generates a name based on the provided parts
     *
     * @param parts information to be used in the generated name
     * @return the resourceName compliant with the provider's requirements
     */
    String resourceName(ResourceType resourceType, Object... parts);
}
