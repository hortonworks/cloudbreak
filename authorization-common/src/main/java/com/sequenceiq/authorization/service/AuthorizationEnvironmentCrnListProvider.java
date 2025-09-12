package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Authorization framework interface for getting environment CRN list by resource CRN list to allow
 * authz framework to execute hierarchical permission check
 * (checking permission not only against the CRN of the resource but against the CRN of parent environmenty)
 */
public interface AuthorizationEnvironmentCrnListProvider extends ResourcePropertyProvider {

    Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns);
}
