package com.sequenceiq.authorization.service;

import java.util.EnumSet;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public interface ConsumptionPropertyProvider extends AuthorizationResourceNamesProvider, ResourcePropertyProvider {

    default AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.CONSUMPTION;
    }

    @Override
    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.ENVIRONMENT);
    }
}
