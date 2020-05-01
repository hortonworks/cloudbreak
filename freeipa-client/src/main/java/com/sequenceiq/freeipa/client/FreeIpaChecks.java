package com.sequenceiq.freeipa.client;

import java.util.List;
import java.util.function.Supplier;

public class FreeIpaChecks {
    /* Users in this list should not be added/deleted. */
    public static final List<String> IPA_PROTECTED_USERS = List.of("admin");

    /* Groups in this list should not be added/deleted. */
    public static final List<String> IPA_PROTECTED_GROUPS = List.of("admins", "editors", "ipausers", "trust admins");

    /* Group membership in these groups should not be changed. */
    public static final List<String> IPA_UNMANAGED_GROUPS = List.of("editors", "ipausers", "trust admins");

    private FreeIpaChecks() {
    }

    public static void checkUserNotProtected(String user, Supplier<String> errorMessage) throws FreeIpaClientException {
        if (IPA_PROTECTED_USERS.contains(user)) {
            throw new FreeIpaClientException(errorMessage.get());
        }
    }

    public static void checkGroupNotProtected(String group, Supplier<String> errorMessage) throws FreeIpaClientException {
        if (IPA_PROTECTED_GROUPS.contains(group)) {
            throw new FreeIpaClientException(errorMessage.get());
        }
    }

    public static void checkGroupNotUnmanaged(String group, Supplier<String> errorMessage) throws FreeIpaClientException {
        if (IPA_UNMANAGED_GROUPS.contains(group)) {
            throw new FreeIpaClientException(errorMessage.get());
        }
    }
}
