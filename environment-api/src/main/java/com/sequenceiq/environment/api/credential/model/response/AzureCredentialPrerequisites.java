package com.sequenceiq.environment.api.credential.model.response;

import static com.sequenceiq.environment.api.credential.doc.CredentialModelDescription.AZURE_APP_CREATION_COMMAND;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AzureCredentialPrerequisites implements Serializable {

    @ApiModelProperty(value = AZURE_APP_CREATION_COMMAND, required = true)
    private String appCreationCommand;

    public AzureCredentialPrerequisites(String appCreationCommand) {
        this.appCreationCommand = appCreationCommand;
    }

    public String getAppCreationCommand() {
        return appCreationCommand;
    }
}
