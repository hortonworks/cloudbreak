package com.sequenceiq.cloudbreak.domain.stack;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchTypeStatus.DEPRECATED;

public enum StackPatchType {
    UNBOUND_RESTART(DEPRECATED),
    LOGGING_AGENT_AUTO_RESTART(DEPRECATED),
    LOGGING_AGENT_AUTO_RESTART_V2(DEPRECATED),
    METERING_AZURE_METADATA(DEPRECATED),
    METERING_FOLLOW_INODES(DEPRECATED),
    CLUSTER_PUBLIC_ENDPOINT(DEPRECATED),
    MOCK,
    DISABLE_REGION_FOR_FLUENTD(DEPRECATED),
    COLLECT_DB_ENGINE_VERSION(DEPRECATED),
    USER_DATA_MIGRATION(DEPRECATED),
    USER_DATA_CCMV2_SETUP,
    UPDATE_OUTDATED_VAULT_SECRETS(DEPRECATED),
    EMBEDDED_DB_CERTIFICATE_ROTATION,
    GCP_SUBNET_ID_FIX(DEPRECATED),
    ATTACHED_VOLUMES_FIX,
    UNKNOWN,
    // for UT we need sample values
    TEST_PATCH_1,
    TEST_PATCH_2,
    TEST_PATCH_3;

    private final StackPatchTypeStatus status;

    StackPatchType() {
        status = StackPatchTypeStatus.ACTIVE;
    }

    StackPatchType(StackPatchTypeStatus status) {
        this.status = status;
    }

    public StackPatchTypeStatus getStatus() {
        return status;
    }
}
