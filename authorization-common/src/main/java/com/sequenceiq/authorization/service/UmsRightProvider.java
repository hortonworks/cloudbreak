package com.sequenceiq.authorization.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class UmsRightProvider {

    public AuthorizationResourceType getResourceType(AuthorizationResourceAction action) {
        return action.getAuthorizationResourceType();
    }

    public String getRight(AuthorizationResourceAction action) {
        return action.getRight();
    }

    public Function<AuthorizationResourceAction, String> getRightMapper() {
        return this::getRight;
    }

    public Optional<AuthorizationResourceAction> getByName(String name) {
        return Arrays.stream(AuthorizationResourceAction.values())
                .filter(action -> StringUtils.equals(action.getRight(), name))
                .findAny();
    }
}
