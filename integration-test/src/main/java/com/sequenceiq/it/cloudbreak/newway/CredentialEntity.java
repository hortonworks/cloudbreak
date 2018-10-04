package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;

public class CredentialEntity extends AbstractCloudbreakEntity<CredentialRequest, CredentialResponse> {
    public static final String CREDENTIAL = "CREDENTIAL";

    CredentialEntity(String newId) {
        super(newId);
        setRequest(new CredentialRequest());
    }

    CredentialEntity() {
        this(CREDENTIAL);
    }

    public CredentialEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public CredentialEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public CredentialEntity withCloudPlatform(String cloudPlatform) {
        getRequest().setCloudPlatform(cloudPlatform);
        return this;
    }

    public CredentialEntity withParameters(Map<String, Object> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }
}
