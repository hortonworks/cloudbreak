package com.sequenceiq.freeipa.service.freeipa.flow;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FreeIpaPostInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPostInstallService.class);

    private static final String SET_PASSWORD_EXPIRATION_PERMISSION = "Set Password Expiration";

    private static final String USER_ADMIN_PRIVILEGE = "User Administrators";

    private static final int MAX_USERNAME_LENGTH = 255;

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
        if (!Objects.equals(MAX_USERNAME_LENGTH, freeIpaClient.getUsernameLength())) {
            LOGGER.debug("Set maximum username length to {}", MAX_USERNAME_LENGTH);
            freeIpaClient.setUsernameLength(MAX_USERNAME_LENGTH);
        }
        passwordPolicyService.updatePasswordPolicy(freeIpaClient);

        Set<String> environmentCrnFilter = Set.of(stack.getEnvironmentCrn());
        environmentCrnFilter.add(stack.getEnvironmentCrn());
        userService.synchronizeUsers(
            threadBasedUserCrnProvider.getAccountId(), CrnService.checkUserCrn(threadBasedUserCrnProvider.getUserCrn()),
            environmentCrnFilter, Set.of(), Set.of());
    }
}
