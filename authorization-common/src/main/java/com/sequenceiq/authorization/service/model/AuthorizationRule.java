package com.sequenceiq.authorization.service.model;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public interface AuthorizationRule {

    Optional<AuthorizationRule> evaluateAndGetFailed(Iterator<Boolean> results);

    void convert(BiConsumer<AuthorizationResourceAction, String> collector);

    String getAsFailureMessage(Function<AuthorizationResourceAction, String> rightMapper, Function<String, Optional<String>> nameMapper);

    default String getResourceType(String crn) {
        return Optional.ofNullable(crn)
                .map(Crn::fromString)
                .map(Crn::getResourceType)
                .map(Crn.ResourceType::getName)
                .orElse("unknown resource type");
    }

    String toString(Function<AuthorizationResourceAction, String> rightMapper);
}
