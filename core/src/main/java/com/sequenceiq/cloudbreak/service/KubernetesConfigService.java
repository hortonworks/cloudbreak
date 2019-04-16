package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.KubernetesConfigRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Service
public class KubernetesConfigService extends AbstractEnvironmentAwareService<KubernetesConfig> {

    private static final String NOT_FOUND_FORMAT_MESS_NAME = "Kubernetes config with name:";

    @Inject
    private KubernetesConfigRepository kubernetesConfigRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private SecretService secretService;

    @Override
    public KubernetesConfig attachToEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        try {
            return transactionService.required(() -> super.attachToEnvironments(resourceName, environments, workspaceId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public KubernetesConfig detachFromEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        try {
            return transactionService.required(() -> super.detachFromEnvironments(resourceName, environments, workspaceId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public KubernetesConfig updateByWorkspaceId(Long workspaceId, KubernetesConfig kubernetesConfig) {
        KubernetesConfig original = kubernetesConfigRepository.findByNameAndWorkspaceId(kubernetesConfig.getName(), workspaceId)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, kubernetesConfig.getName()));
        kubernetesConfig.setId(original.getId());
        kubernetesConfig.setWorkspace(original.getWorkspace());
        kubernetesConfig.setEnvironments(original.getEnvironments());
        KubernetesConfig updated = kubernetesConfigRepository.save(kubernetesConfig);
        secretService.delete(original.getConfigurationSecret());
        return updated;
    }

    public KubernetesConfig getByNameForWorkspace(String name, Workspace workspace) {
        return getByNameForWorkspaceId(name, workspace.getId());
    }

    @Override
    public Set<Cluster> getClustersUsingResource(KubernetesConfig resource) {
        return Collections.emptySet();
    }

    @Override
    public Set<Cluster> getClustersUsingResourceInEnvironment(KubernetesConfig kubernetesConfig, Long environmentId) {
        return Collections.emptySet();
    }

    @Override
    protected void prepareDeletion(KubernetesConfig kubernetesConfig) {
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.KUBERNETES;
    }

    @Override
    public EnvironmentResourceRepository<KubernetesConfig, Long> repository() {
        return kubernetesConfigRepository;
    }
}
