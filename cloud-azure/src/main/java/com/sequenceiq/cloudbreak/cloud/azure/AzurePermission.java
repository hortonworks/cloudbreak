package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzurePermission {

    private List<String> actions;

    private List<String> notActions;

    public AzurePermission() {
    }

    public AzurePermission(List<String> actions, List<String> notActions) {
        this.actions = actions;
        this.notActions = notActions;
    }

    public Collection<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public Collection<String> getNotActions() {
        return notActions;
    }

    public void setNotActions(List<String> notActions) {
        this.notActions = notActions;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "AzurePermission{" +
                "actions=" + actions +
                ", notActions=" + notActions +
                '}';
    }
    //END GENERATED CODE
}
