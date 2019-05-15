package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription.AZURE_APP_CREATION_COMMAND;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@ApiModel
public class AzureCredentialPrerequisites implements JsonEntity {

    @ApiModelProperty(value = AZURE_APP_CREATION_COMMAND, required = true)
    private String appCreationCommand;

    public AzureCredentialPrerequisites(String appCreationCommand) {
        this.appCreationCommand = appCreationCommand;
    }

    public String getAppCreationCommand() {
        return appCreationCommand;
    }
}
