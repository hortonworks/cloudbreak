package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.authorization.RightsConstants;

public enum AuthorizationResourceAction {
    READ(RightsConstants.READ_ACTION),
    WRITE(RightsConstants.WRITE_ACTION);

    private String authorizationName;

    AuthorizationResourceAction(String authorizationName) {
        this.authorizationName = authorizationName;
    }

    public String getAuthorizationName() {
        return authorizationName;
    }

    public static Optional<AuthorizationResourceAction> getByName(String name) {
        return Arrays.stream(AuthorizationResourceAction.values())
                .filter(resource -> StringUtils.equals(resource.getAuthorizationName(), name))
                .findAny();
    }
}
