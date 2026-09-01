package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseInstanceTypesV4Response {

    @Schema(description = "Available database instance types for the given environment and region")
    private List<DatabaseInstanceTypeV4> instanceTypes;

    @Schema(description = "The default database instance type for the region")
    private String defaultInstanceType;

    public DatabaseInstanceTypesV4Response() {
    }

    public DatabaseInstanceTypesV4Response(List<DatabaseInstanceTypeV4> instanceTypes, String defaultInstanceType) {
        this.instanceTypes = instanceTypes;
        this.defaultInstanceType = defaultInstanceType;
    }

    public List<DatabaseInstanceTypeV4> getInstanceTypes() {
        return instanceTypes;
    }

    public void setInstanceTypes(List<DatabaseInstanceTypeV4> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }

    public String getDefaultInstanceType() {
        return defaultInstanceType;
    }

    public void setDefaultInstanceType(String defaultInstanceType) {
        this.defaultInstanceType = defaultInstanceType;
    }

    @Override
    public String toString() {
        return "DatabaseInstanceTypesV4Response{" +
                "instanceTypes=" + instanceTypes +
                ", defaultInstanceType='" + defaultInstanceType + '\'' +
                '}';
    }
}
