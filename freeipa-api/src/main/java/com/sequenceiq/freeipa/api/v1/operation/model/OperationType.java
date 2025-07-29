package com.sequenceiq.freeipa.api.v1.operation.model;

import java.util.Locale;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;

public enum OperationType {
    USER_SYNC,
    SET_PASSWORD,
    CLEANUP,
    REBOOT,
    REPAIR,
    DOWNSCALE,
    UPSCALE,
    BIND_USER_CREATE,
    UPGRADE,
    UPGRADE_CCM,
    MODIFY_PROXY_CONFIG,
    CHANGE_DYNAMIC_ENTITLEMENTS,
    REBUILD,
    MODIFY_ROOT_VOLUME,
    MODIFY_SELINUX_MODE,
    TRUST_SETUP,
    TRUST_SETUP_FINISH,
    CANCEL_TRUST_SETUP,
    UPGRADE_DEFAULT_OUTBOUND;

    private final String lowerCaseName;

    OperationType() {
        lowerCaseName = name().toLowerCase(Locale.ROOT);
    }

    public static OperationType fromSyncOperationType(SyncOperationType syncOperationType) {
        return switch (syncOperationType) {
            case USER_SYNC -> USER_SYNC;
            case SET_PASSWORD -> SET_PASSWORD;
            default -> throw new UnsupportedOperationException("SyncOperationType not mapped: " + syncOperationType);
        };
    }

    public String getLowerCaseName() {
        return lowerCaseName;
    }
}
