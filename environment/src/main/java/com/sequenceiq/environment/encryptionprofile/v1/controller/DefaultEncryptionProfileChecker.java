package com.sequenceiq.environment.encryptionprofile.v1.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;
import com.sequenceiq.environment.encryptionprofile.cache.DefaultEncryptionProfileProvider;

@Component
public class DefaultEncryptionProfileChecker implements DefaultResourceChecker {

    private final DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    @Inject
    public DefaultEncryptionProfileChecker(DefaultEncryptionProfileProvider defaultEncryptionProfileProvider) {
        this.defaultEncryptionProfileProvider = defaultEncryptionProfileProvider;
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.ENCRYPTION_PROFILE;
    }

    @Override
    public boolean isDefault(String resourceCrn) {
        return defaultEncryptionProfileProvider.defaultEncryptionProfilesByCrn().containsKey(resourceCrn);
    }

    @Override
    public CrnsByCategory getDefaultResourceCrns(Collection<String> resourceCrns) {
        Map<Boolean, List<String>> byDefault = resourceCrns
                .stream()
                .collect(Collectors.partitioningBy(this::isDefault));
        return CrnsByCategory
                .newBuilder()
                .defaultResourceCrns(byDefault.getOrDefault(true, List.of()))
                .notDefaultResourceCrns(byDefault.getOrDefault(false, List.of()))
                .build();
    }

    @Override
    public boolean isAllowedAction(AuthorizationResourceAction action) {
        return DESCRIBE_ENCRYPTION_PROFILE.equals(action);
    }
}
