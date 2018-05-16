package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

public class AzureRoleDefinitionListResponse {

    private List<AzureRoleDefinition> value;

    public AzureRoleDefinitionListResponse() {
    }

    public AzureRoleDefinitionListResponse(List<AzureRoleDefinition> value) {
        this.value = value;
    }

    public List<AzureRoleDefinition> getValue() {
        return value;
    }

    public void setValue(List<AzureRoleDefinition> value) {
        this.value = value;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "AzureRoleDefinitionListResponse{"
                + "value=" + value
                + '}';
    }
    //END GENERATED CODE
}
