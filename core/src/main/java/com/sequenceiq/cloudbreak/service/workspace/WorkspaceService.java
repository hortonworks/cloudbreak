package com.sequenceiq.cloudbreak.service.workspace;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus.DELETED;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.WORKSPACE_MANAGE;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Request;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@Service
public class WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private WorkspaceRepository workspaceRepository;

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceModificationVerifierService verifierService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private Clock clock;

    public Workspace create(User user, Workspace workspace) {
        try {
            return transactionService.required(() -> {
                Workspace createdWorkspace = workspaceRepository.save(workspace);
                UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
                userWorkspacePermissions.setWorkspace(createdWorkspace);
                userWorkspacePermissions.setUser(user);
                userWorkspacePermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), WORKSPACE_MANAGE.value()));
                userWorkspacePermissionsService.save(userWorkspacePermissions);
                return createdWorkspace;
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Workspace with name '%s' in your tenant already exists.",
                        workspace.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Workspace> retrieveForUser(User user) {
        return userWorkspacePermissionsService.findForUser(user).stream()
                .map(UserWorkspacePermissions::getWorkspace).collect(Collectors.toSet());
    }

    public Workspace getDefaultWorkspaceForUser(User user) {
        return workspaceRepository.getByName(user.getUserName(), user.getTenant());
    }

    public Optional<Workspace> getByName(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(name, user.getTenant()));
    }

    public Optional<Workspace> getByNameForUser(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(name, user.getTenant()));
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
        UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserByWorkspaceId(user, id);
        if (userWorkspacePermissions == null) {
            throw new NotFoundException("Cannot find workspace by user.");
        }
        return userWorkspacePermissions.getWorkspace();
    }

    public Set<User> removeUsers(String workspaceName, Set<String> userIds, User user) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, user);
        verifierService.authorizeWorkspaceManipulation(user, workspace, Action.MANAGE,
                "You cannot remove users from this workspace.");
        verifierService.ensureWorkspaceManagementForUserRemoval(workspace, userIds);
        try {
            return transactionService.required(() -> {
                Set<User> users = userService.getByUsersIds(userIds);
                Set<UserWorkspacePermissions> toBeRemoved = verifierService.validateAllUsersAreAlreadyInTheWorkspace(workspace, users);
                Set<User> usersToBeRemoved = toBeRemoved.stream().map(UserWorkspacePermissions::getUser).collect(Collectors.toSet());
                verifierService.verifyDefaultWorkspaceUserRemovals(user, workspace, usersToBeRemoved);

                userWorkspacePermissionsService.deleteAll(toBeRemoved);
                return toBeRemoved.stream()
                        .map(UserWorkspacePermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> addUsers(String workspaceName, Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, Action.INVITE,
                "You cannot add users to this workspace.");
        Map<User, Set<String>> usersToAddWithPermissions = workspaceUserPermissionJsonSetToMap(changeWorkspaceUsersV4Requests);
        verifierService.validateUsersAreNotInTheWorkspaceYet(workspace, usersToAddWithPermissions.keySet());
        try {
            return transactionService.required(() -> {
                Set<UserWorkspacePermissions> userWorkspacePermsToAdd = usersToAddWithPermissions.entrySet().stream()
                        .map(userWithPermissions -> {
                            UserWorkspacePermissions newUserPermission = new UserWorkspacePermissions();
                            newUserPermission.setPermissionSet(userWithPermissions.getValue());
                            newUserPermission.setUser(userWithPermissions.getKey());
                            newUserPermission.setWorkspace(workspace);
                            return newUserPermission;
                        })
                        .collect(Collectors.toSet());

                userWorkspacePermissionsService.saveAll(userWorkspacePermsToAdd);
                return userWorkspacePermsToAdd.stream().map(UserWorkspacePermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> updateUsers(String workspaceName, Set<ChangeWorkspaceUsersV4Request> updateWorkspaceUsersJsons, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, Action.MANAGE,
                "You cannot modify the users in this workspace.");
        verifierService.ensureWorkspaceManagementForUserUpdates(workspace, updateWorkspaceUsersJsons);
        Map<User, Set<String>> usersToUpdateWithPermissions = workspaceUserPermissionJsonSetToMap(updateWorkspaceUsersJsons);
        try {
            return transactionService.required(() -> {
                Map<User, UserWorkspacePermissions> toBeUpdated = verifierService.validateAllUsersAreAlreadyInTheWorkspace(
                        workspace, usersToUpdateWithPermissions.keySet()).stream()
                        .collect(Collectors.toMap(UserWorkspacePermissions::getUser, uop -> uop));

                Set<UserWorkspacePermissions> userWorkspacePermissions = toBeUpdated.entrySet().stream()
                        .map(userPermission -> {
                            userPermission.getValue().setPermissionSet(usersToUpdateWithPermissions.get(userPermission.getKey()));
                            return userPermission.getValue();
                        })
                        .collect(Collectors.toSet());

                Set<User> usersToBeUpdated = userWorkspacePermissions.stream().map(UserWorkspacePermissions::getUser).collect(Collectors.toSet());
                verifierService.verifyDefaultWorkspaceUserUpdates(currentUser, workspace, usersToBeUpdated);

                userWorkspacePermissionsService.saveAll(userWorkspacePermissions);
                return toBeUpdated.keySet();
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace getByNameForUserOrThrowNotFound(String workspaceName, User currentUser) {
        Optional<Workspace> workspace = getByNameForUser(workspaceName, currentUser);
        return workspace.orElseThrow(() -> new NotFoundException("Cannot find workspace with name: " + workspaceName));
    }

    public Set<User> changeUsers(String workspaceName, Set<ChangeWorkspaceUsersV4Request> newUserPermissions, User currentUser) {
        Workspace workspace = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
        verifierService.authorizeWorkspaceManipulation(currentUser, workspace, Action.MANAGE,
                "You cannot modify the users in this workspace.");
        verifierService.ensureWorkspaceManagementForChangeUsers(newUserPermissions);

        try {
            return transactionService.required(() -> {
                Set<UserWorkspacePermissions> oldPermissions = userWorkspacePermissionsService.findForWorkspace(workspace);
                Map<String, User> newUsers = userService.getByUsersIds(getUserIds(newUserPermissions)).stream()
                        .collect(Collectors.toMap(User::getUserId, user -> user));
                Map<String, Set<String>> newPermissions = newUserPermissions.stream()
                        .collect(Collectors.toMap(ChangeWorkspaceUsersV4Request::getUserId, ChangeWorkspaceUsersV4Request::getPermissions));
                Map<String, User> oldUsers = oldPermissions.stream().map(UserWorkspacePermissions::getUser)
                        .collect(Collectors.toMap(User::getUserId, user -> user));

                removeUsers(currentUser, workspace, oldPermissions, newUsers, oldUsers);
                updateUsers(currentUser, workspace, oldPermissions, newUsers, newPermissions, oldUsers);
                addUsers(workspace, newUsers, newPermissions, oldUsers);
                return new HashSet<>(newUsers.values());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void removeUsers(User currentUser, Workspace workspace, Set<UserWorkspacePermissions> oldPermissions,
            Map<String, User> newUsers, Map<String, User> oldUsers) {

        Set<String> usersIdsToBeDeleted = new HashSet<>(oldUsers.keySet());
        usersIdsToBeDeleted.removeAll(newUsers.keySet());
        Set<User> usersToBeDeleted = oldUsers.values().stream()
                .filter(u -> usersIdsToBeDeleted.contains(u.getUserId()))
                .collect(Collectors.toSet());
        verifierService.verifyDefaultWorkspaceUserRemovals(currentUser, workspace, usersToBeDeleted);

        Set<UserWorkspacePermissions> permissionsToDelete = oldPermissions.stream()
                .filter(oldPermission -> usersIdsToBeDeleted.contains(oldPermission.getUser().getUserId()))
                .collect(Collectors.toSet());
        userWorkspacePermissionsService.deleteAll(permissionsToDelete);
    }

    private void updateUsers(User currentUser, Workspace workspace, Set<UserWorkspacePermissions> oldPermissions, Map<String, User> newUsers,
            Map<String, Set<String>> newPermissions, Map<String, User> oldUsers) {

        Set<String> usersIdsToBeUpdated = new HashSet<>(oldUsers.keySet());
        usersIdsToBeUpdated.retainAll(newUsers.keySet());

        Set<User> usersToBeUpdated = oldUsers.values().stream()
                .filter(u -> usersIdsToBeUpdated.contains(u.getUserId()))
                .collect(Collectors.toSet());
        verifierService.verifyDefaultWorkspaceUserUpdates(currentUser, workspace, usersToBeUpdated);

        Set<UserWorkspacePermissions> permissionsToUpdate = oldPermissions.stream()
                .filter(oldPermission -> usersIdsToBeUpdated.contains(oldPermission.getUser().getUserId()))
                .peek(oldPermission -> oldPermission.setPermissionSet(newPermissions.get(oldPermission.getUser().getUserId())))
                .collect(Collectors.toSet());
        userWorkspacePermissionsService.saveAll(permissionsToUpdate);
    }

    private void addUsers(Workspace workspace, Map<String, User> newUsers, Map<String, Set<String>> newPermissions, Map<String, User> oldUsers) {
        Set<String> usersIdsToBeAdded = new HashSet<>(newUsers.keySet());
        usersIdsToBeAdded.removeAll(oldUsers.keySet());

        Map<String, Set<String>> userAdditionsWithPermissions = usersIdsToBeAdded.stream()
                .collect(Collectors.toMap(userId -> userId, newPermissions::get));

        Set<UserWorkspacePermissions> userPermissionsToAdd = userAdditionsWithPermissions.entrySet().stream()
                .map(userPermissions -> {
                    UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
                    userWorkspacePermissions.setUser(newUsers.get(userPermissions.getKey()));
                    userWorkspacePermissions.setPermissionSet(userPermissions.getValue());
                    userWorkspacePermissions.setWorkspace(workspace);
                    return userWorkspacePermissions;
                })
                .collect(Collectors.toSet());
        userWorkspacePermissionsService.saveAll(userPermissionsToAdd);
    }

    private Set<String> getUserIds(Set<ChangeWorkspaceUsersV4Request> userPermissions) {
        return userPermissions.stream().map(ChangeWorkspaceUsersV4Request::getUserId).collect(Collectors.toSet());
    }

    public Workspace deleteByNameForUser(String workspaceName, User currentUser, Workspace defaultWorkspace) {
        try {
            return transactionService.required(() -> {
                Workspace workspaceForDelete = getByNameForUserOrThrowNotFound(workspaceName, currentUser);
                verifierService.authorizeWorkspaceManipulation(currentUser, workspaceForDelete, Action.MANAGE,
                        "You cannot delete this workspace.");

                verifierService.checkThatWorkspaceIsDeletable(currentUser, workspaceForDelete, defaultWorkspace);
                Long deleted = userWorkspacePermissionsService.deleteByWorkspace(workspaceForDelete);
                setupDeletionDateAndFlag(workspaceForDelete);
                workspaceRepository.save(workspaceForDelete);
                LOGGER.debug("Deleted workspace: {}, related permissions: {}", workspaceName, deleted);
                return workspaceForDelete;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Map<User, Set<String>> workspaceUserPermissionJsonSetToMap(Set<ChangeWorkspaceUsersV4Request> updateWorkspaceUsersJsons) {
        return updateWorkspaceUsersJsons.stream()
                .collect(Collectors.toMap(json -> userService.getByUserId(json.getUserId()), ChangeWorkspaceUsersV4Request::getPermissions));
    }

    private void setupDeletionDateAndFlag(Workspace workspaceForDelete) {
        workspaceForDelete.setStatus(DELETED);
        workspaceForDelete.setDeletionTimestamp(clock.getCurrentTimeMillis());
    }

    public Workspace getForCurrentUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
    }
}
