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
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
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
    private Map<AuthorizationResourceType, AuthorizationEnvironmentCrnListProvider> environmentCrnListProviderMap;

    @Inject
    private Map<AuthorizationResourceType, AuthorizationEnvironmentCrnProvider> environmentCrnProviderMap;

    public Optional<AuthorizationRule> getAuthorizations(String resourceCrn, AuthorizationResourceAction action) {
        if (action.getAuthorizationResourceType().isHierarchicalAuthorizationNeeded()) {
            AuthorizationEnvironmentCrnProvider environmentCrnProvider = environmentCrnProviderMap.get(action.getAuthorizationResourceType());
            if (environmentCrnProvider == null) {
                LOGGER.error("There is no resource based crn provider implemented for action {} against resource type {}, " +
                        "thus authorization is failing automatically.", action, Crn.safeFromString(resourceCrn).getResourceType().name());
                throw new AccessDeniedException(String.format("Action %s is not supported over resource %s, thus access is denied",
                        action.getRight(), resourceCrn));
            }
            Optional<String> environmentCrnByResourceCrn = environmentCrnProvider.getEnvironmentCrnByResourceCrn(resourceCrn);
            if (environmentCrnByResourceCrn.isPresent()) {
                return Optional.of(new HasRightOnAny(action, List.of(environmentCrnByResourceCrn.get(), resourceCrn)));
            } else {
                return Optional.of(new HasRight(action, resourceCrn));
            }
        } else {
            return Optional.of(new HasRight(action, resourceCrn));
        }
    }

    public Optional<AuthorizationRule> getAuthorizations(Collection<String> resourceCrns, AuthorizationResourceAction action) {
        if (action.getAuthorizationResourceType().isHierarchicalAuthorizationNeeded()) {
            AuthorizationEnvironmentCrnListProvider environmentCrnListProvider = environmentCrnListProviderMap.get(action.getAuthorizationResourceType());
            if (environmentCrnListProvider == null) {
                LOGGER.error("There is no resource based crn provider implemented for action {} against resource types {}, " +
                        "thus authorization is failing automatically.", action, Joiner.on(",").join(
                        resourceCrns.stream().map(crn -> Crn.safeFromString(crn).getResourceType().name()).collect(Collectors.toSet())));
                throw new AccessDeniedException(String.format("Action %s is not supported over resources %s, thus access is denied",
                        action.getRight(), Joiner.on(",").join(resourceCrns)));
            }
            Map<String, String> withEnvironmentCrn = environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(resourceCrns)
                    .entrySet().stream().filter(entry -> entry.getValue().isPresent()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get()));
            if (MapUtils.isEmpty(withEnvironmentCrn)) {
                return HasRightOnAll.from(action, resourceCrns);
            } else {
                Set<String> resourcesWithoutEnvironment = Sets.difference(Sets.newHashSet(resourceCrns), withEnvironmentCrn.keySet());
                List<AuthorizationRule> authorizationRules = getEnvironmentAwareAuthorizationRules(action, withEnvironmentCrn);
                HasRightOnAll.from(action, resourcesWithoutEnvironment).ifPresent(authorizationRules::add);
                return AllMatch.fromList(authorizationRules);
            }
        } else {
            return HasRightOnAll.from(action, resourceCrns);
        }
    }

    private List<AuthorizationRule> getEnvironmentAwareAuthorizationRules(AuthorizationResourceAction action, Map<String, String> withEnvironmentCrn) {
        Map<String, Set<String>> byEnvironments = new LinkedHashMap<>();
        withEnvironmentCrn.forEach((resourceCrn, envCrn) -> byEnvironments.computeIfAbsent(envCrn, s -> new LinkedHashSet<>()).add(resourceCrn));
        List<AuthorizationRule> authorizations = new ArrayList<>();
        authorizations.addAll(byEnvironments.entrySet()
                .stream()
                .map(entry -> transformCrnEntryToAuthorizationRule(action, entry))
                .collect(Collectors.toList()));
        return authorizations;
    }

    private AuthorizationRule transformCrnEntryToAuthorizationRule(AuthorizationResourceAction action, Map.Entry<String, Set<String>> crnEntry) {
        String environmentCrn = crnEntry.getKey();
        Set<String> resourceCrns = crnEntry.getValue();
        if (resourceCrns.size() == 1) {
            return new HasRightOnAny(action, List.of(environmentCrn, resourceCrns.iterator().next()));
        } else {
            return new AnyMatch(List.of(
                    new HasRight(action, environmentCrn),
                    new HasRightOnAll(action, resourceCrns)
            ));
        }
    }
}
