package com.sequenceiq.environment.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class EnvironmentFiltering extends AbstractAuthorizationFiltering<List<EnvironmentDto>> {

    private static final String REMOTE_ENV_CRN = "REMOTE_ENV_CRN";

    private final EnvironmentService environmentService;

    public EnvironmentFiltering(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    public List<EnvironmentDto> filterEnvironments(AuthorizationResourceAction action, Optional<String> remoteEnvironmentCrn) {
        Map<String, Object> args = new HashMap<>();
        if (remoteEnvironmentCrn.isPresent()) {
            args.put(REMOTE_ENV_CRN, remoteEnvironmentCrn.get());
        }
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, args);
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
        String remoteEnvironmentCrn = getRemoteEnvCrn(args);
        if (StringUtils.isNotBlank(remoteEnvironmentCrn)) {
            return environmentService.listByAccountIdAndRemoteEnvironmentCrn(
                    ThreadBasedUserCrnProvider.getAccountId(),
                    remoteEnvironmentCrn
            );
        } else {
            return environmentService.listByAccountId(
                    ThreadBasedUserCrnProvider.getAccountId()
            );
        }
    }

    private String getRemoteEnvCrn(Map<String, Object> args) {
        return (String) args.get(REMOTE_ENV_CRN);
    }
}
