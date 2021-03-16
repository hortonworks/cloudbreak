package com.sequenceiq.environment.authorization;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class EnvironmentFiltering extends AbstractAuthorizationFiltering<List<EnvironmentDto>> {

    private final EnvironmentService environmentService;

    public EnvironmentFiltering(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    public List<EnvironmentDto> filterEnvironments(AuthorizationResourceAction action) {
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, Map.of());
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        return environmentService.findAsAuthorizationResourcesInAccount(ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public List<EnvironmentDto> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return environmentService.findAllByIds(authorizedResourceIds);
    }

    @Override
    public List<EnvironmentDto> getAll(Map<String, Object> args) {
        return environmentService.listByAccountId(ThreadBasedUserCrnProvider.getAccountId());
    }
}
