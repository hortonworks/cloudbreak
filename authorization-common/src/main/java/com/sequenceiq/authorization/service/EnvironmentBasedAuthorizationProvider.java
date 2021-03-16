package com.sequenceiq.authorization.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AllMatch;
import com.sequenceiq.authorization.service.model.AnyMatch;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRight;
import com.sequenceiq.authorization.service.model.HasRightOnAll;
import com.sequenceiq.authorization.service.model.HasRightOnAny;

@Component
public class EnvironmentBasedAuthorizationProvider {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    public Optional<AuthorizationRule> getAuthorizations(String resourceCrn, AuthorizationResourceAction action) {
        ResourcePropertyProvider resourceBasedCrnProvider = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action);
        if (resourceBasedCrnProvider == null) {
            return Optional.empty();
        }
        Optional<String> environmentCrn = resourceBasedCrnProvider.getEnvironmentCrnByResourceCrn(resourceCrn);
        if (environmentCrn.isPresent()) {
            return Optional.of(new HasRightOnAny(action, List.of(environmentCrn.get(), resourceCrn)));
        } else {
            return Optional.of(new HasRight(action, resourceCrn));
        }
    }

    public Optional<AuthorizationRule> getAuthorizations(Collection<String> resourceCrns, AuthorizationResourceAction action) {
        ResourcePropertyProvider resourceBasedCrnProvider = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action);
        if (resourceBasedCrnProvider == null) {
            return Optional.empty();
        }
        Map<String, Optional<String>> withEnvironmentCrn = resourceBasedCrnProvider.getEnvironmentCrnsByResourceCrns(resourceCrns);
        if (MapUtils.isEmpty(withEnvironmentCrn)) {
            return HasRightOnAll.from(action, resourceCrns);
        } else {
            Map<String, Set<String>> byEnvironments = new LinkedHashMap<>();
            Set<String> withoutEnvironments = new LinkedHashSet<>();
            withEnvironmentCrn.forEach((resourceCrn, envCrn) -> {
                if (envCrn.isPresent()) {
                    byEnvironments.computeIfAbsent(envCrn.get(), s -> new LinkedHashSet<>()).add(resourceCrn);
                } else {
                    withoutEnvironments.add(resourceCrn);
                }
            });
            List<AuthorizationRule> authorizations = new ArrayList<>();
            authorizations.addAll(byEnvironments.entrySet()
                    .stream()
                    .map(e -> {
                        if (e.getValue().size() == 1) {
                            return new HasRightOnAny(action, List.of(e.getKey(), e.getValue().iterator().next()));
                        } else {
                            return new AnyMatch(List.of(
                                    new HasRight(action, e.getKey()),
                                    new HasRightOnAll(action, e.getValue())
                            ));
                        }
                    }).collect(Collectors.toList()));
            HasRightOnAll.from(action, withoutEnvironments).ifPresent(authorizations::add);
            return AllMatch.fromList(authorizations);
        }
    }
}
