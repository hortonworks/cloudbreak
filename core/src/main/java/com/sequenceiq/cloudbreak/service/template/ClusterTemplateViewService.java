package com.sequenceiq.cloudbreak.service.template;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.projection.ClusterTemplateStatusView;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.distrox.v1.distrox.service.HybridClusterTemplateValidator;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@Service
public class ClusterTemplateViewService extends AbstractWorkspaceAwareResourceService<ClusterTemplateView> {

    @Inject
    private ClusterTemplateViewRepository repository;

    @Inject
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @Inject
    private HybridClusterTemplateValidator hybridClusterTemplateValidator;

    @Inject
    private EnvironmentService environmentClientService;

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

    public Set<ClusterTemplateView> findAllActive(Long workspaceId, boolean internalTenant) {
        Set<ClusterTemplateView> allActive = repository.findAllActive(workspaceId);
        return allActive.stream()
                        .filter(e -> internalClusterTemplateValidator.shouldPopulate(e, internalTenant))
                        .collect(Collectors.toSet());
    }

    public Set<ClusterTemplateView> findAllByStackIds(List<Long> stackIds) {
        return repository.findAllByStackIds(stackIds);
    }

    public ClusterTemplateStatusView getStatusViewByResourceCrn(String resourceCrn) {
        return repository.findViewByResourceCrn(resourceCrn);
    }

    public Set<ClusterTemplateView> findAllUserManagedAndDefaultByEnvironmentCrn(Long workspaceId, String environmentCrn,
        String cloudPlatform, String runtime, boolean internalTenant, Boolean hybridEnvironment) {
        Set<ClusterTemplateView> allActive = repository.findAllUserManagedAndDefaultByEnvironmentCrn(workspaceId, environmentCrn, cloudPlatform, runtime);
        return allActive.stream()
                .filter(e -> internalClusterTemplateValidator.shouldPopulate(e, internalTenant))
                .filter(e -> hybridClusterTemplateValidator.shouldPopulate(e, hybridEnvironment))
                .collect(Collectors.toSet());
    }
}
