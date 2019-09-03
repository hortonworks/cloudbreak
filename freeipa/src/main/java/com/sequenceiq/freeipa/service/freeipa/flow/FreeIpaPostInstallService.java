package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;

@Service
public class FreeIpaPostInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPostInstallService.class);

    private static final String SET_PASSWORD_EXPIRATION_PERMISSION = "Set Password Expiration";

    private static final String USER_ADMIN_PRIVILEGE = "User Administrators";

    private static final int MAX_USERNAME_LENGTH = 255;

    private static final String HOST_ENROLLMENT_PRIVILEGE = "Host Enrollment";

    private static final String ADD_HOSTS_PERMISSION = "System: Add Hosts";

    private static final String DNS_ADMINISTRATORS_PRIVILEGE = "DNS Administrators";

    private static final String ENROLLMENT_ADMINISTRATOR_ROLE = "Enrollment Administrator";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UserService userService;

    @Inject
    private StackService stackService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private PasswordPolicyService passwordPolicyService;

    public void postInstallFreeIpa(Long stackId) throws Exception {
        LOGGER.debug("Performing post-install configuration for stack {}", stackId);
        Stack stack = stackService.getStackById(stackId);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        Set<Permission> permission = freeIpaClient.findPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        if (permission.isEmpty()) {
            freeIpaClient.addPasswordExpirationPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        }
        freeIpaClient.addPermissionToPrivilege(USER_ADMIN_PRIVILEGE, SET_PASSWORD_EXPIRATION_PERMISSION);
        freeIpaClient.addPermissionToPrivilege(HOST_ENROLLMENT_PRIVILEGE, ADD_HOSTS_PERMISSION);
        freeIpaClient.addRolePriviliges(ENROLLMENT_ADMINISTRATOR_ROLE, Set.of(DNS_ADMINISTRATORS_PRIVILEGE));
        if (!Objects.equals(MAX_USERNAME_LENGTH, freeIpaClient.getUsernameLength())) {
            LOGGER.debug("Set maximum username length to {}", MAX_USERNAME_LENGTH);
            freeIpaClient.setUsernameLength(MAX_USERNAME_LENGTH);
        }
        passwordPolicyService.updatePasswordPolicy(freeIpaClient);
        userService.synchronizeUsers(
            threadBasedUserCrnProvider.getAccountId(), threadBasedUserCrnProvider.getUserCrn(), Set.of(stack.getEnvironmentCrn()), Set.of(), Set.of());

    }
}
