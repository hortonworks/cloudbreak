package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaPostInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPostInstallService.class);

    private static final String SET_PASSWORD_EXPIRATION_PERMISSION = "Set Password Expiration";

    private static final String USER_ADMIN_PRIVILEGE = "User Administrators";

    private static final int MAX_USERNAME_LENGTH = 255;

    private static final String HOST_ENROLLMENT_PRIVILEGE = "Host Enrollment";

    private static final String ADD_HOSTS_PERMISSION = "System: Add Hosts";

    private static final String REMOVE_SERVICES_PERMISSION = "System: Remove Services";

    private static final String REMOVE_HOSTS_PERMISSION = "System: Remove Hosts";

    private static final String DNS_ADMINISTRATORS_PRIVILEGE = "DNS Administrators";

    private static final String ENROLLMENT_ADMINISTRATOR_ROLE = "Enrollment Administrator";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UserSyncService userSyncService;

    @Inject
    private StackService stackService;

    @Inject
    private PasswordPolicyService passwordPolicyService;

    @Inject
    private FreeIpaPermissionService freeIpaPermissionService;

    @Inject
    private FreeIpaTopologyService freeIpaTopologyService;

    @Inject
    private FreeIpaService freeIpaService;

    public void postInstallFreeIpa(Long stackId, boolean fullPostInstall) throws Exception {
        LOGGER.debug("Performing post-install configuration for stack {}. {}.", stackId, fullPostInstall ? "Full post install" : "Partial post install");
        Stack stack = stackService.getStackById(stackId);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        freeIpaTopologyService.updateReplicationTopology(stackId, freeIpaClient);
        if (fullPostInstall) {
            setInitialFreeIpaPolicies(stack, freeIpaClient);
        }
    }

    private void setInitialFreeIpaPolicies(Stack stack, FreeIpaClient freeIpaClient) throws Exception {
        Set<Permission> permission = freeIpaClient.findPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        if (permission.isEmpty()) {
            freeIpaClient.addPasswordExpirationPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        }
        freeIpaClient.addPermissionToPrivilege(USER_ADMIN_PRIVILEGE, SET_PASSWORD_EXPIRATION_PERMISSION);
        freeIpaPermissionService.setPermissions(freeIpaClient);
        if (!Objects.equals(MAX_USERNAME_LENGTH, freeIpaClient.getUsernameLength())) {
            LOGGER.debug("Set maximum username length to {}", MAX_USERNAME_LENGTH);
            freeIpaClient.setUsernameLength(MAX_USERNAME_LENGTH);
        }
        passwordPolicyService.updatePasswordPolicy(freeIpaClient);
        modifyAdminPasswordExpirationIfNeeded(freeIpaClient);
        userSyncService.synchronizeUsers(
                ThreadBasedUserCrnProvider.getAccountId(), ThreadBasedUserCrnProvider.getUserCrn(), Set.of(stack.getEnvironmentCrn()),
                Set.of(), Set.of(), false);
    }

    private void modifyAdminPasswordExpirationIfNeeded(FreeIpaClient client) throws FreeIpaClientException {
        Optional<User> user = client.userFind(freeIpaClientFactory.getAdminUser());
        if (user.isPresent() && !FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME.equals(user.get().getKrbPasswordExpiration())) {
            User actualUser = user.get();
            LOGGER.debug(String.format("Modifying user [%s] current password expiration time [%s] to [%s]",
                    actualUser.getUid(), actualUser.getKrbPasswordExpiration(), FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME));
            client.updateUserPasswordMaxExpiration(actualUser.getUid());
        } else if (user.isEmpty()) {
            LOGGER.warn(String.format("No [%s] user found!", freeIpaClientFactory.getAdminUser()));
        } else {
            LOGGER.debug("Password expiration is already set.");
        }
    }

}
