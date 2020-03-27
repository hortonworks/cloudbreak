package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_DNS_ENTRIES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_HOSTS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_ROLES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_USERS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_VAULT_ENTRIES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REVOKE_CERTS_STATE;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep;

@Component
public class CleanupStepToStateNameConverter {

    public String convert(CleanupStep cleanupStep) {
        switch (cleanupStep) {
            case REMOVE_HOSTS:
                return REMOVE_HOSTS_STATE.name();
            case REMOVE_ROLES:
                return REMOVE_ROLES_STATE.name();
            case REMOVE_USERS:
                return REMOVE_USERS_STATE.name();
            case REMOVE_DNS_ENTRIES:
                return REMOVE_DNS_ENTRIES_STATE.name();
            case REVOKE_CERTS:
                return REVOKE_CERTS_STATE.name();
            case REMOVE_VAULT_ENTRIES:
                return REMOVE_VAULT_ENTRIES_STATE.name();
            default:
                return "";
        }
    }

    public Set<String> convert(Collection<CleanupStep> cleanupSteps) {
        return cleanupSteps == null ? Set.of() : cleanupSteps.stream().map(this::convert).collect(Collectors.toSet());
    }
}
