package com.sequenceiq.environment.authorization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;

@Component
public class EnvironmentCredentialFiltering extends AbstractAuthorizationFiltering<Set<Credential>> {

    private static final CredentialType ENVIRONMENT = CredentialType.ENVIRONMENT;

    private final CredentialService credentialService;

    public EnvironmentCredentialFiltering(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    public Set<Credential> filterCredntials(AuthorizationResourceAction action) {
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, Map.of());
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialService.findAsAuthorizationResourcesInAccountByType(accountId, ENVIRONMENT);
    }

    @Override
    public Set<Credential> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return credentialService.findAllById(authorizedResourceIds);
    }

    @Override
    public Set<Credential> getAll(Map<String, Object> args) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialService.listAvailablesByAccountId(accountId, ENVIRONMENT);
    }
}
