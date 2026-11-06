package com.sequenceiq.maintenance.authorization;

import java.util.Optional;

/**
 * Resolves the parent environment CRN for a FreeIPA stack resource CRN (schedule {@code scopeId} for FREEIPA scope).
 */
public interface FreeIpaEnvironmentCrnLookup {

    Optional<String> lookupEnvironmentCrn(String freeIpaResourceCrn, String accountId);
}
