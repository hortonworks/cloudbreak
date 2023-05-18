package com.sequenceiq.cloudbreak.vault;

import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadBasedVaultReadFieldProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadBasedVaultReadFieldProvider.class);

    private static final ThreadLocal<String> READ_FIELD_NAME = ThreadLocal.withInitial(() -> VaultConstants.FIELD_SECRET);

    private static final ThreadLocal<Set<String>> AFFECTED_SECRETS = ThreadLocal.withInitial(Set::of);

    private ThreadBasedVaultReadFieldProvider() {

    }

    public static <T> T doRollback(Set<String> affectedSecrets, Supplier<T> callable) {
        READ_FIELD_NAME.set(VaultConstants.FIELD_BACKUP);
        AFFECTED_SECRETS.set(affectedSecrets);
        try {
            return callable.get();
        } finally {
            READ_FIELD_NAME.set(VaultConstants.FIELD_SECRET);
            AFFECTED_SECRETS.set(Set.of());
        }
    }

    public static void doRollback(Set<String> affectedVaultPaths, Runnable runnable) {
        READ_FIELD_NAME.set(VaultConstants.FIELD_BACKUP);
        AFFECTED_SECRETS.set(affectedVaultPaths);
        try {
            runnable.run();
        } finally {
            READ_FIELD_NAME.set(VaultConstants.FIELD_SECRET);
            AFFECTED_SECRETS.set(Set.of());
        }
    }

    public static String getFieldName(String secret) {
        String vaultReadFieldName = READ_FIELD_NAME.get();
        Set<String> affectedSecrets = AFFECTED_SECRETS.get();
        if (vaultReadFieldName != null && !affectedSecrets.isEmpty() && affectedSecrets.stream().anyMatch(affectedSecret -> affectedSecret.contains(secret))) {
            return vaultReadFieldName;
        } else {
            return VaultConstants.FIELD_SECRET;
        }
    }
}
