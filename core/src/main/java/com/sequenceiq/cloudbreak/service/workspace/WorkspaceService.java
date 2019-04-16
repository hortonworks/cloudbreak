package com.sequenceiq.cloudbreak.service.workspace;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus.DELETED;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Request;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.authorization.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.authorization.ResourceAction;
import com.sequenceiq.cloudbreak.authorization.WorkspaceRole;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserService;

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
    private WorkspaceModificationVerifierService verifierService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private Clock clock;

    @Inject
    private UmsAuthorizationService umsAuthorizationService;

    public Workspace create(User user, Workspace workspace) {
        try {
            return transactionService.required(() -> {
                workspace.setResourceCrnByUser(user);
                umsAuthorizationService.assignResourceRoleToUserInWorkspace(user, workspace, WorkspaceRole.WORKSPACEMANAGER);
                return workspaceRepository.save(workspace);
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Workspace with name '%s' in your tenant already exists.",
                        workspace.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

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
        //return umsAuthorizationService.getWorkspacesOfCurrentUser(user);
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

    public Set<User> removeUsers(String workspaceName, Set<String> userIds, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, ResourceAction.MANAGE,
                "You cannot remove users from this workspace.");
        verifierService.ensureWorkspaceManagementForUserRemainingUsers(currentUser, workspace, userIds);
        try {
            return transactionService.required(() -> {
                Set<User> usersToBeRemoved = userService.getByUsersIds(userIds);
                verifierService.validateAllUsersAreAlreadyInTheWorkspace(currentUser, workspace, usersToBeRemoved);
                verifierService.verifyDefaultWorkspaceUserRemovals(currentUser, workspace, usersToBeRemoved);
                umsAuthorizationService.removeResourceRolesOfUserInWorkspace(usersToBeRemoved, workspace);
                return usersToBeRemoved;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> addUsers(String workspaceName, Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, ResourceAction.MANAGE,
                "You cannot add users to this workspace.");
        Set<String> userIdsToAdd = changeWorkspaceUsersV4Requests.stream()
                .map(request -> request.getUserId())
                .collect(Collectors.toSet());
        Set<User> usersToAdd = userService.getByUsersIds(userIdsToAdd);
        verifierService.validateUsersAreNotInTheWorkspaceYet(currentUser, workspace, usersToAdd);
        try {
            return transactionService.required(() -> {
                usersToAdd.stream().forEach(userToAdd -> addUserByRequest(changeWorkspaceUsersV4Requests, workspace, userToAdd));
                return usersToAdd;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> updateUsers(String workspaceName, Set<ChangeWorkspaceUsersV4Request> updateWorkspaceUsersJsons, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        Set<User> usersInWorkspace = umsAuthorizationService.getUsersOfWorkspace(currentUser, workspace);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, ResourceAction.MANAGE,
                "You cannot modify the users in this workspace.");
        verifierService.ensureWorkspaceManagementForUserUpdates(currentUser, workspace, updateWorkspaceUsersJsons);

        try {
            return transactionService.required(() -> {
                Set<String> userIdsToBeUpdated = updateWorkspaceUsersJsons.stream()
                        .map(request -> request.getUserId())
                        .collect(Collectors.toSet());
                Set<User> usersToBeUpdated = userService.getByUsersIds(userIdsToBeUpdated);
                verifierService.validateAllUsersAreAlreadyInTheWorkspace(currentUser, workspace, usersToBeUpdated);
                verifierService.verifyDefaultWorkspaceUserUpdates(currentUser, workspace, usersToBeUpdated);
                usersToBeUpdated.stream()
                        .filter(userToBeChange -> usersInWorkspace.contains(userToBeChange))
                        .forEach(userToBeChange -> updateUser(updateWorkspaceUsersJsons, workspace, userToBeChange));
                return usersToBeUpdated;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace getByNameForUserOrThrowNotFound(String workspaceName, User currentUser) {
        Optional<Workspace> workspace = getByNameForUser(workspaceName, currentUser);
        return workspace.orElseThrow(() -> new NotFoundException("Cannot find workspace with name: " + workspaceName));
    }

    public Set<User> changeUsers(String workspaceName, Set<ChangeWorkspaceUsersV4Request> changeUsersRequests, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        Set<User> usersInWorkspace = umsAuthorizationService.getUsersOfWorkspace(currentUser, workspace);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, ResourceAction.MANAGE,
                "You cannot modify the users in this workspace.");
        verifierService.ensureWorkspaceManagementForChangeUsers(changeUsersRequests);

        try {
            return transactionService.required(() -> {
                Set<String> newUserIds = changeUsersRequests.stream()
                        .map(newUserPermission -> newUserPermission.getUserId())
                        .collect(Collectors.toSet());

                Set<User> removableUsers = usersInWorkspace.stream()
                        .filter(workspaceUser -> !newUserIds.contains(workspaceUser.getUserId()))
                        .collect(Collectors.toSet());
                umsAuthorizationService.removeResourceRolesOfUserInWorkspace(removableUsers, workspace);

                Set<User> newUsers = userService.getByUsersIds(newUserIds);

                Set<User> usersToBeUpdated = newUsers.stream()
                        .filter(userToBeChange -> usersInWorkspace.contains(userToBeChange))
                        .collect(Collectors.toSet());
                verifierService.verifyDefaultWorkspaceUserUpdates(currentUser, workspace, usersToBeUpdated);
                usersToBeUpdated.stream().forEach(userToBeChange -> updateUser(changeUsersRequests, workspace, userToBeChange));

                newUsers.stream()
                        .filter(newUser -> !usersInWorkspace.contains(newUser))
                        .forEach(newUser -> addUserByRequest(changeUsersRequests, workspace, newUser));

                return newUsers;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace deleteByNameForUser(String workspaceName, User currentUser, Workspace defaultWorkspace) {
        Workspace workspaceForDelete = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspaceForDelete, ResourceAction.MANAGE,
                "You cannot delete this workspace.");

        try {
            return transactionService.required(() -> {
                verifierService.checkThatWorkspaceIsDeletable(currentUser, workspaceForDelete, defaultWorkspace);
                setupDeletionDateAndFlag(workspaceForDelete);
                workspaceRepository.save(workspaceForDelete);
                umsAuthorizationService.notifyAltusAboutResourceDeletion(currentUser, workspaceForDelete);
                LOGGER.debug("Deleted workspace: {}", workspaceName);
                return workspaceForDelete;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace getForCurrentUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
    }

    private void addUserByRequest(Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests, Workspace workspace, User user) {
        Optional<ChangeWorkspaceUsersV4Request> requestForUser = changeWorkspaceUsersV4Requests.stream()
                .filter(request -> StringUtils.equals(request.getUserId(), user.getUserId()))
                .findFirst();
        if (requestForUser.isPresent() && requestForUser.get().getRoles() != null) {
            requestForUser.get().getRoles().stream()
                    .forEach(role -> umsAuthorizationService.assignResourceRoleToUserInWorkspace(user, workspace, role));
        }
    }

    private void updateUser(Set<ChangeWorkspaceUsersV4Request> requestWithRoles, Workspace workspace, User userToBeChange) {
        Set<WorkspaceRole> userCurrentRoles = umsAuthorizationService.getUserRolesInWorkspace(userToBeChange, workspace);
        Optional<ChangeWorkspaceUsersV4Request> newRolesOptional = requestWithRoles.stream()
                .filter(request -> StringUtils.equals(request.getUserId(), userToBeChange.getUserId()))
                .findFirst();
        Set<WorkspaceRole> newRoles = newRolesOptional.isPresent() ? newRolesOptional.get().getRoles() : Sets.newHashSet();
        if (!userCurrentRoles.containsAll(newRoles) || !newRoles.containsAll(userCurrentRoles)) {
            userCurrentRoles.stream()
                    .forEach(currentRole -> umsAuthorizationService.unassignResourceRoleFromUserInWorkspace(userToBeChange, workspace, currentRole));
            newRoles.stream()
                    .forEach(currentRole -> umsAuthorizationService.assignResourceRoleToUserInWorkspace(userToBeChange, workspace, currentRole));
        }
    }

    private void setupDeletionDateAndFlag(Workspace workspaceForDelete) {
        workspaceForDelete.setStatus(DELETED);
        workspaceForDelete.setDeletionTimestamp(clock.getCurrentTimeMillis());
    }

    private String getAccountWorkspaceName(User user) {
        return Crn.isCrn(user.getUserCrn()) ? Crn.fromString(user.getUserCrn()).getAccountId()
                : user.getTenant().getName();
    }

}
