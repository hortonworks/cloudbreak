package com.sequenceiq.cloudbreak.startup;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ORG_MANAGE;

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
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.Tenant;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.organization.TenantRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class UserAndOrganizationMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAndOrganizationMigrator.class);

    private static final String ORPHANED_RESOURCES = "OrphanedResources";

    private static final long MILLIS_IN_A_SECOND = 1000L;

    private static final long SLEEP_TIME = 5000L;

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private UserOrgPermissionsService userOrgPermissionsService;

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

    public UserMigrationResults migrateUsersAndOrgs() throws TransactionExecutionException {
        long uaaStartupTimeoutMillis = uaaStartupTimeoutSec * MILLIS_IN_A_SECOND;
        long start = System.currentTimeMillis();
        while (start + uaaStartupTimeoutMillis > System.currentTimeMillis()) {
            try {
                List<IdentityUser> identityUsers = tryFetchingUsers();
                Map<String, User> ownerIdToUser = new HashMap<>();
                createUsersAndFillUserDataStructures(identityUsers, ownerIdToUser);
                Organization orphanedResources = getOrCreateOrphanedResourcesOrg();
                addUsersToOrphanedResourcesOrg(ownerIdToUser, orphanedResources);
                return new UserMigrationResults(ownerIdToUser, orphanedResources);
            } catch (TokenUnavailableException e) {
                LOGGER.error("Failed to fetch identity users. Waiting for 5 seconds for UAA.", e);
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException interrupted) {
                    throw new IllegalStateException(interrupted);
                }
            }
        }
        String errorMessage = "Failed to fetch identity users. Org migration is no possible.";
        LOGGER.error(errorMessage);
        throw new IllegalStateException(errorMessage);
    }

    private List<IdentityUser> tryFetchingUsers() {
        return userDetailsService.getAllUsers(clientSecret);
    }

    private void createUsersAndFillUserDataStructures(List<IdentityUser> identityUsers, Map<String, User> ownerIdToUser) throws TransactionExecutionException {
        transactionService.required(() -> {
            identityUsers.forEach(identityUser -> {
                User user = userService.getOrCreate(identityUser);
                ownerIdToUser.put(identityUser.getUserId(), user);
                userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user);
            });
            return null;
        });
    }

    private Organization getOrCreateOrphanedResourcesOrg() throws TransactionExecutionException {
        return transactionService.required(() -> {
            Tenant tenant = tenantRepository.findByName("DEFAULT");
            Organization orphanedResources = organizationRepository.getByName(ORPHANED_RESOURCES, tenant);
            if (orphanedResources == null) {
                orphanedResources = new Organization();
                orphanedResources.setName(ORPHANED_RESOURCES);
                orphanedResources.setDescription("Organization for storing resources that were created by users "
                        + "who were not available during organization database migration.");
                orphanedResources.setTenant(tenant);
                orphanedResources = organizationRepository.save(orphanedResources);
            }
            return orphanedResources;
        });
    }

    private void addUsersToOrphanedResourcesOrg(Map<String, User> ownerIdToUser, Organization orphanedResources) throws TransactionExecutionException {
        transactionService.required(() -> {
            Set<UserOrgPermissions> orphanedUserPermissions = ownerIdToUser.values().stream()
                    .filter(u -> userOrgPermissionsService.findForUserAndOrganization(u, orphanedResources) == null)
                    .map(u -> {
                        UserOrgPermissions userOrgPermissions = new UserOrgPermissions();
                        userOrgPermissions.setUser(u);
                        userOrgPermissions.setOrganization(orphanedResources);
                        userOrgPermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), ORG_MANAGE.value()));
                        return userOrgPermissions;
                    }).collect(Collectors.toSet());
            userOrgPermissionsService.saveAll(orphanedUserPermissions);
            return null;
        });
    }

    public void setUaaStartupTimeoutSec(long uaaStartupTimeoutSec) {
        this.uaaStartupTimeoutSec = uaaStartupTimeoutSec;
    }
}
