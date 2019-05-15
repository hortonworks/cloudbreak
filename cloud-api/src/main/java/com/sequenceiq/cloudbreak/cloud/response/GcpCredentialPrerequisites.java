package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription.GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@ApiModel
public class GcpCredentialPrerequisites implements JsonEntity {

    @ApiModelProperty(value = GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND, required = true)
    private String creationCommand;

    public GcpCredentialPrerequisites(String creationCommand) {
        this.creationCommand = creationCommand;
    }

    public String getCreationCommand() {
        return creationCommand;
    }
}
