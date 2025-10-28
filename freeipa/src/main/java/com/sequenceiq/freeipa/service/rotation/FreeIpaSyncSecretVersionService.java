package com.sequenceiq.freeipa.service.rotation;

import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.CCMV2_JUMPGATE_AGENT_ACCESS_KEY;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_BOOT_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_MASTER_PRIVATE_KEY;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_SIGN_PRIVATE_KEY;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.secret.vault.SyncSecretVersionService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaSyncSecretVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSyncSecretVersionService.class);

    @Inject
    private SyncSecretVersionService syncSecretVersionService;

    @Inject
    private StackService stackService;

    public void syncOutdatedSecrets(String environmentCrn) {
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();
        try {
            Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);

            syncSecretVersionService.updateEntityIfNeeded(environmentCrn, stack.getSecurityConfig().getSaltSecurityConfig(),
                    Set.of(SALT_PASSWORD, SALT_MASTER_PRIVATE_KEY, SALT_SIGN_PRIVATE_KEY, SALT_BOOT_PASSWORD));
            syncSecretVersionService.updateEntityIfNeeded(environmentCrn, stack.getImage(),
                    Set.of(CCMV2_JUMPGATE_AGENT_ACCESS_KEY));
        } catch (NotFoundException nfe) {
            LOGGER.debug("There is no FreeIPA cluster for environment CRN {}, there is nothing to sync!", environmentCrn);
        }
    }
}
