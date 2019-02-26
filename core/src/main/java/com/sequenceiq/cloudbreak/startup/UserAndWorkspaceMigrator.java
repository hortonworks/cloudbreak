package com.sequenceiq.cloudbreak.startup;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.WORKSPACE_MANAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.TokenUnavailableException;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@Component
public class UserAndWorkspaceMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAndWorkspaceMigrator.class);

    private static final String ORPHANED_RESOURCES = "OrphanedResources";

    private static final long MILLIS_IN_A_SECOND = 1000L;

    private static final long SLEEP_TIME = 5000L;

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private WorkspaceRepository workspaceRepository;

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private UserService userService;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private TransactionService transactionService;

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Value("${cb.uaa.startup.timeout.sec}")
    private long uaaStartupTimeoutSec;

    public UserMigrationResults migrateUsersAndWorkspaces() throws TransactionExecutionException {
        long uaaStartupTimeoutMillis = uaaStartupTimeoutSec * MILLIS_IN_A_SECOND;
        long start = System.currentTimeMillis();
        while (start + uaaStartupTimeoutMillis > System.currentTimeMillis()) {
            try {
                List<CloudbreakUser> cloudbreakUsers = tryFetchingUsers();
                Map<String, User> ownerIdToUser = new HashMap<>();
                createUsersAndFillUserDataStructures(cloudbreakUsers, ownerIdToUser);
                Workspace orphanedResources = getOrCreateOrphanedResourcesWorkspace();
                addUsersToOrphanedResourcesWorkspace(ownerIdToUser, orphanedResources);
                return new UserMigrationResults(ownerIdToUser, orphanedResources);
            } catch (TokenUnavailableException e) {
                LOGGER.error("Failed to fetch identity users. Waiting for 5 seconds for UAA.", e);
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException interrupted) {
                throw new IllegalStateException(interrupted);
            }
        }
        String errorMessage = "Failed to fetch identity users. Workspace migration is no possible.";
        LOGGER.error(errorMessage);
        throw new IllegalStateException(errorMessage);
    }

    private List<CloudbreakUser> tryFetchingUsers() {
        return userDetailsService.getAllUsers(clientSecret);
    }

    private void createUsersAndFillUserDataStructures(List<CloudbreakUser> cloudbreakUsers, Map<String, User> ownerIdToUser)
            throws TransactionExecutionException {
        transactionService.required(() -> {
            cloudbreakUsers.forEach(identityUser -> {
                User user = userService.getOrCreate(identityUser);
                ownerIdToUser.put(identityUser.getUserId(), user);
                userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user);
            });
            return null;
        });
    }

    private Workspace getOrCreateOrphanedResourcesWorkspace() throws TransactionExecutionException {
        return transactionService.required(() -> {
            Tenant tenant = tenantRepository.findByName("DEFAULT");
            Workspace orphanedResources = workspaceRepository.getByName(ORPHANED_RESOURCES, tenant);
            if (orphanedResources == null) {
                orphanedResources = new Workspace();
                orphanedResources.setName(ORPHANED_RESOURCES);
                orphanedResources.setDescription("Workspace for storing resources that were created by users "
                        + "who were not available during workspace database migration.");
                orphanedResources.setTenant(tenant);
                orphanedResources = workspaceRepository.save(orphanedResources);
            }
            return orphanedResources;
        });
    }

    private void addUsersToOrphanedResourcesWorkspace(Map<String, User> ownerIdToUser, Workspace orphanedResources) throws TransactionExecutionException {
        transactionService.required(() -> {
            Set<UserWorkspacePermissions> orphanedUserPermissions = ownerIdToUser.values().stream()
                    .filter(u -> userWorkspacePermissionsService.findForUserAndWorkspace(u, orphanedResources) == null)
                    .map(u -> {
                        UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
                        userWorkspacePermissions.setUser(u);
                        userWorkspacePermissions.setWorkspace(orphanedResources);
                        userWorkspacePermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), WORKSPACE_MANAGE.value()));
                        return userWorkspacePermissions;
                    }).collect(Collectors.toSet());
            userWorkspacePermissionsService.saveAll(orphanedUserPermissions);
            return null;
        });
    }

    public void setUaaStartupTimeoutSec(long uaaStartupTimeoutSec) {
        this.uaaStartupTimeoutSec = uaaStartupTimeoutSec;
    }
}
