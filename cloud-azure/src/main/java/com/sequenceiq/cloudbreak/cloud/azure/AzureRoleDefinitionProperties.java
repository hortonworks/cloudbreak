package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureRoleDefinitionProperties {

    private String roleName;

    private String description;

    private String type;

    private List<AzurePermission> permissions;

    private List<String> assignableScopes;

    public AzureRoleDefinitionProperties() {
    }

    public AzureRoleDefinitionProperties(String roleName, String description, String type, List<AzurePermission> permissions, List<String> assignableScopes) {
        this.roleName = roleName;
        this.description = description;
        this.type = type;
        this.permissions = permissions;
        this.assignableScopes = assignableScopes;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Iterable<AzurePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<AzurePermission> permissions) {
        this.permissions = permissions;
    }

    public List<String> getAssignableScopes() {
        return assignableScopes;
    }

    public void setAssignableScopes(List<String> assignableScopes) {
        this.assignableScopes = assignableScopes;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "AzureRoleDefinitionProperties{" +
                "roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", permissions=" + permissions +
                ", assignableScopes=" + assignableScopes +
                '}';
    }
    //END GENERATED CODE
}
