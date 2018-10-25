package com.sequenceiq.cloudbreak.service;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public abstract class AbstractWorkspaceAwareResourceService<T extends WorkspaceAwareResource> implements WorkspaceAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkspaceAwareResourceService.class);

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

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
            prepareCreation(resource);
            setWorkspace(resource, user, workspace);
            return repository().save(resource);
        } catch (DataIntegrityViolationException e) {
            String message = String.format("%s already exists with name '%s' in workspace %s",
                    resource().getShortName(), resource.getName(), resource.getWorkspace().getName());
            throw new BadRequestException(message, e);
        }
    }

    @Override
    public T getByNameForWorkspace(String name, Workspace workspace) {
        T object = repository().findByNameAndWorkspace(name, workspace);
        if (object == null) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
        }
        return object;
    }

    @Override
    public T getByNameForWorkspaceId(String name, Long workspaceId) {
        T object = repository().findByNameAndWorkspaceId(name, workspaceId);
        if (object == null) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
        }
        return object;
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
        LOGGER.info("Deleting {} with name: {}", resource().getReadableName(), resource.getName());
        prepareDeletion(resource);
        repository().delete(resource);
        return resource;
    }

    @Override
    public T deleteByNameFromWorkspace(String name, Long workspaceId) {
        T toBeDeleted = getByNameForWorkspaceId(name, workspaceId);
        return delete(toBeDeleted);
    }

    protected void setWorkspace(T resource, User user, Workspace workspace) {
        Set<Workspace> usersWorkspaces = workspaceService.retrieveForUser(user);
        if (!usersWorkspaces.contains(workspace)) {
            throw new NotFoundException("Workspace not found for user.");
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
        return userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
    }

    protected abstract WorkspaceResourceRepository<T, Long> repository();

    protected abstract void prepareDeletion(T resource);

    protected abstract void prepareCreation(T resource);
}
