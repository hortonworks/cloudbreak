package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.repository.KubernetesConfigRepository;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Service
public class KubernetesConfigService extends AbstractWorkspaceAwareResourceService<KubernetesConfig> {

    private static final String NOT_FOUND_FORMAT_MESS_NAME = "Kubernetes config with name:";

    @Inject
    private KubernetesConfigRepository kubernetesConfigRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private SecretService secretService;

    public KubernetesConfig updateByWorkspaceId(Long workspaceId, KubernetesConfig kubernetesConfig) {
        KubernetesConfig original = kubernetesConfigRepository.findByNameAndWorkspaceId(kubernetesConfig.getName(), workspaceId)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, kubernetesConfig.getName()));
        kubernetesConfig.setId(original.getId());
        kubernetesConfig.setWorkspace(original.getWorkspace());
        KubernetesConfig updated = kubernetesConfigRepository.save(kubernetesConfig);
        secretService.delete(original.getConfigurationSecret());
        return updated;
    }

    public KubernetesConfig getByNameForWorkspace(String name, Workspace workspace) {
        return getByNameForWorkspaceId(name, workspace.getId());
    }

    @Override
    protected WorkspaceResourceRepository<KubernetesConfig, Long> repository() {
        return kubernetesConfigRepository;
    }

    @Override
    protected void prepareDeletion(KubernetesConfig kubernetesConfig) {
    }

    @Override
    protected void prepareCreation(KubernetesConfig resource) {

    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.KUBERNETES;
    }

}
