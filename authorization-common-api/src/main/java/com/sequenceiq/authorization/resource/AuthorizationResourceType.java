package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.authorization.RightsConstants;

public enum AuthorizationResourceType {
    DATALAKE(RightsConstants.DATALAKE_RESOURCE, RightsConstants.DATALAKE_RESOURCE),
    ENVIRONMENT(RightsConstants.ENVIRONMENTS_RESOURCE, RightsConstants.ENVIRONMENTS_RESOURCE),
    CREDENTIAL(RightsConstants.CREDENTIAL_RESOURCE, RightsConstants.CREDENTIALS_RESOURCE),
    DATAHUB(RightsConstants.DATAHUB_RESOURCE, RightsConstants.DATAHUB_RESOURCE);

    private final String resourceDependentAuthorizationName;

    private final String resourceIndependentAuthorizationName;

    AuthorizationResourceType(String resourceDependentAuthorizationName, String resourceIndependentAuthorizationName) {
        this.resourceDependentAuthorizationName = resourceDependentAuthorizationName;
        this.resourceIndependentAuthorizationName = resourceIndependentAuthorizationName;
    }

    public String getResourceDependentAuthorizationName() {
        return resourceDependentAuthorizationName;
    }

    public String getResourceIndependentAuthorizationName() {
        return resourceIndependentAuthorizationName;
    }

    public static Optional<AuthorizationResourceType> getByName(String name) {
        return Arrays.stream(AuthorizationResourceType.values())
                .filter(resource -> StringUtils.equals(resource.getResourceDependentAuthorizationName(), name) ||
                        StringUtils.equals(resource.getResourceIndependentAuthorizationName(), name))
                .findAny();
    }
}
