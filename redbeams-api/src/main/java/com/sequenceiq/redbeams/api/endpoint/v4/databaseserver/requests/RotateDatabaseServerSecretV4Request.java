package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ModelDescriptions.ROTATE_DATABASE_SERVER_SECRETS_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RotateDatabaseServerSecretV4Request {

    @ValidCrn(resource = { CrnResourceDescriptor.DATABASE_SERVER })
    private String crn;

    @NotNull
    private String secret;

    @NotNull
    private RotationFlowExecutionType executionType;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(RotationFlowExecutionType executionType) {
        this.executionType = executionType;
    }
}
