package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

public abstract class AbstractWorkspaceAwareResourceService<T extends WorkspaceAwareResource>
        implements WorkspaceAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkspaceAwareResourceService.class);

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private TransactionService transactionService;

    @Override
    public T createForLoggedInUser(T resource, @Nonnull Long workspaceId) {
        User user = getLoggedInUser();
        return create(resource, workspaceId, user);
    }

    @Override
    public T createForLoggedInUserInTransaction(T resource, @Nonnull Long workspaceId) {
        try {
            return transactionService.required(() -> createForLoggedInUser(resource, workspaceId));
        } catch (TransactionService.TransactionExecutionException e) {
            RuntimeException cause = e.getCause();
            if (cause instanceof BadRequestException) {
                throw cause;
            }
            if (cause instanceof DataIntegrityViolationException || cause instanceof ConstraintViolationException) {
                throw alreadyExistsException(resource, cause);
            }
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public T create(T resource, @Nonnull Long workspaceId, User user) {
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return create(resource, workspace, user);
    }

    @Override
    public T createWithMdcContextRestore(T resource, Workspace workspace, User user) {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        try {
            return createInternal(resource, workspace, user);
        } finally {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        }
    }

    @Override
    public T createWithMdcContextRestoreForCurrentUser(T resource, Workspace workspace) {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        try {
            return createInternal(resource, workspace, getLoggedInUser());
        } finally {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        }
    }

    @Override
    public T create(T resource, Workspace workspace, User user) {
        return createInternal(resource, workspace, user);
    }

    private T createInternal(T resource, Workspace workspace, User user) {
        try {
            MDCBuilder.buildMdcContext(resource);
            prepareCreation(resource);
            setWorkspace(resource, user, workspace);
            return repository().save(resource);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw alreadyExistsException(resource, e);
        }
    }

    private BadRequestException alreadyExistsException(T resource, RuntimeException e) throws BadRequestException {
        String message = String.format("%s already exists with name '%s' in workspace %s",
                resource.getResourceName(), resource.getName(), resource.getWorkspace().getName());
        return new BadRequestException(message, e);
    }

    @Override
    public T getByNameForWorkspace(String name, Workspace workspace) {
        Optional<T> object = repository().findByNameAndWorkspace(name, workspace);
        if (object.isEmpty()) {
            throw new NotFoundException(String.format("No resource found with name '%s'", name));
        }
        MDCBuilder.buildMdcContext(object.get());
        return object.get();
    }

    @Override
    public Set<T> getByNamesForWorkspaceId(Set<String> names, Long workspaceId) {
        Set<T> results = repository().findByNameInAndWorkspaceId(names, workspaceId);
        Set<String> notFound = Sets.difference(names,
                results.stream().map(WorkspaceAwareResource::getName).collect(Collectors.toSet()));

        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No resource(s) found with name(s) '%s'",
                    notFound.stream().map(name -> '\'' + name + '\'').collect(Collectors.joining(", "))));
        }

        return results;
    }

    @Override
    public T getByNameForWorkspaceId(String name, Long workspaceId, boolean fillMdcContext) {
        Optional<T> object = repository().findByNameAndWorkspaceId(name, workspaceId);
        if (object.isEmpty()) {
            throw new NotFoundException(String.format("No resource found with name '%s'", name));
        }
        if (fillMdcContext) {
            MDCBuilder.buildMdcContext(object.get());
        }
        return object.get();
    }

    @Override
    public Set<T> findAllByWorkspace(Workspace workspace) {
        return repository().findAllByWorkspace(workspace);
    }

    @Override
    public Set<T> findAllByWorkspaceId(Long workspaceId) {
        return repository().findAllByWorkspaceId(workspaceId);
    }

    @Override
    public T deleteWithMdcContextRestore(T resource) {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        try {
            return deleteInternal(resource, this::prepareDeletion);
        } finally {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        }
    }

    @Override
    public T delete(T resource) {
        return deleteInternal(resource, this::prepareDeletion);
    }

    public T delete(T resource, Consumer<T> prepareDeletion) {
        return deleteInternal(resource, prepareDeletion);
    }

    private T deleteInternal(T resource, Consumer<T> prepareDeletion) {
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Deleting {} with name: {}", resource.getResourceName(), resource.getName());
        prepareDeletion.accept(resource);
        repository().delete(resource);
        return resource;
    }

    @Override
    public Set<T> delete(Set<T> resources) {
        return delete(resources, this::prepareDeletion);
    }

    public Set<T> delete(Set<T> resources, Consumer<T> prepareDeletion) {
        return resources.stream()
                .map(r -> delete(r, prepareDeletion))
                .collect(Collectors.toSet());
    }

    @Override
    public T deleteByNameFromWorkspace(String name, Long workspaceId) {
        T toBeDeleted = getByNameForWorkspaceId(name, workspaceId);
        return delete(toBeDeleted);
    }

    @Override
    public Set<T> deleteMultipleByNameFromWorkspace(Set<String> names, Long workspaceId) {
        Set<T> toBeDeleted = getByNamesForWorkspaceId(names, workspaceId);
        return delete(toBeDeleted);
    }

    protected void setWorkspace(T resource, User user, Workspace workspace) {
        Set<Workspace> usersWorkspaces = workspaceService.retrieveForUser(user);
        if (!usersWorkspaces.contains(workspace)) {
            throw new NotFoundException("Workspace not found for user: " + workspace.getName());
        }
        resource.setWorkspace(workspace);
    }

    @Override
    public Iterable<T> findAll() {
        return repository().findAll();
    }

    @Override
    public T pureSave(T resource) {
        return repository().save(resource);
    }

    @Override
    public Iterable<T> pureSaveAll(Iterable<T> resources) {
        return repository().saveAll(resources);
    }

    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    protected User getLoggedInUser() {
        return userService.getOrCreate(legacyRestRequestThreadLocalService.getCloudbreakUser());
    }

    protected abstract WorkspaceResourceRepository<T, Long> repository();

    protected abstract void prepareDeletion(T resource);

    protected abstract void prepareCreation(T resource);

}
