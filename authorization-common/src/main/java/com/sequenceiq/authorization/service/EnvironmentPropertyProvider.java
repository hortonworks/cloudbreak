package com.sequenceiq.authorization.service;

import java.util.EnumSet;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public interface EnvironmentPropertyProvider extends AuthorizationResourceNamesProvider, ResourcePropertyProvider {

    default AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.ENVIRONMENT;
    }

    @Override
    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.ENVIRONMENT);
    }
}
