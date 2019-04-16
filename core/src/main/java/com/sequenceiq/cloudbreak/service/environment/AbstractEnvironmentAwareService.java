package com.sequenceiq.cloudbreak.service.environment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.ResourceDetachValidator;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;

public abstract class AbstractEnvironmentAwareService<T extends EnvironmentAwareResource & ArchivableResource> extends AbstractArchivistService<T> {

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private ResourceDetachValidator resourceDetachValidator;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public T createInEnvironment(T resource, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentsInWorkspace = environmentViewService.findByNamesInWorkspace(environments, workspaceId);
        validateEnvironments(environmentsInWorkspace, environments, "created");
        resource.setEnvironments(environmentsInWorkspace);
        return createForLoggedInUser(resource, workspaceId);
    }

    public <C> C attachToEnvironmentsAndConvert(String resourceName, Set<String> environments, @NotNull Long workspaceId, Class<C> classToConvert) {
        return conversionService.convert(attachToEnvironments(resourceName, environments, workspaceId), classToConvert);
    }

    public T attachToEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentsInWorkspace = environmentViewService.findByNamesInWorkspace(environments, workspaceId);
        validateEnvironments(environmentsInWorkspace, environments, "attached");
        T resource = getByNameForWorkspaceId(resourceName, workspaceId);
        resource.getEnvironments().removeAll(environmentsInWorkspace);
        resource.getEnvironments().addAll(environmentsInWorkspace);
        return repository().save(resource);
    }

    public <C> C detachFromEnvironmentsAndConvert(String resourceName, Set<String> environments, @NotNull Long workspaceId, Class<C> classToConvert) {
        return conversionService.convert(detachFromEnvironments(resourceName, environments, workspaceId), classToConvert);
    }

    public T detachFromEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentsInWorkspace = environmentViewService.findByNamesInWorkspace(environments, workspaceId);
        validateEnvironments(environmentsInWorkspace, environments, "detached");
        T resource = getByNameForWorkspaceId(resourceName, workspaceId);
        checkClustersForDetach(resource, environmentsInWorkspace);
        resource.getEnvironments().removeAll(environmentsInWorkspace);
        return repository().save(resource);
    }

    public Set<T> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : repository().findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    public Set<T> findAllInWorkspaceAndEnvironment(@NotNull Long workspaceId, EnvironmentView environment) {
        return repository().findAllByWorkspaceIdAndEnvironments(workspaceId, environment);
    }

    public Set<T> findAllInWorkspaceAndEnvironment(@NotNull Long workspaceId, String environmentName, Boolean attachGlobalResources) {
        attachGlobalResources = attachGlobalResources == null ? Boolean.TRUE : attachGlobalResources;
        Set<T> resources;
        if (StringUtils.isEmpty(environmentName)) {
            resources = attachGlobalResources ? findAllByWorkspaceId(workspaceId) : repository().findAllByWorkspaceIdAndEnvironmentsIsNotNull(workspaceId);
        } else {
            EnvironmentView env = environmentViewService.getByNameForWorkspaceId(environmentName, workspaceId);
            resources = new HashSet<>(repository().findAllByWorkspaceIdAndEnvironments(workspaceId, env));
            if (attachGlobalResources) {
                resources.addAll(repository().findAllByWorkspaceIdAndEnvironmentsIsNull(workspaceId));
            }
        }
        return resources;
    }

    @Override
    protected void prepareDeletion(T resource) {
        checkClustersForDeletion(resource);
    }

    protected void prepareCreation(T resource) {
    }

    protected void checkClustersForDeletion(T resource) {
        Set<Cluster> clustersWithThisProxy = getClustersUsingResource(resource);
        if (!clustersWithThisProxy.isEmpty()) {
            String clusters = clustersWithThisProxy
                    .stream()
                    .map(Cluster::getName)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format(resource().getReadableName() + " '%s' cannot be deleted"
                    + " because there are clusters associated with it: [%s].", resource.getName(), clusters));
        }
    }

    protected void checkClustersForDetach(T resource, Set<EnvironmentView> envsInWorkspace) {
        Map<EnvironmentView, Set<Cluster>> envsToClusters = envsInWorkspace.stream()
                .collect(Collectors.toMap(env -> env, env -> getClustersUsingResourceInEnvironment(resource, env.getId())));
        ValidationResult validationResult = resourceDetachValidator.validate(resource, envsToClusters);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateEnvironments(Set<EnvironmentView> environmentsInWorkspace, Set<String> environments, String messageEnding) {
        if (environmentsInWorkspace.size() < environments.size()) {
            Set<String> existingEnvNames = environmentsInWorkspace.stream().map(EnvironmentView::getName)
                    .collect(Collectors.toSet());
            Set<String> requestedEnvironments = new HashSet<>(environments);
            requestedEnvironments.removeAll(existingEnvNames);
            throw new BadRequestException(
                    String.format("The following environments does not exist in the workspace: [%s], therefore the resource cannot be %s.",
                            String.join(", ", requestedEnvironments), messageEnding
                    )
            );
        }
    }

    protected abstract EnvironmentResourceRepository<T, Long> repository();

    public abstract Set<Cluster> getClustersUsingResource(T resource);

    public abstract Set<Cluster> getClustersUsingResourceInEnvironment(T resource, Long environmentId);

    public EnvironmentViewService environmentViewService() {
        return environmentViewService;
    }
}
