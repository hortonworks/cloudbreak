package com.sequenceiq.environment.environment;

public enum EnvironmentStatus {

    CREATION_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATION_INITIATED),
    DELETE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED),
    NETWORK_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS),
    NETWORK_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS),
    RDBMS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS),
    FREEIPA_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS),
    FREEIPA_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS),
    AVAILABLE(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE),
    ARCHIVED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED),
    CORRUPTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CORRUPTED);

    private final com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus;

    EnvironmentStatus(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus getResponseStatus() {
        return responseStatus;
    }

}
