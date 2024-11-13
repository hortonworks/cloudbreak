package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureRoleDefinitionProperties {

    @JsonProperty("Name")
    private String roleName;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Actions")
    private List<String> actions;

    @JsonProperty("NotActions")
    private List<String> notActions;

    @JsonProperty("DataActions")
    private List<String> dataActions;

    @JsonProperty("AssignableScopes")
    private List<String> assignableScopes;

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

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public List<String> getNotActions() {
        return notActions;
    }

    public void setNotActions(List<String> notActions) {
        this.notActions = notActions;
    }

    public List<String> getDataActions() {
        return dataActions;
    }

    public void setDataActions(List<String> dataActions) {
        this.dataActions = dataActions;
    }

    public List<String> getAssignableScopes() {
        return assignableScopes;
    }

    public void setAssignableScopes(List<String> assignableScopes) {
        this.assignableScopes = assignableScopes;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AzureRoleDefinitionProperties.class.getSimpleName() + "[", "]")
                .add("roleName='" + roleName + "'")
                .add("description='" + description + "'")
                .add("actions=" + actions)
                .add("notActions=" + notActions)
                .add("dataActions=" + dataActions)
                .add("assignableScopes=" + assignableScopes)
                .toString();
    }
}