package com.sequenceiq.cloudbreak.auth.security.internal;

import org.apache.commons.lang3.NotImplementedException;

public interface AccountIdProvider {

    default String getAccountIdByResourceName(String resourceName) {
        throw new NotImplementedException("You need to implement AccountIdProvider#getAccountIdByResourceName to get account id " +
                "for internal actor call.");
    }
}
