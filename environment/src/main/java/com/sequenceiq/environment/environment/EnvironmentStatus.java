package com.sequenceiq.environment.environment;

import java.util.List;
import java.util.Set;

public enum EnvironmentStatus {

    CREATION_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATION_INITIATED),
    DELETE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED),
    UPDATE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_INITIATED),

    NETWORK_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS),
    NETWORK_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS),

    FREEIPA_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS),
    FREEIPA_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS),

    RDBMS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS),

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS),
    S3GUARD_TABLE_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS),

    AVAILABLE(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE),
    ARCHIVED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED),

    CREATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED),
    DELETE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED),
    UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_FAILED);

    public static final Set<EnvironmentStatus> AVAILABLE_STATUSES = Set.of(
            CREATION_INITIATED,
            UPDATE_INITIATED,
            NETWORK_CREATION_IN_PROGRESS,
            FREEIPA_CREATION_IN_PROGRESS,
            AVAILABLE);

    private final com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus;

    EnvironmentStatus(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus getResponseStatus() {
        return responseStatus;
    }

    public boolean isDeleteInProgress() {
        return List.of(NETWORK_DELETE_IN_PROGRESS, FREEIPA_DELETE_IN_PROGRESS, RDBMS_DELETE_IN_PROGRESS, IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
                S3GUARD_TABLE_DELETE_IN_PROGRESS, DELETE_INITIATED)
                .contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return ARCHIVED.equals(this);
    }
}
