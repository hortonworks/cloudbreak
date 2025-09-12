package com.sequenceiq.authorization.service;

import java.util.Optional;

/**
 * Authorization framework interface for getting environment CRN by resource CRN to allow
 * authz framework to execute hierarchical permission check
 * (checking permission not only against the CRN of the resource but against the CRN of parent environmenty)
 */
public interface AuthorizationEnvironmentCrnProvider extends ResourcePropertyProvider {

    Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn);
}
