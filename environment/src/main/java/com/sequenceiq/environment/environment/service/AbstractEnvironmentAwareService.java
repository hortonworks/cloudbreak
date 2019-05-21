package com.sequenceiq.environment.environment.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.repository.EnvironmentResourceRepository;

public abstract class AbstractEnvironmentAwareService<T extends EnvironmentAwareResource> {

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public T createInEnvironment(T resource, Set<String> environments, String accountId) {
        Set<EnvironmentView> environmentsInWorkspace = environmentViewService.findByNamesInAccount(environments, accountId);
        validateEnvironments(environmentsInWorkspace, environments, "created");
        resource.setEnvironments(environmentsInWorkspace);
        return create(resource, accountId);
    }

    public T create(T resource, String accountId) {
        MDCBuilder.buildMdcContext(resource);
        resource.setAccountId(accountId);
        return repository().save(resource);
    }

    public <C> C attachToEnvironmentsAndConvert(String resourceName, Set<String> environments, @NotNull Long workspaceId, Class<C> classToConvert) {
        return conversionService.convert(attachToEnvironments(resourceName, environments, workspaceId), classToConvert);
    }

    public T attachToEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentsInWorkspace = environmentViewService.findByNamesInAccount(environments, workspaceId.toString());
        validateEnvironments(environmentsInWorkspace, environments, "attached");
        T resource = getByNameForAccountId(resourceName, workspaceId.toString());
        resource.getEnvironments().removeAll(environmentsInWorkspace);
        resource.getEnvironments().addAll(environmentsInWorkspace);
        return repository().save(resource);
    }

    public <C> C detachFromEnvironmentsAndConvert(String resourceName, Set<String> environments, @NotNull Long workspaceId, Class<C> classToConvert) {
        return conversionService.convert(detachFromEnvironments(resourceName, environments, workspaceId), classToConvert);
    }

    public T detachFromEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        Set<EnvironmentView> environmentsInWorkspace = environmentViewService.findByNamesInAccount(environments, workspaceId.toString());
        validateEnvironments(environmentsInWorkspace, environments, "detached");
        T resource = getByNameForAccountId(resourceName, workspaceId.toString());
        checkClustersForDetach(resource, environmentsInWorkspace);
        resource.getEnvironments().removeAll(environmentsInWorkspace);
        return repository().save(resource);
    }

    public T getByNameForAccountId(String name, String accountId) {
        return repository().getByNameAndAccountId(name, accountId);
    }

    public Set<T> findByNamesInAccount(Set<String> proxyNames, String accountId) {
        return CollectionUtils.isEmpty(proxyNames)
                ? new HashSet<>()
                : repository().findAllByNameInAndAccountId(proxyNames, accountId);
    }

    public Set<T> findAllInAccountAndEnvironment(String accountId, EnvironmentView environment) {
        return repository().findAllByAccountIdAndEnvironments(accountId, environment);
    }

    public T delete(T resource) {
        MDCBuilder.buildMdcContext(resource);
        repository().delete(resource);
        return resource;
    }

    protected void checkClustersForDeletion(T resource) {
        //TODO Design and implement of the deletion flow between the services
//        Set<Cluster> clustersWithThisProxy = getClustersUsingResource(resource);
//        if (!clustersWithThisProxy.isEmpty()) {
//            String clusters = clustersWithThisProxy
//                    .stream()
//                    .map(Cluster::getName)
//                    .collect(Collectors.joining(", "));
//            throw new BadRequestException(String.format(resource().getReadableName() + " '%s' cannot be deleted"
//                    + " because there are clusters associated with it: [%s].", resource.getName(), clusters));
//        }
    }

    protected void checkClustersForDetach(T resource, Set<EnvironmentView> envsInWorkspace) {
        //TODO Design and implement of the deletion flow between the services
//        Map<EnvironmentView, Set<Cluster>> envsToClusters = envsInWorkspace.stream()
//                .collect(Collectors.toMap(env -> env, env -> getClustersUsingResourceInEnvironment(resource, env.getId())));
//        ValidationResult validationResult = resourceDetachValidator.validate(resource, envsToClusters);
//        if (validationResult.hasError()) {
//            throw new BadRequestException(validationResult.getFormattedErrors());
//        }
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

    //TODO Design and implement of the deletion flow between the services
//    public abstract Set<Cluster> getClustersUsingResource(T resource);
//
//    public abstract Set<Cluster> getClustersUsingResourceInEnvironment(T resource, Long environmentId);

    public EnvironmentViewService environmentViewService() {
        return environmentViewService;
    }
}
