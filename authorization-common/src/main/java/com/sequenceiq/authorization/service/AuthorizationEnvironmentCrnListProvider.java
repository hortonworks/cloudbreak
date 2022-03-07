package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface AuthorizationEnvironmentCrnListProvider extends ResourcePropertyProvider {

    Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns);
}
