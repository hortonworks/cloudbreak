package com.sequenceiq.flow.core;

import org.apache.commons.lang3.NotImplementedException;

public interface ResourceIdProvider {

    default Long getResourceIdByResourceCrn(String resourceCrn) {
        throw new NotImplementedException("You have to implement getResourceIdByResourceCrn for your resource to be able "
                + "to use Flow API endpoints using resource CRN!");
    }

    default Long getResourceIdByResourceName(String resourceName) {
        throw new NotImplementedException("You have to implement getResourceIdByResourceName for your resource "
                + "to be able to use Flow API endpoints using resource name!");
    }
}
