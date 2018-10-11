package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

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
        return repository().findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    protected abstract EnvironmentResourceRepository<T, Long> repository();

    public EnvironmentViewService environmentViewService() {
        return environmentViewService;
    }
}
