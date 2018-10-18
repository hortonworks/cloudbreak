package com.sequenceiq.cloudbreak.service.environment;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

public abstract class AbstractEnvironmentAwareService<T extends EnvironmentAwareResource> extends AbstractWorkspaceAwareResourceService<T> {

    @Inject
    private EnvironmentViewService environmentViewService;

    public T createInEnvironment(T resource, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentViews = environmentViewService().findByNamesInWorkspace(environments, workspaceId);
        resource.setEnvironments(environmentViews);
        return createForLoggedInUser(resource, workspaceId);
    }

    public Set<T> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : repository().findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    public Set<T> findAllInWorkspaceAndEnvironment(@NotNull Long workspaceId, String environmentName, Boolean attachGlobalResources) {
        attachGlobalResources = attachGlobalResources == null ? Boolean.TRUE : attachGlobalResources;
        Set<T> resources;
        if (StringUtils.isEmpty(environmentName)) {
            if (attachGlobalResources) {
                resources = findAllByWorkspaceId(workspaceId);
            } else {
                resources = repository().findAllByWorkspaceIdAndEnvironmentsIsNotNull(workspaceId);
            }
        } else {
            resources = new HashSet<>();
            EnvironmentView env = environmentViewService.getByNameForWorkspaceId(environmentName, workspaceId);
            resources.addAll(repository().findAllByWorkspaceIdAndEnvironments(workspaceId, env));
            if (attachGlobalResources) {
                resources.addAll(repository().findAllByWorkspaceIdAndEnvironmentsIsNull(workspaceId));
            }
        }
        return resources;
    }

    protected abstract EnvironmentResourceRepository<T, Long> repository();

    public EnvironmentViewService environmentViewService() {
        return environmentViewService;
    }
}
