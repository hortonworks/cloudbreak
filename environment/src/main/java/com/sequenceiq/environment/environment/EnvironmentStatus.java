package com.sequenceiq.environment.environment;

import java.util.List;

public enum EnvironmentStatus {

    CREATION_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATION_INITIATED),
    DELETE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED),
    UPDATE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_INITIATED),

    NETWORK_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS),
    NETWORK_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS),

    FREEIPA_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS),
    FREEIPA_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS),

    RDBMS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS),

    CLUSTER_DEFINITION_DELETE_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS),

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS),

    AVAILABLE(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE),
    ARCHIVED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED),

    CREATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED),
    DELETE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED),
    UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_FAILED);

    private final com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus;

    EnvironmentStatus(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus getResponseStatus() {
        return responseStatus;
    }

    public boolean isDeleteInProgress() {
        return List.of(NETWORK_DELETE_IN_PROGRESS, FREEIPA_DELETE_IN_PROGRESS, RDBMS_DELETE_IN_PROGRESS, IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
                CLUSTER_DEFINITION_DELETE_PROGRESS, DELETE_INITIATED)
                .contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return ARCHIVED.equals(this);
    }
}
