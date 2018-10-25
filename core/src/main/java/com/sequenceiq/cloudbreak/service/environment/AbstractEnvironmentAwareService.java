package com.sequenceiq.cloudbreak.service.environment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

public abstract class AbstractEnvironmentAwareService<T extends EnvironmentAwareResource> extends AbstractWorkspaceAwareResourceService<T> {

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public T createInEnvironment(T resource, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentViews = environmentViewService.findByNamesInWorkspace(environments, workspaceId);
        validateAttachDetach(environmentViews, environments, "created");
        resource.setEnvironments(environmentViews);
        return createForLoggedInUser(resource, workspaceId);
    }

    public <C> C attachToEnvironmentsAndConvert(String resourceName, Set<String> environments, @NotNull Long workspaceId, Class<C> classToConvert) {
        return conversionService.convert(attachToEnvironments(resourceName, environments, workspaceId), classToConvert);
    }

    public T attachToEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentViews = environmentViewService.findByNamesInWorkspace(environments, workspaceId);
        validateAttachDetach(environmentViews, environments, "attached");
        T resource = getByNameForWorkspaceId(resourceName, workspaceId);
        resource.getEnvironments().removeAll(environmentViews);
        resource.getEnvironments().addAll(environmentViews);
        return repository().save(resource);
    }

    public <C> C detachFromEnvironmentsAndConvert(String resourceName, Set<String> environments, @NotNull Long workspaceId, Class<C> classToConvert) {
        return conversionService.convert(detachFromEnvironments(resourceName, environments, workspaceId), classToConvert);
    }

    public T detachFromEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentViews = environmentViewService.findByNamesInWorkspace(environments, workspaceId);
        validateAttachDetach(environmentViews, environments, "detached");
        T resource = getByNameForWorkspaceId(resourceName, workspaceId);
        resource.getEnvironments().removeAll(environmentViews);
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

    private void validateAttachDetach(Set<EnvironmentView> environmentViews, Set<String> environments, String messageEnding) {
        if (environmentViews.size() < environments.size()) {
            Set<String> existingEnvNames = environmentViews.stream().map(EnvironmentView::getName)
                    .collect(Collectors.toSet());
            Set<String> requestedEnvironments = new HashSet<>(environments);
            requestedEnvironments.removeAll(existingEnvNames);
            throw new BadRequestException(
                    String.format("The following environments does not exist in the workspace: [%s], therefore the resource cannot be %s.",
                            requestedEnvironments.stream().collect(Collectors.joining(", ")), messageEnding
                    )
            );
        }
    }

    public EnvironmentViewService environmentViewService() {
        return environmentViewService;
    }
}
