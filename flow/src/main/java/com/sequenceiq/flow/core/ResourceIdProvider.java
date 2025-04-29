package com.sequenceiq.flow.core;

import java.util.List;

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

    default List<Long> getResourceIdsByResourceCrn(String resourceName) {
        throw new NotImplementedException("You have to implement getResourceIdsByResourceCrn for your resource "
                + "to be able to use Flow API endpoints using resource name!");
    }

    default String getResourceCrnByResourceId(Long resourceId) {
        throw new NotImplementedException("You have to implement getResourceCrnByResourceId for your resource to be able to send usage events!");
    }
}
