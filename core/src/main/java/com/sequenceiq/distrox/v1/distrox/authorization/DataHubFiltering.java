package com.sequenceiq.distrox.v1.distrox.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Component
public class DataHubFiltering extends AbstractAuthorizationFiltering<StackViewV4Responses> {

    public static final String ENV_NAME = "ENV_NAME";

    public static final String ENV_CRN = "ENV_CRN";

    private static final List<StackType> STACK_TYPES = List.of(StackType.WORKLOAD);

    @Inject
    private StackOperations stackOperations;

    @Inject
    private StackService stackService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentService environmentClientService;

    public StackViewV4Responses filterDataHubs(AuthorizationResourceAction action, String environmentName, String environmentCrn) {
        Map<String, Object> args = new HashMap<>();
        args.put(ENV_NAME, environmentName);
        args.put(ENV_CRN, environmentCrn);
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, args);
    }

    @Override
    protected List<ResourceWithId> getAllResources(Map<String, Object> args) {
        Optional<String> envCrn = resolveEnvCrn(args);
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        if (envCrn.isPresent()) {
            return stackService.getAsAuthorizationResourcesByEnvCrn(workspaceId, envCrn.get(), STACK_TYPES);
        } else {
            return stackService.getAsAuthorizationResources(workspaceId, STACK_TYPES);
        }
    }

    @Override
    protected StackViewV4Responses filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        Optional<String> envCrn = resolveEnvCrn(args);
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return stackOperations.listByStackIds(workspaceId, authorizedResourceIds, envCrn.orElse(null), STACK_TYPES);
    }

    @Override
    protected StackViewV4Responses getAll(Map<String, Object> args) {
        String envName = getEnvName(args);
        return Strings.isNullOrEmpty(envName)
                ? stackOperations.listByEnvironmentCrn(workspaceService.getForCurrentUser().getId(), getEnvCrn(args), STACK_TYPES)
                : stackOperations.listByEnvironmentName(workspaceService.getForCurrentUser().getId(), envName, STACK_TYPES);
    }

    private Optional<String> resolveEnvCrn(Map<String, Object> args) {
        String envCrn = getEnvCrn(args);
        if (!Strings.isNullOrEmpty(envCrn)) {
            return Optional.of(envCrn);
        }
        String envName = getEnvName(args);
        if (!Strings.isNullOrEmpty(envName)) {
            return Optional.ofNullable(environmentClientService.getCrnByName(envName));
        }
        return Optional.empty();
    }

    private String getEnvName(Map<String, Object> args) {
        return (String) args.get(ENV_NAME);
    }

    private String getEnvCrn(Map<String, Object> args) {
        return (String) args.get(ENV_CRN);
    }
}
