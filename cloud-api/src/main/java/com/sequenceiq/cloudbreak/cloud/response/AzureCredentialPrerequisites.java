package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_APP_CREATION_COMMAND;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_ROLE_DEF_JSON;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class AzureCredentialPrerequisites extends CredentialBasePrerequisites implements Serializable {

    @Schema(description = AZURE_APP_CREATION_COMMAND, requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCreationCommand;

    @Schema(description = AZURE_ROLE_DEF_JSON, requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleDefitionJson;

    public AzureCredentialPrerequisites() {
    }

    public AzureCredentialPrerequisites(String appCreationCommand, String roleDefitionJson) {
        this.appCreationCommand = appCreationCommand;
        this.roleDefitionJson = roleDefitionJson;
    }

    public AzureCredentialPrerequisites(String appCreationCommand, String roleDefitionJson, Map<String, String> policies,
            Set<GranularPolicyResponse> granularPolicies) {
        this.appCreationCommand = appCreationCommand;
        this.roleDefitionJson = roleDefitionJson;
        setGranularPolicies(granularPolicies);
        setPolicies(policies);

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
