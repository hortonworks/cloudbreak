package com.sequenceiq.cloudbreak.service.environment;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
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
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentViewRepository.findAllByNameInAndWorkspaceIdAndArchivedFalse(names, workspaceId);
    }

    @Override
    protected void prepareDeletion(EnvironmentView resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void prepareCreation(EnvironmentView resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.ENVIRONMENT;
    }

    @Override
    public Set<EnvironmentView> findAllByWorkspaceId(Long workspaceId) {
        return environmentViewRepository.findAllByWorkspaceIdAndArchivedFalse(workspaceId);
    }

    @Override
    public EnvironmentView getByNameForWorkspace(String name, Workspace workspace) {
        return environmentViewRepository.getByNameAndWorkspaceIdAndArchivedFalse(name, workspace.getId());
    }

    public Set<EnvironmentView> findAllByCredentialId(Long credentialId) {
        return environmentViewRepository.findAllByCredentialIdAndArchivedFalse(credentialId);
    }
}
