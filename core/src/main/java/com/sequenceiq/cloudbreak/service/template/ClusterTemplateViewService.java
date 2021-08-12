package com.sequenceiq.cloudbreak.service.template;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class ClusterTemplateViewService extends AbstractWorkspaceAwareResourceService<ClusterTemplateView> {

    @Inject
    private ClusterTemplateViewRepository repository;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplateView, Long> repository() {
        return repository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplateView resource) {
        throw new BadRequestException("Cluster template deletion is not supported from ClusterTemplateViewService");
    }

    @Override
    protected void prepareCreation(ClusterTemplateView resource) {
        throw new BadRequestException("Cluster template creation is not supported from ClusterTemplateViewService");
    }

    public Set<ClusterTemplateView> findAllActive(Long workspaceId) {
        return repository.findAllActive(workspaceId);
    }

    public Set<ClusterTemplateView> findAllByStackIds(List<Long> stackIds) {
        return repository.findAllByStackIds(stackIds);
    }

    public Set<ClusterTemplateView> findAllUserManagedAndDefaultByEnvironmentCrn(Long workspaceId, String environmentCrn,
        String cloudPlatform, String runtime) {
        return repository.findAllUserManagedAndDefaultByEnvironmentCrn(workspaceId, environmentCrn, cloudPlatform, runtime);
    }
}
