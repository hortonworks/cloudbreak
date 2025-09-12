package com.sequenceiq.authorization.service;

import java.util.EnumSet;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

/**
 * Authorization interface specifically for environment resource,
 * since hierarchical permission check is targetting environment resource as parent resource
 */
public interface EnvironmentPropertyProvider extends AuthorizationResourceNamesProvider, ResourcePropertyProvider {

    default AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.ENVIRONMENT;
    }

    @Override
    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.ENVIRONMENT);
    }
}
