package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentViewRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

@Service
public class EnvironmentViewService extends AbstractWorkspaceAwareResourceService<EnvironmentView> {

    @Inject
    private EnvironmentViewRepository environmentViewRepository;

    @Override
    protected WorkspaceResourceRepository<EnvironmentView, Long> repository() {
        return environmentViewRepository;
    }

    public Set<EnvironmentView> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return environmentViewRepository.findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    @Override
    protected void prepareDeletion(EnvironmentView resource) {

    }

    @Override
    protected void prepareCreation(EnvironmentView resource) {

    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.ENVIRONMENT;
    }
}
