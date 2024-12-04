package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.POLICIES;

import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public class CredentialBasePrerequisites {

    @Schema(description = POLICIES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> policies = new LinkedHashMap<>();

    public Map<String, String> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, String> policies) {
        this.policies = policies;
    }

}
