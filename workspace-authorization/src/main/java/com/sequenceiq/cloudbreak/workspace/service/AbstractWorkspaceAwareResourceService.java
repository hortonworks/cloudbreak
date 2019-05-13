package com.sequenceiq.cloudbreak.workspace.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.user.UserService;
import com.sequenceiq.cloudbreak.util.ThrowableUtil;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

public abstract class AbstractWorkspaceAwareResourceService<T extends WorkspaceAwareResource>
        implements WorkspaceAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkspaceAwareResourceService.class);

    @Inject
    private WorkspaceService workspaceService;

    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserService userService;

    @Override
    public T createForLoggedInUser(T resource, @Nonnull Long workspaceId) {
        User user = getLoggedInUser();
        return create(resource, workspaceId, user);
    }

    @Override
    public T create(T resource, @Nonnull Long workspaceId, User user) {
        Workspace workspace = workspaceService.get(workspaceId, user);
        return create(resource, workspace, user);
    }

    @Override
    public T create(T resource, Workspace workspace, User user) {
        try {
            MDCBuilder.buildMdcContext(resource);
            prepareCreation(resource);
            setWorkspace(resource, user, workspace);
            return repository().save(resource);
        } catch (AccessDeniedException e) {
            ConstraintViolationException cve = ThrowableUtil.getSpecificCauseRecursively(e, ConstraintViolationException.class);
            if (cve != null) {
                String message = String.format("%s already exists with name '%s' in workspace %s",
                        resource().getShortName(), resource.getName(), resource.getWorkspace().getName());
                throw new BadRequestException(message, e);
            }
            throw e;
        }
    }

    @Override
    public T getByNameForWorkspace(String name, Workspace workspace) {
        Optional<T> object = repository().findByNameAndWorkspace(name, workspace);
        if (object.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
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
            throw new NotFoundException(String.format("No %s(s) found with name(s) '%s'", resource().getShortName(),
                    notFound.stream().map(name -> '\'' + name + '\'').collect(Collectors.joining(", "))));
        }

        return results;
    }

    @Override
    public T getByNameForWorkspaceId(String name, Long workspaceId) {
        Optional<T> object = repository().findByNameAndWorkspaceId(name, workspaceId);
        if (object.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
        }
        MDCBuilder.buildMdcContext(object.get());
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
    public T delete(T resource) {
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Deleting {} with name: {}", resource().getReadableName(), resource.getName());
        prepareDeletion(resource);
        repository().delete(resource);
        return resource;
    }

    @Override
    public Set<T> delete(Set<T> resources) {
        return resources.stream()
                .map(r -> delete(r))
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
        return userService.getOrCreate(authenticatedUserService.getCbUser());
    }

    protected abstract WorkspaceResourceRepository<T, Long> repository();

    protected abstract void prepareDeletion(T resource);

    protected abstract void prepareCreation(T resource);
}
