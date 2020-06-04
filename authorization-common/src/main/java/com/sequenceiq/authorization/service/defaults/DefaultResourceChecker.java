package com.sequenceiq.authorization.service.defaults;

import java.util.Collection;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public interface DefaultResourceChecker {

    AuthorizationResourceType getResourceType();

    boolean isDefault(String resourceCrn);

    boolean isAllowedAction(AuthorizationResourceAction action);

    CrnsByCategory getDefaultResourceCrns(Collection<String> resourceCrns);
}
