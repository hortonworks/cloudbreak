package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_APP_CREATION_COMMAND;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_ROLE_DEF_JSON;

import java.io.Serializable;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AzureCredentialPrerequisites extends CredentialBasePrerequisites implements Serializable {

    @ApiModelProperty(value = AZURE_APP_CREATION_COMMAND, required = true)
    private String appCreationCommand;

    @ApiModelProperty(value = AZURE_ROLE_DEF_JSON, required = true)
    private String roleDefitionJson;

    public AzureCredentialPrerequisites() {
    }

    public AzureCredentialPrerequisites(String appCreationCommand, String roleDefitionJson) {
        this.appCreationCommand = appCreationCommand;
        this.roleDefitionJson = roleDefitionJson;
    }

    public AzureCredentialPrerequisites(String appCreationCommand, String roleDefitionJson, Map<String, String> policies) {
        this.appCreationCommand = appCreationCommand;
        this.roleDefitionJson = roleDefitionJson;
        this.setPolicies(policies);

    }

    public void setAppCreationCommand(String appCreationCommand) {
        this.appCreationCommand = appCreationCommand;
    }

    public void setRoleDefitionJson(String roleDefitionJson) {
        this.roleDefitionJson = roleDefitionJson;
    }

    public String getAppCreationCommand() {
        return appCreationCommand;
    }

    public String getRoleDefitionJson() {
        return roleDefitionJson;
    }
}
