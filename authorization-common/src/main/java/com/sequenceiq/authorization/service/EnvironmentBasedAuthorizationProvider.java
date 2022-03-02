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

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AllMatch;
import com.sequenceiq.authorization.service.model.AnyMatch;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRight;
import com.sequenceiq.authorization.service.model.HasRightOnAll;
import com.sequenceiq.authorization.service.model.HasRightOnAny;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Component
public class EnvironmentBasedAuthorizationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBasedAuthorizationProvider.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    public Optional<AuthorizationRule> getAuthorizations(String resourceCrn, AuthorizationResourceAction action) {
        ResourcePropertyProvider resourceBasedCrnProvider = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action);
        if (resourceBasedCrnProvider == null) {
            LOGGER.error("There is no resource based crn provider implemented for action {} against resource type {}, " +
                    "thus authorization is failing automatically.", action, Crn.safeFromString(resourceCrn).getResourceType().name());
            throw new AccessDeniedException(String.format("Action %s is not supported over resource %s, thus access is denied",
                    action.getRight(), resourceCrn));
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
            LOGGER.error("There is no resource based crn provider implemented for action {} against resource types {}, " +
                    "thus authorization is failing automatically.", action, Joiner.on(",").join(
                            resourceCrns.stream().map(crn -> Crn.safeFromString(crn).getResourceType().name()).collect(Collectors.toSet())));
            throw new AccessDeniedException(String.format("Action %s is not supported over resources %s, thus access is denied",
                    action.getRight(), Joiner.on(",").join(resourceCrns)));
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
