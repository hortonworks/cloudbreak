package com.sequenceiq.freeipa.api.v1.freeipa.cleanup;

public enum CleanupStep {
    REVOKE_CERTS,
    REMOVE_HOSTS,
    REMOVE_DNS_ENTRIES,
    REMOVE_VAULT_ENTRIES,
    REMOVE_USERS,
    REMOVE_ROLES;
}
