package com.sequenceiq.authorization.service;

import java.util.Optional;

public interface AuthorizationEnvironmentCrnProvider extends ResourcePropertyProvider {

    Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn);
}
