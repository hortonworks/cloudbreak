package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential;

public enum CredentialType {
    UNKNOWN,
    AWS_KEY_BASED,
    AWS_ROLE_BASED,
    GCP_JSON,
    GCP_P12,
    AZURE_CODEGRANTFLOW,
    AZURE_APPBASED_CERTIFICATE,
    AZURE_APPBASED_SECRET,
    YARN,
    MOCK
}
