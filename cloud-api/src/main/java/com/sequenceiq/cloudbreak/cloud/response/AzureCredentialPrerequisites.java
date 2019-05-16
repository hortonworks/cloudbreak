package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_APP_CREATION_COMMAND;

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
