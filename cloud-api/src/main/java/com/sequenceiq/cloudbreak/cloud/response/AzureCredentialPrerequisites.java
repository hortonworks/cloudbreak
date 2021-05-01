package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_APP_CREATION_COMMAND;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_ROLE_DEF_JSON;

import java.io.Serializable;
import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AzureCredentialPrerequisites implements Serializable {

    @ApiModelProperty(value = AZURE_APP_CREATION_COMMAND, required = true)
    private String appCreationCommand;

    @ApiModelProperty(value = AZURE_ROLE_DEF_JSON, required = true)
    private String roleDefitionJson;

    public AzureCredentialPrerequisites(String appCreationCommand, String roleDefitionJson) {
        this.appCreationCommand = appCreationCommand;
        this.roleDefitionJson = roleDefitionJson;
    }

    public String getAppCreationCommand() {
        return appCreationCommand;
    }

    public String getRoleDefitionJson() {
        return roleDefitionJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AzureCredentialPrerequisites that = (AzureCredentialPrerequisites) o;
        return Objects.equals(appCreationCommand, that.appCreationCommand)
                && Objects.equals(roleDefitionJson, that.roleDefitionJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appCreationCommand, roleDefitionJson);
    }

    @Override
    public String toString() {
        return "AzureCredentialPrerequisites{" +
                "appCreationCommand='" + appCreationCommand + '\'' +
                ", roleDefitionJson='" + roleDefitionJson + '\'' +
                '}';
    }
}
