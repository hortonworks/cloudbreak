package com.sequenceiq.cloudbreak.service.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.repository.workspace.UserWorkspacePermissionsRepository;

@Service
public class UserWorkspacePermissionsService {

    private static final String VALID_PERMISSIONS_MESSAGE = "Valid permissions are: " + Arrays.stream(WorkspacePermissions.values())
            .map(WorkspacePermissions::value)
            .collect(Collectors.joining(", "));

    @Inject
    private UserWorkspacePermissionsRepository userWorkspacePermissionsRepository;

    @Inject
    private UserWorkspacePermissionsValidator userWorkspacePermissionsValidator;

    @Inject
    private CachedUserService cachedUserService;

    public Set<UserWorkspacePermissions> findForCurrentUser(User currentUser) {
        return userWorkspacePermissionsRepository.findForUser(currentUser);
    }

    public UserWorkspacePermissions findForUserByWorkspaceId(Long workspaceId, User currentUser) {
        return userWorkspacePermissionsRepository.findForUserByWorkspaceId(currentUser, workspaceId);
    }

    public Set<UserWorkspacePermissions> findForUser(User user) {
        return userWorkspacePermissionsRepository.findForUser(user);
    }

    public Set<UserWorkspacePermissions> findForWorkspace(Workspace workspace) {
        return userWorkspacePermissionsRepository.findForWorkspace(workspace);
    }

    public UserWorkspacePermissions findForUserAndWorkspace(User user, Workspace workspace) {
        return userWorkspacePermissionsRepository.findForUserAndWorkspace(user, workspace);
    }

    public UserWorkspacePermissions findForUserByWorkspaceId(User user, Long workspaceId) {
        return userWorkspacePermissionsRepository.findForUserByWorkspaceId(user, workspaceId);
    }

    public Set<UserWorkspacePermissions> deleteAll(Set<UserWorkspacePermissions> toBeDeleted) {
        toBeDeleted.forEach(p -> cachedUserService.evictUser(p.getUser()));
        userWorkspacePermissionsRepository.deleteAll(toBeDeleted);
        return toBeDeleted;
    }

    public Set<UserWorkspacePermissions> findForUserByWorkspaceIds(User user, Set<Long> workspaceIds) {
        return userWorkspacePermissionsRepository.findForUserByWorkspaceIds(user, workspaceIds);
    }

    public Long deleteByWorkspace(Workspace workspace) {
        return userWorkspacePermissionsRepository.deleteByWorkspace(workspace);
    }

    public UserWorkspacePermissions save(UserWorkspacePermissions userWorkspacePermissions) {
        validateSaveAll(Collections.singleton(userWorkspacePermissions));
        return userWorkspacePermissionsRepository.save(userWorkspacePermissions);
    }

    public Iterable<UserWorkspacePermissions> saveAll(Set<UserWorkspacePermissions> userWorkspacePermsToAdd) {
        userWorkspacePermsToAdd.forEach(p -> cachedUserService.evictUser(p.getUser()));
        validateSaveAll(userWorkspacePermsToAdd);
        return userWorkspacePermissionsRepository.saveAll(userWorkspacePermsToAdd);
    }

    private void validateSaveAll(Set<UserWorkspacePermissions> userWorkspacePermsToAdd) {
        Set<ValidationResult> validationResults = userWorkspacePermsToAdd.stream()
                .map(userWorkspacePermissionsValidator::validate)
                .collect(Collectors.toSet());
        Optional<ValidationResult> mergedResults = validationResults.stream()
                .reduce(ValidationResult::merge);
        if (mergedResults.isPresent() && mergedResults.get().hasError()) {
            throw new IllegalArgumentException(buildErrorMessage(mergedResults.get()));
        }
    }

    private String buildErrorMessage(ValidationResult validationResult) {
        return new StringBuilder("Invalid permissions: ")
                .append(validationResult.getFormattedErrors()).append('\n')
                .append(VALID_PERMISSIONS_MESSAGE)
                .toString();
    }
}
