package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND;

import java.io.Serializable;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class GcpCredentialPrerequisites extends CredentialBasePrerequisites implements Serializable {

    @ApiModelProperty(value = GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND, required = true)
    private String creationCommand;

    public GcpCredentialPrerequisites(String creationCommand) {
        this.creationCommand = creationCommand;
    }

    public GcpCredentialPrerequisites(String creationCommand, Map<String, String> policies) {
        this.creationCommand = creationCommand;
        setPolicies(policies);
    }

    public String getCreationCommand() {
        return creationCommand;
    }
}
