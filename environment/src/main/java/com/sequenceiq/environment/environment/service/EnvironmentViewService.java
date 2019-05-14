package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.workspace.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.environment.environment.repository.EnvironmentViewRepository;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Service
public class EnvironmentViewService extends AbstractWorkspaceAwareResourceService<EnvironmentView> {

    @Inject
    private EnvironmentViewRepository environmentViewRepository;

    @Override
    protected WorkspaceResourceRepository<EnvironmentView, Long> repository() {
        return environmentViewRepository;
    }

    public Set<EnvironmentView> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentViewRepository.findAllByNameInAndWorkspaceId(names, workspaceId);
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

    public Set<EnvironmentView> findAllByCredentialId(Long credentialId) {
        return environmentViewRepository.findAllByCredentialId(credentialId);
    }

    public Long getIdByName(String environmentName, Long workspaceId) {
        return Optional.ofNullable(environmentViewRepository.getIdByNameAndWorkspaceId(environmentName, workspaceId))
                .orElseThrow(notFound("Environment with name", environmentName));
    }
}
