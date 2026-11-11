package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatabaseServerModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The valid target instance types of the database server of a cluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerValidInstanceTypeOptionsV4Response {

    @Schema(description = DatabaseServerModelDescription.CURRENT_INSTANCE_TYPE)
    private String currentInstanceType;

    @Schema(description = DatabaseServerModelDescription.VALID_INSTANCE_TYPES)
    private List<String> validInstanceTypes = new ArrayList<>();

    public String getCurrentInstanceType() {
        return currentInstanceType;
    }

    public void setCurrentInstanceType(String currentInstanceType) {
        this.currentInstanceType = currentInstanceType;
    }

    public List<String> getValidInstanceTypes() {
        return validInstanceTypes;
    }

    public void setValidInstanceTypes(List<String> validInstanceTypes) {
        this.validInstanceTypes = validInstanceTypes;
    }

    @Override
    public String toString() {
        return "DatabaseServerValidInstanceTypeOptionsV4Response{" +
                "currentInstanceType='" + currentInstanceType + '\'' +
                ", validInstanceTypes=" + validInstanceTypes +
                '}';
    }
}
