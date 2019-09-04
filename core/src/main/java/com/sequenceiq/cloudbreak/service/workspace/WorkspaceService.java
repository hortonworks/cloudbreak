package com.sequenceiq.cloudbreak.service.workspace;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceRepository;

@Service
public class WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private WorkspaceRepository workspaceRepository;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    public Workspace create(Workspace workspace) {
        try {
            return transactionService.required(() -> workspaceRepository.save(workspace));
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Workspace with name '%s' in your tenant already exists.",
                        workspace.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Workspace> retrieveForUser(User user) {
        return Sets.newHashSet(getByName(getAccountWorkspaceName(user), user).get());
    }

    public Workspace getDefaultWorkspaceForUser(User user) {
        return workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant());
    }

    public Optional<Workspace> getByName(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant()));
    }

    public Optional<Workspace> getByNameForUser(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant()));
    }

    public Optional<Workspace> getByNameForTenant(String workspaceName, Tenant tenant) {
        return Optional.ofNullable(workspaceRepository.getByName(workspaceName, tenant));
    }

    /**
     * Use this method with caution, since it is not authorized! Don!t use it in REST context!
     *
     * @param id id of Workspace
     * @return Workspace
     */
    public Workspace getByIdWithoutAuth(Long id) {
        Optional<Workspace> workspace = workspaceRepository.findById(id);
        if (workspace.isPresent()) {
            return workspace.get();
        }
        throw new IllegalArgumentException(String.format("No Workspace found with id: %s", id));
    }

    public Workspace getByIdForCurrentUser(Long id) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return get(id, user);
    }

    public Workspace get(Long id, User user) {
        return getDefaultWorkspaceForUser(user);
    }

    public Workspace getByNameForUserOrThrowNotFound(String workspaceName, User currentUser) {
        Optional<Workspace> workspace = getByNameForUser(workspaceName, currentUser);
        return workspace.orElseThrow(() -> new NotFoundException("Cannot find workspace with name: " + workspaceName));
    }

    public Workspace getForCurrentUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
    }

    private String getAccountWorkspaceName(User user) {
        return Crn.isCrn(user.getUserCrn()) ? Crn.fromString(user.getUserCrn()).getAccountId()
                : user.getTenant().getName();
    }

}
