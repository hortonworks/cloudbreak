package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;

@Service
public class FreeIpaPostInstallService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPostInstallService.class);

    private static final String SET_PASSWORD_EXPIRATION_PERMISSION = "Set Password Expiration";

    private static final String USER_ADMIN_PRIVILEGE = "User Administrators";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public void postInstallFreeIpa(Long stackId) throws Exception {
        LOGGER.debug("Performing post-install configuration for stack {}", stackId);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
        freeIpaClient.addPasswordExpirationPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        freeIpaClient.addPermissionToPrivilege(USER_ADMIN_PRIVILEGE, SET_PASSWORD_EXPIRATION_PERMISSION);
    }
}
