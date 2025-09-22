package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;
import com.sequenceiq.authorization.service.model.AuthorizationRule;

@Component
public class DefaultResourceAuthorizationProvider {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap;

    public Optional<AuthorizationRule> authorizeDefaultOrElseCompute(String resourceCrn, AuthorizationResourceAction action,
            Supplier<Optional<AuthorizationRule>> supplier) {
        AuthorizationResourceType authorizationResourceType = action.getAuthorizationResourceType();
        DefaultResourceChecker defaultResourceChecker = null;
        if (authorizationResourceType != null) {
            defaultResourceChecker = defaultResourceCheckerMap.get(authorizationResourceType);
        }
        if (defaultResourceChecker != null && defaultResourceChecker.isDefault(resourceCrn)) {
            commonPermissionCheckingUtils.throwAccessDeniedIfActionNotAllowed(action, List.of(resourceCrn), Optional.of(defaultResourceChecker));
            return Optional.empty();
        } else {
            return supplier.get();
        }
    }

    public Optional<AuthorizationRule> authorizeDefaultOrElseCompute(Collection<String> resourceCrns, AuthorizationResourceAction action,
            Function<Collection<String>, Optional<AuthorizationRule>> function) {
        DefaultResourceChecker defaultResourceChecker = defaultResourceCheckerMap.get(action.getAuthorizationResourceType());
        if (defaultResourceChecker != null) {
            CrnsByCategory crnsByCategory = defaultResourceChecker.getDefaultResourceCrns(resourceCrns);
            if (!crnsByCategory.getDefaultResourceCrns().isEmpty()) {
                commonPermissionCheckingUtils.throwAccessDeniedIfActionNotAllowed(action, crnsByCategory.getDefaultResourceCrns(),
                        Optional.of(defaultResourceChecker));
            }
            if (!crnsByCategory.getNotDefaultResourceCrns().isEmpty()) {
                return function.apply(crnsByCategory.getNotDefaultResourceCrns());
            } else {
                return Optional.empty();
            }
        } else {
            return function.apply(resourceCrns);
        }
    }
}
