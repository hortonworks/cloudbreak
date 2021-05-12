package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.POLICIES;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

public class CredentialBasePrerequisites {

    @ApiModelProperty(value = POLICIES)
    private Map<String, String> policies;

    public Map<String, String> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, String> policies) {
        this.policies = policies;
    }
}
